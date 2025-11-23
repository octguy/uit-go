package com.example.driverservice.service.impl;

import com.example.driverservice.entity.DriverSession;
import com.example.driverservice.enums.DriverStatus;
import com.example.driverservice.repository.DriverSessionRepository;
import com.example.driverservice.service.IDriverSessonService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DriverSessionImpl implements IDriverSessonService {

    private final DriverSessionRepository driverSessionRepository;

    public DriverSessionImpl(DriverSessionRepository driverSessionRepository) {
        this.driverSessionRepository = driverSessionRepository;
    }

    @Override
    @Transactional
    public void create(UUID driverId) {
        System.out.println("Driver created in service with ID: " + driverId);

        DriverSession driverSession = new DriverSession(driverId, DriverStatus.OFFLINE, LocalDateTime.now());

        driverSessionRepository.save(driverSession);

    }

    @Override
    public List<DriverSession> findAll() {
        return driverSessionRepository.findAll();
    }
}
