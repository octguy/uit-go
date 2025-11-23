package com.example.driverservice.controller;

import com.example.driverservice.entity.DriverSession;
import com.example.driverservice.service.IDriverSessonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/driver-sessions")
public class DriverSessionController {

    private final IDriverSessonService driverSessonService;

    public DriverSessionController(IDriverSessonService driverSessonService) {
        this.driverSessonService = driverSessonService;
    }

    @GetMapping
    public List<DriverSession> findAll() {
        return driverSessonService.findAll();
    }
}
