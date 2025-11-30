package com.example.api_gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Resilience4JConfig {

    private static final Logger log = LoggerFactory.getLogger(Resilience4JConfig.class);

    /**
     * Configures the ReactiveResilience4JCircuitBreakerFactory to use default settings.
     * 
     * IMPORTANT: We use recordException predicate to record ALL exceptions as failures.
     * This is necessary because when Linkerd returns 503, Spring Cloud Gateway throws
     * various exceptions that need to be caught.
     */
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(10)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .minimumNumberOfCalls(5)
                        .automaticTransitionFromOpenToHalfOpenEnabled(true)
                        // Record ALL exceptions as failures - this catches any error from downstream
                        .recordException(throwable -> {
                            log.warn("Circuit Breaker recording exception: {} - {}", 
                                    throwable.getClass().getName(), throwable.getMessage());
                            return true; // Record ALL exceptions as failures
                        })
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(5))
                        .cancelRunningFuture(true)
                        .build())
                .build());
    }
}
