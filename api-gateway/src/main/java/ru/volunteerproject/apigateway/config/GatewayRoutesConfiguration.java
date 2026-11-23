package ru.volunteerproject.apigateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Mono;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.UriSpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.volunteerproject.apigateway.error.GatewayNotFoundHandler;
import ru.volunteerproject.apigateway.ratelimit.ClientIpKeyResolver;
import ru.volunteerproject.apigateway.ratelimit.FailClosedRedisRateLimiter;
import ru.volunteerproject.apigateway.ratelimit.GlobalKeyResolver;
import ru.volunteerproject.apigateway.ratelimit.RateLimitMetrics.Scope;
import ru.volunteerproject.apigateway.ratelimit.UserKeyResolver;

@Configuration(proxyBeanMethods = false)
public class GatewayRoutesConfiguration {

    @Bean
    RouteLocator gatewayRoutes(
            RouteLocatorBuilder builder,
            GatewayRouteCatalog catalog,
            GatewayRoutesProperties routes,
            GatewayRateLimitProperties rateLimits,
            FailClosedRedisRateLimiter rateLimiter,
            @Qualifier("globalRateLimiter") FailClosedRedisRateLimiter globalRateLimiter,
            UserKeyResolver userKeyResolver,
            ClientIpKeyResolver clientIpKeyResolver,
            GlobalKeyResolver globalKeyResolver,
            GatewayNotFoundHandler notFoundHandler
    ) {
        rateLimits.validateRoutes(catalog.routeIds());

        for (GatewayRouteSpec route : catalog.routes()) {
            rateLimiter.getConfig().put(route.id(), minuteLimit(rateLimits.limitFor(route.id())));
            rateLimiter.setScope(route.id(), switch (route.clientKeyType()) {
                case USER -> Scope.USER;
                case IP -> Scope.IP;
            });
            globalRateLimiter.getConfig().put(
                    route.id(), minuteLimit(rateLimits.globalLimitFor(route.id())));
            globalRateLimiter.setScope(route.id(), Scope.GLOBAL);
        }

        RouteLocatorBuilder.Builder routesBuilder = builder.routes();
        for (GatewayRouteSpec routeSpec : catalog.routes()) {
            KeyResolver clientKeyResolver = clientKeyResolverFor(
                    routeSpec, userKeyResolver, clientIpKeyResolver);
            String targetUrl = targetUrlFor(routeSpec, routes);
            routesBuilder.route(routeSpec.id(), route -> route
                    .path(routeSpec.paths().toArray(String[]::new))
                    .and().method(routeSpec.methods().toArray(HttpMethod[]::new))
                    .filters(filters -> rateLimit(filters, rateLimiter, clientKeyResolver,
                            globalRateLimiter, globalKeyResolver))
                    .uri(targetUrl));
        }

        return routesBuilder
                .route("api-not-found", route -> route
                        .path("/api/v1/**")
                        .filters(filters -> filters.filter(
                                (exchange, chain) -> notFoundHandler.handle(exchange)))
                        .uri("no://op"))
                .build();
    }

    private static KeyResolver clientKeyResolverFor(
            GatewayRouteSpec route,
            UserKeyResolver userKeyResolver,
            ClientIpKeyResolver clientIpKeyResolver
    ) {
        return switch (route.clientKeyType()) {
            case USER -> userKeyResolver;
            case IP -> clientIpKeyResolver;
        };
    }

    private static String targetUrlFor(
            GatewayRouteSpec route,
            GatewayRoutesProperties routes
    ) {
        return switch (route.targetService()) {
            case AUTH -> routes.authServiceUrl().toString();
            case HELP -> routes.helpServiceUrl().toString();
        };
    }

    private static UriSpec rateLimit(
            GatewayFilterSpec filters,
            FailClosedRedisRateLimiter rateLimiter,
            KeyResolver keyResolver,
            FailClosedRedisRateLimiter globalRateLimiter,
            GlobalKeyResolver globalKeyResolver
    ) {
        return filters.filter(rateLimitFilter(rateLimiter, keyResolver))
                .filter(rateLimitFilter(globalRateLimiter, globalKeyResolver));
    }

    static GatewayFilter rateLimitFilter(
            FailClosedRedisRateLimiter limiter, KeyResolver keyResolver
    ) {
        return (exchange, chain) -> keyResolver.resolve(exchange)
                .defaultIfEmpty("")
                .flatMap(key -> {
                    if (key.isEmpty()) {
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        return exchange.getResponse().setComplete();
                    }
                    org.springframework.cloud.gateway.route.Route route =
                            exchange.getRequiredAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
                    return limiter.isAllowed(route.getId(), key).flatMap(result -> {
                        result.getHeaders().forEach((name, value) ->
                                exchange.getResponse().getHeaders().set(name, value));
                        if (!result.isAllowed()) {
                            return Mono.error(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));
                        }
                        return chain.filter(exchange);
                    });
                });
    }

    private static FailClosedRedisRateLimiter.Config minuteLimit(int limit) {
        return new FailClosedRedisRateLimiter.Config()
                .setReplenishRate(limit)
                .setBurstCapacity(limit * 60L)
                .setRequestedTokens(60);
    }
}
