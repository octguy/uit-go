package com.example.driver_service.controller;

import com.example.driver_service.service.DriverStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
