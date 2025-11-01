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
                // User Service Routes
                .route("user-service", r -> r.path("/api/users/**")
                        .uri("http://localhost:8081"))
                
                // Trip Service Routes
                .route("trip-service", r -> r.path("/api/trips/**")
                        .uri("http://localhost:8082"))
                
                // Driver Service Routes
                .route("driver-service", r -> r.path("/api/drivers/**")
                        .uri("http://localhost:8083"))
                
                .build();
    }
}