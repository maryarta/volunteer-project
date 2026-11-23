package ru.volunteerproject.apigateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GatewayIntegrationTests {

    static final WireMockServer auth = new WireMockServer(wireMockConfig().dynamicPort());

    static final WireMockServer help = new WireMockServer(wireMockConfig().dynamicPort());

    static final GenericContainer<?> redis = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    static {
        auth.start();
        help.start();
        redis.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("gateway.routes.auth-service-url", auth::baseUrl);
        registry.add("gateway.routes.help-service-url", help::baseUrl);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port",
                () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");

        registry.add("security.jwt.secret",
                () -> Base64.getEncoder().encodeToString(
                        "01234567890123456789012345678901"
                                .getBytes(StandardCharsets.UTF_8)));
    }

    @Autowired
    WebTestClient client;

    @BeforeEach
    void resetState() throws Exception {
        auth.resetAll();
        help.resetAll();

        // Очищаем Redis, созданный этим тестовым классом
        var result = redis.execInContainer("redis-cli", "FLUSHALL");
        assertEquals(0, result.getExitCode());
    }

    @Test
    void forwardsPublicRequestToHelpService() {
        help.stubFor(get(urlPathEqualTo("/api/v1/help/tasks"))
                .willReturn(okJson("{\"items\":[]}")));

        client.get()
                .uri("/api/v1/help/tasks")
                .header("X-Request-Id", "integration-test-1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader()
                .valueEquals("X-Request-Id", "integration-test-1")
                .expectBody()
                .json("{\"items\":[]}");

        help.verify(1, getRequestedFor(
                urlPathEqualTo("/api/v1/help/tasks"))
                .withHeader("X-Request-Id", equalTo("integration-test-1")));

        auth.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void rejectsProtectedRequestWithoutToken() {
        client.post()
                .uri("/api/v1/help/tasks")
                .header("X-Request-Id", "unauthorized-test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("unauthorized")
                .jsonPath("$.request_id").isEqualTo("unauthorized-test");

        help.verify(0, anyRequestedFor(anyUrl()));
    }

    @AfterAll
    static void stopDependencies() {
        auth.stop();
        help.stop();
        redis.stop();
    }
}
