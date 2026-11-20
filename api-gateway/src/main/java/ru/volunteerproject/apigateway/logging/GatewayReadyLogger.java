package ru.volunteerproject.apigateway.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import ru.volunteerproject.apigateway.config.GatewayJwtProperties;
import ru.volunteerproject.apigateway.config.GatewayRoutesProperties;

@Component
public class GatewayReadyLogger {

    private static final Logger log = LoggerFactory.getLogger(GatewayReadyLogger.class);

    private final GatewayRoutesProperties routes;
    private final GatewayJwtProperties jwt;

    public GatewayReadyLogger(GatewayRoutesProperties routes, GatewayJwtProperties jwt) {
        this.routes = routes;
        this.jwt = jwt;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logGatewayReady() {
        log.atInfo()
                .addKeyValue("event", "gateway_ready")
                .addKeyValue("authServiceUrl", routes.authServiceUrl())
                .addKeyValue("helpServiceUrl", routes.helpServiceUrl())
                .addKeyValue("issuer", jwt.issuer())
                .addKeyValue("audience", jwt.audience())
                .addKeyValue("rateLimitPolicy", "personal-and-global-per-endpoint")
                .log(
                "gateway_ready authServiceUrl={} helpServiceUrl={} issuer={} audience={} rateLimitPolicy=personal-and-global-per-endpoint",
                routes.authServiceUrl(),
                routes.helpServiceUrl(),
                jwt.issuer(),
                jwt.audience()
                );
    }
}
