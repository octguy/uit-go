package com.example.driver_service.controller;

import com.example.driver_service.config.RedisOperationCounter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller to expose Redis operation metrics
 * Useful for measuring read/write patterns before implementing replicas
 */
@RestController
@RequestMapping("/api/driver-service/metrics")
@RequiredArgsConstructor
public class RedisMetricsController {

    private final RedisOperationCounter counter;

    @GetMapping("/redis-ops")
    public Map<String, Object> getRedisOperationStats() {
        long reads = counter.getReadCount();
        long writes = counter.getWriteCount();
        double ratio = writes > 0 ? (double) reads / writes : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReads", reads);
        stats.put("totalWrites", writes);
        stats.put("readWriteRatio", String.format("%.2f:1", ratio));
        stats.put("recommendation", getRecommendation(ratio));

        return stats;
    }

    @PostMapping("/redis-ops/reset")
    public Map<String, String> resetCounters() {
        counter.reset();
        return Map.of("message", "Redis operation counters reset successfully");
    }

    @GetMapping("/redis-ops/print")
    public Map<String, String> printStats() {
        counter.printStats();
        return Map.of("message", "Stats printed to logs");
    }

    private String getRecommendation(double ratio) {
        if (ratio > 10) {
            return "High read ratio - Read replicas recommended";
        } else if (ratio > 5) {
            return "Moderate read ratio - Consider read replicas at scale";
        } else {
            return "Low read ratio - Read replicas may not be necessary yet";
        }
    }
}
