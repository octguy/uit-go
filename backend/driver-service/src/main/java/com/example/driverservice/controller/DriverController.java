package com.example.driverservice.controller;

import com.example.driverservice.service.DriverStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverStatusService driverStatusService;

    public DriverController(DriverStatusService driverStatusService) {
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
}
