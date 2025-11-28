package com.example.driverservice.controller;

import com.example.driverservice.dto.NearbyDriverResponse;
import com.example.driverservice.service.DriverLocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/drivers")
public class InternalController {

    private final DriverLocationService driverLocationService;

    public InternalController(DriverLocationService driverlocationService) {
        this.driverLocationService = driverlocationService;
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyDriverResponse>> getNearbyDrivers(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "3.0") double radiusKm,
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<NearbyDriverResponse> drivers =
                driverLocationService.findNearbyDrivers(lat, lng, radiusKm, limit);

        return ResponseEntity.ok(drivers);
    }
}
