package ru.volunteerproject.apigateway.filter;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TrustedIdentityGlobalFilterTest {

    @Test
    void setsUserIdFromJwt(){
        String userId = UUID.randomUUID().toString();

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(userId)
                .build();

        var authentication = new JwtAuthenticationToken(jwt);

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/help/tasks")
                        .header("X-User-Id", "fake-user")
                        .header("Authorization", "Bearer test-token")
        );

        var authenticatedExchange = exchange.mutate()
                .principal(Mono.just(authentication))
                .build();

        var chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        new TrustedIdentityGlobalFilter().filter(authenticatedExchange, chain).block();

        var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture()); // упадёт, если chain не вызвался или вызвался 2 раза

        var headers = captor.getValue().getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isEqualTo(userId);
        assertThat(headers.getFirst("Authorization")).isNull();
    }

    @Test
    void deletesHeadersWhenUnauthenticated(){
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/help/tasks")
                        .header("X-User-Id", "fake-user")
                        .header("Authorization", "Bearer test-token")
        );

        var chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        new TrustedIdentityGlobalFilter().filter(exchange, chain).block();

        var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture()); // упадёт, если chain не вызвался или вызвался 2 раза

        var headers = captor.getValue().getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isNull();
        assertThat(headers.getFirst("Authorization")).isNull();
    }
}
