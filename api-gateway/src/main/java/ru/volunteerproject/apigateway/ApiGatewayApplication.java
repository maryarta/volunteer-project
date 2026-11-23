package ru.volunteerproject.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import ru.volunteerproject.apigateway.config.GatewayJwtProperties;
import ru.volunteerproject.apigateway.config.GatewayRateLimitProperties;
import ru.volunteerproject.apigateway.config.GatewayRoutesProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        GatewayJwtProperties.class,
        GatewayRateLimitProperties.class,
        GatewayRoutesProperties.class,
})
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
