package com.example.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service Routes (no context path)
                .route("user-service", r -> r.path("/api/users/**")
                        .uri("http://user-service:8081"))
                
                // Trip Service Routes - Main trips API (no context path)
                .route("trip-service", r -> r.path("/api/trips/**")
                        .uri("http://trip-service:8082"))
                
                // Driver Service Routes (with path rewriting for context path)
                .route("driver-service", r -> r.path("/api/drivers/**")
                        .filters(f -> f.rewritePath("/api/drivers/(?<segment>.*)", "/api/driver-service/api/drivers/$\\{segment}"))
                        .uri("http://driver-service:8083"))
                
                .build();
    }
}