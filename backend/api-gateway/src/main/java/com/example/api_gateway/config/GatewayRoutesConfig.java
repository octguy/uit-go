package com.example.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.time.Duration;

/**
 * Java-based route configuration to ensure filters are properly applied.
 * This replaces YAML-based route configuration to avoid duplicate routes issue.
 * 
 * Circuit Breaker can be enabled/disabled via:
 * - application.yaml: gateway.circuitbreaker.enabled=true/false
 * - Environment variable: GATEWAY_CIRCUITBREAKER_ENABLED=true/false
 * - Kubernetes ConfigMap
 */
@Configuration
public class GatewayRoutesConfig {

    @Value("${gateway.circuitbreaker.enabled:true}")
    private boolean circuitBreakerEnabled;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service Route
                .route("user-service", r -> r
                        .path("/api/users/**")
                        .filters(f -> applyFilters(f, "userServiceCircuitBreaker"))
                        .uri("http://user-service:8081")
                )
                // Trip Service Route
                .route("trip-service", r -> r
                        .path("/api/trips/**")
                        .filters(f -> applyFilters(f, "tripServiceCircuitBreaker"))
                        .uri("http://trip-service:8082")
                )
                // Driver Service Route
                .route("driver-service", r -> r
                        .path("/api/drivers/**")
                        .filters(f -> applyFilters(f, "driverServiceCircuitBreaker"))
                        .uri("http://driver-service:8083")
                )
                .build();
    }

    /**
     * Apply filters based on circuit breaker enabled flag.
     * When disabled, only retry filter is applied.
     */
    private GatewayFilterSpec applyFilters(GatewayFilterSpec filterSpec, String circuitBreakerName) {
        if (circuitBreakerEnabled) {
            filterSpec.circuitBreaker(config -> config
                    .setName(circuitBreakerName)
                    .addStatusCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                    .addStatusCode(String.valueOf(HttpStatus.BAD_GATEWAY.value()))
                    .addStatusCode(String.valueOf(HttpStatus.SERVICE_UNAVAILABLE.value()))
                    .addStatusCode(String.valueOf(HttpStatus.GATEWAY_TIMEOUT.value()))
            );
        }
        
        // Retry filter is always applied
        filterSpec.retry(retryConfig -> retryConfig
                .setRetries(3)
                .setStatuses(HttpStatus.BAD_GATEWAY, HttpStatus.GATEWAY_TIMEOUT)
                .setMethods(HttpMethod.GET, HttpMethod.POST)
                .setBackoff(Duration.ofMillis(100), Duration.ofMillis(500), 2, true)
        );
        
        return filterSpec;
    }
}
