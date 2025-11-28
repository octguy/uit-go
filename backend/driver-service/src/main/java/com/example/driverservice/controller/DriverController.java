package com.example.driverservice.controller;

import com.example.driverservice.service.DriverStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/drivers")
public class DriverController {

    private final DriverStatusService driverStatusService;

    public DriverController(DriverStatusService driverStatusService) {
        this.driverStatusService = driverStatusService;
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
