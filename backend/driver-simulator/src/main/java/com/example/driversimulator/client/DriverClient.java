package com.example.driversimulator.client;

import com.example.driversimulator.dto.DriverResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(
        name = "user-service",
        url = "http://user-service:8081"
)
public interface DriverClient {

    @GetMapping("/api/internal/drivers")
    List<DriverResponse> getAllDrivers();
}
