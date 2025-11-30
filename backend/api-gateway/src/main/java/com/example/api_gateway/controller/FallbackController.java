package com.example.api_gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Fallback Controller for Circuit Breaker
 * Provides user-friendly error responses when backend services are unavailable
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/user-service")
    @PostMapping("/user-service")
    @PutMapping("/user-service")
    @DeleteMapping("/user-service")
    public ResponseEntity<Map<String, String>> userServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "User Service Unavailable",
                        "message", "The user service is temporarily unavailable. Please try again later.",
                        "service", "user-service"
                ));
    }

    @GetMapping("/trip-service")
    @PostMapping("/trip-service")
    @PutMapping("/trip-service")
    @DeleteMapping("/trip-service")
    public ResponseEntity<Map<String, String>> tripServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Trip Service Unavailable",
                        "message", "The trip service is temporarily unavailable. Please try again later.",
                        "service", "trip-service"
                ));
    }

    @GetMapping("/driver-service")
    @PostMapping("/driver-service")
    @PutMapping("/driver-service")
    @DeleteMapping("/driver-service")
    public ResponseEntity<Map<String, String>> driverServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Driver Service Unavailable",
                        "message", "The driver service is temporarily unavailable. Please try again later.",
                        "service", "driver-service"
                ));
    }
}
