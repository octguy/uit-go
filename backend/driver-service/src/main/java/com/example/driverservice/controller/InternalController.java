package com.example.driverservice.controller;

import com.example.driverservice.service.IDriverSessonService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/drivers")
public class InternalController {

    private final IDriverSessonService driverSessonService;

    public InternalController(IDriverSessonService driverSessonService) {
        this.driverSessonService = driverSessonService;
    }

    @PostMapping("/create")
    public void createDriver(@RequestParam UUID driverId) {
        driverSessonService.create(driverId);
        System.out.println("Driver created in controller with ID: " + driverId);
    }
}
