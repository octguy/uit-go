package com.example.driver_service.controller;

import com.example.driver_service.dto.*;
import com.example.driver_service.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    @Autowired
    private DriverService driverService;

    @PostMapping("/register")
    public ResponseEntity<DriverResponse> registerDriver(@RequestBody CreateDriverRequest request) {
        DriverResponse driver = driverService.createDriver(request);
        return ResponseEntity.ok(driver);
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<DriverResponse> getDriver(@PathVariable Long driverId) {
        DriverResponse driver = driverService.getDriverById(driverId);
        return ResponseEntity.ok(driver);
    }

    @PutMapping("/{driverId}/status")
    public ResponseEntity<DriverResponse> updateDriverStatus(@PathVariable Long driverId, @RequestBody UpdateDriverStatusRequest request) {
        DriverResponse driver = driverService.updateDriverStatus(driverId, request);
        return ResponseEntity.ok(driver);
    }

    @PutMapping("/{driverId}/location")
    public ResponseEntity<DriverResponse> updateDriverLocation(@PathVariable Long driverId, @RequestBody UpdateLocationRequest request) {
        DriverResponse driver = driverService.updateDriverLocation(driverId, request);
        return ResponseEntity.ok(driver);
    }

    @GetMapping("/available")
    public ResponseEntity<List<DriverResponse>> getAvailableDrivers() {
        List<DriverResponse> drivers = driverService.getAvailableDrivers();
        return ResponseEntity.ok(drivers);
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<DriverResponse>> getNearbyDrivers(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5.0") Double radiusKm) {
        List<DriverResponse> drivers = driverService.getNearbyDrivers(latitude, longitude, radiusKm);
        return ResponseEntity.ok(drivers);
    }
}