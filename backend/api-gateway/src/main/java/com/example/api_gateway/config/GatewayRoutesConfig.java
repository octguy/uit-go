package com.example.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
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
 * NOTE: Fallback is intentionally removed so that CircuitBreaker can record failures correctly.
 * When fallback is used, successful fallback execution is counted as success, not failure.
 */
@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service Route
                .route("user-service-with-cb", r -> r
                        .path("/api/users/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("userServiceCircuitBreaker")
                                        // Removed fallback to allow circuit breaker to record failures
                                        // .setFallbackUri("forward:/fallback/user-service")
                                        .addStatusCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                                        .addStatusCode(String.valueOf(HttpStatus.BAD_GATEWAY.value()))
                                        .addStatusCode(String.valueOf(HttpStatus.SERVICE_UNAVAILABLE.value()))
                                        .addStatusCode(String.valueOf(HttpStatus.GATEWAY_TIMEOUT.value()))
                                )
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setStatuses(HttpStatus.BAD_GATEWAY, HttpStatus.GATEWAY_TIMEOUT)
                                        .setMethods(HttpMethod.GET, HttpMethod.POST)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(500), 2, true)
                                )
                        )
                        .uri("http://user-service:8081")
                )
                // Trip Service Route
                .route("trip-service-with-cb", r -> r
                        .path("/api/trips/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("tripServiceCircuitBreaker")
                                        // Removed fallback to allow circuit breaker to record failures
                                        // .setFallbackUri("forward:/fallback/trip-service")
                                        .addStatusCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                                        .addStatusCode(String.valueOf(HttpStatus.BAD_GATEWAY.value()))
                                        .addStatusCode(String.valueOf(HttpStatus.SERVICE_UNAVAILABLE.value()))
                                        .addStatusCode(String.valueOf(HttpStatus.GATEWAY_TIMEOUT.value()))
                                )
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setStatuses(HttpStatus.BAD_GATEWAY, HttpStatus.GATEWAY_TIMEOUT)
                                        .setMethods(HttpMethod.GET, HttpMethod.POST)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(500), 2, true)
                                )
                        )
                        .uri("http://trip-service:8082")
                )
                // Driver Service Route
                .route("driver-service-with-cb", r -> r
                        .path("/api/drivers/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("driverServiceCircuitBreaker")
                                        // Removed fallback to allow circuit breaker to record failures
                                        // .setFallbackUri("forward:/fallback/driver-service")
                                        .addStatusCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                                        .addStatusCode(String.valueOf(HttpStatus.BAD_GATEWAY.value()))
                                        .addStatusCode(String.valueOf(HttpStatus.SERVICE_UNAVAILABLE.value()))
                                        .addStatusCode(String.valueOf(HttpStatus.GATEWAY_TIMEOUT.value()))
                                )
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setStatuses(HttpStatus.BAD_GATEWAY, HttpStatus.GATEWAY_TIMEOUT)
                                        .setMethods(HttpMethod.GET, HttpMethod.POST)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(500), 2, true)
                                )
                        )
                        .uri("http://driver-service:8083")
                )
                .build();
    }
}
