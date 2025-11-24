package com.example.trip_service.client;

import com.example.trip_service.dto.response.NearbyDriverResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "driver-service",
        url = "http://driver-service:8083"
)
public interface DriverClient {

    @GetMapping("/api/internal/drivers/nearby")
    List<NearbyDriverResponse> getNearbyDrivers(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "3.0") double radiusKm,
            @RequestParam(defaultValue = "5") int limit
    );
}
