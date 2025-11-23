package com.example.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(
        name = "driver-service",
        url = "http://driver-service:8083"
)
public interface DriverClient {

    @PostMapping("/api/internal/drivers/create")
    void createDriver(@RequestParam UUID driverId);
}
