package com.example.driver_service.controller;

import com.example.driver_service.aop.RequireDriver;
import com.example.driver_service.service.DriverStatusService;
import com.example.driver_service.utils.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverStatusService driverStatusService;

    public DriverController(DriverStatusService driverStatusService) {
        this.driverStatusService = driverStatusService;
    }

    @PostMapping("/online-all")
    public ResponseEntity<Void> setAllDriversOnline() {
        driverStatusService.setAllDriversOnline();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/online")
    public ResponseEntity<Void> setOnline() {
        driverStatusService.setOnline();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/offline")
    public ResponseEntity<Void> setOffline() {
        driverStatusService.setOffline();
        return ResponseEntity.ok().build();
    }

    @RequireDriver
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        String driverId = SecurityUtil.getCurrentUserId().toString();
        String status = driverStatusService.getStatus(driverId);
        return ResponseEntity.ok(Map.of(
            "driverId", driverId,
            "status", status != null ? status : "OFFLINE"
        ));
    }
}
