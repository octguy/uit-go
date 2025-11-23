package com.example.driverservice.controller;

import com.example.driverservice.entity.DriverSession;
import com.example.driverservice.repository.DriverSessionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/driver-sessions")
public class DriverSessionController {

    private final DriverSessionRepository driverSessionRepository;

    public DriverSessionController(DriverSessionRepository driverSessionRepository) {
        this.driverSessionRepository = driverSessionRepository;
    }

    @GetMapping
    public List<DriverSession> findAll() {
        return driverSessionRepository.findAll();
    }
}
