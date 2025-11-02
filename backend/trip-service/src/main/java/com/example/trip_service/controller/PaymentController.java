package com.example.trip_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/trip-service/payments")
public class PaymentController {

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> request) {
        // Mock payment processing implementation
        Map<String, Object> response = new HashMap<>();
        response.put("paymentId", UUID.randomUUID().toString());
        response.put("tripId", request.get("tripId"));
        response.put("amount", request.getOrDefault("amount", new BigDecimal("25.50")));
        response.put("currency", "USD");
        response.put("status", "COMPLETED");
        response.put("paymentMethod", request.getOrDefault("paymentMethod", "CREDIT_CARD"));
        response.put("transactionId", "TXN-" + System.currentTimeMillis());
        response.put("processedAt", LocalDateTime.now());
        response.put("success", true);
        response.put("message", "Payment processed successfully");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<Map<String, Object>> getPayment(@PathVariable String paymentId) {
        Map<String, Object> response = new HashMap<>();
        response.put("paymentId", paymentId);
        response.put("status", "COMPLETED");
        response.put("amount", new BigDecimal("25.50"));
        response.put("currency", "USD");
        response.put("success", true);
        
        return ResponseEntity.ok(response);
    }
}