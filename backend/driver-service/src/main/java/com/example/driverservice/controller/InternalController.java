package com.example.driverservice.controller;

import com.example.driverservice.service.IDriverSessionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/drivers")
public class InternalController {

    private final IDriverSessionService driverSessionService;

    public InternalController(IDriverSessionService driverSessionService) {
        this.driverSessionService = driverSessionService;
    }

    @PostMapping("/create")
    public void createDriver(@RequestParam UUID driverId) {
        driverSessionService.create(driverId);
        System.out.println("Driver created in controller with ID: " + driverId);
    }
}
