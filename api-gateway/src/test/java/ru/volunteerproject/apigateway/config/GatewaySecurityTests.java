package ru.volunteerproject.apigateway.config;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.volunteerproject.apigateway.error.GatewayAuthenticationEntryPoint;
import ru.volunteerproject.apigateway.error.GatewayErrorWriter;
import ru.volunteerproject.apigateway.filter.TrustedIdentityGlobalFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class GatewaySecurityTests {
    private ReactiveJwtDecoder decoder;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        decoder = mock(ReactiveJwtDecoder.class);
        when(decoder.decode("expired")).thenReturn(Mono.error(new BadJwtException("JWT expired")));
        var config = new SecurityConfiguration();
        var catalog = new GatewayRouteCatalog();
        var entryPoint = new GatewayAuthenticationEntryPoint(new GatewayErrorWriter(new ObjectMapper()));
        var proxy = new WebFilterChainProxy(List.of(
                config.authApiSecurityWebFilterChain(ServerHttpSecurity.http(), catalog),
                config.protectedApiSecurityWebFilterChain(http(), catalog, entryPoint),
                config.publicApiSecurityWebFilterChain(http(), catalog, entryPoint)));
        client = WebTestClient.bindToWebHandler(exchange -> exchange.getResponse().setComplete())
                .webFilter(proxy).build();
    }

    private ServerHttpSecurity http() {
        return ServerHttpSecurity.http().oauth2ResourceServer(resource ->
                resource.jwt(jwt -> jwt.jwtDecoder(decoder)));
    }

    @Test
    void authEndpointsIgnoreExpiredAccessToken() {
        for (String operation : List.of("register", "login", "refresh", "logout")) {
            client.post().uri("/api/v1/auth/" + operation)
                    .header("Authorization", "Bearer expired")
                    .cookie("refreshToken", "refresh-cookie")
                    .exchange().expectStatus().isOk();
        }
        verifyNoInteractions(decoder);
    }

    @Test
    void publicHelpAllowsAnonymousButRejectsExpiredToken() {
        client.get().uri("/api/v1/help/tasks").exchange().expectStatus().isOk();
        client.get().uri("/api/v1/help/tasks").header("Authorization", "Bearer expired")
                .exchange().expectStatus().isUnauthorized();
        verify(decoder).decode("expired");
    }

    @Test
    void protectedHelpRequiresValidAuthentication() {
        client.post().uri("/api/v1/help/tasks").exchange().expectStatus().isUnauthorized();
        client.post().uri("/api/v1/help/tasks").header("Authorization", "Bearer expired")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void protectedHelpAllowsUserWithJwt(){
        Jwt jwt = Jwt.withTokenValue("valid")
                .header("alg", "HS256")
                .subject(UUID.randomUUID().toString())
                .build();
        when(decoder.decode("valid")).thenReturn(Mono.just(jwt));

        client.post().uri("/api/v1/help/tasks").header("Authorization", "Bearer valid")
                .exchange().expectStatus().isOk();
        client.get().uri("/api/v1/help/tasks").header("Authorization", "Bearer valid")
                .exchange().expectStatus().isOk();

    }

}
