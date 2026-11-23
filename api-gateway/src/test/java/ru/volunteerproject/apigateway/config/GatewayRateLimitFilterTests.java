package ru.volunteerproject.apigateway.config;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Mono;
import ru.volunteerproject.apigateway.ratelimit.FailClosedRedisRateLimiter;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GatewayRateLimitFilterTests {
    @Test
    void rejectedRequestPreservesRetryAfterAndDoesNotReachService() {
        var limiter = mock(FailClosedRedisRateLimiter.class);
        var chain = mock(GatewayFilterChain.class);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/help/tasks"));
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR,
                Route.async().id("listTasks").uri("http://localhost:8082")
                        .predicate(e -> true).build());
        when(limiter.isAllowed("listTasks", "client"))
                .thenReturn(Mono.just(new RateLimiter.Response(false, Map.of("Retry-After", "7"))));
        var filter = GatewayRoutesConfiguration.rateLimitFilter(limiter, e -> Mono.just("client"));
        var error = assertThrows(HttpClientErrorException.class,
                () -> filter.filter(exchange, chain).block());
        assertEquals(429, error.getStatusCode().value());
        assertEquals("7", exchange.getResponse().getHeaders().getFirst("Retry-After"));
        verifyNoInteractions(chain);
    }

    @Test
    void allowedRequestReachesService() {
        var limiter = mock(FailClosedRedisRateLimiter.class);
        var chain = mock(GatewayFilterChain.class);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/help/tasks"));
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR,
                Route.async().id("listTasks").uri("http://localhost:8082")
                        .predicate(e -> true).build());
        when(limiter.isAllowed("listTasks", "client"))
                .thenReturn(Mono.just(new RateLimiter.Response(true, Map.of())));
        when(chain.filter(exchange)).thenReturn(Mono.empty());
        GatewayRoutesConfiguration.rateLimitFilter(limiter, e -> Mono.just("client"))
                .filter(exchange, chain).block();
        verify(chain).filter(exchange);
    }
}
