package com.example.driverservice.controller;

import com.example.driverservice.dto.NearbyDriverResponse;
import com.example.driverservice.service.DriverLocationService;
import com.example.driverservice.service.DriverStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/drivers")
public class DriverController {

    private final DriverStatusService driverStatusService;

    private final DriverLocationService driverLocationService;

    public DriverController(DriverStatusService driverStatusService, DriverLocationService driverlocationService) {
        this.driverLocationService = driverlocationService;
        this.driverStatusService = driverStatusService;
    }

    @PostMapping("/{driverId}/online")
    public ResponseEntity<Void> setOnline(@PathVariable String driverId) {
        driverStatusService.setOnline(driverId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{driverId}/offline")
    public ResponseEntity<Void> setOffline(@PathVariable String driverId) {
        driverStatusService.setOffline(driverId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{driverId}/status")
    public ResponseEntity<String> getStatus(@PathVariable String driverId) {
        String status = driverStatusService.getStatus(driverId);
        return ResponseEntity.ok(status != null ? status : "UNKNOWN");
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
