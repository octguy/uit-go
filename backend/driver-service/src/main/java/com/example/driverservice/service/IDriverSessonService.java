package com.example.driverservice.service;

import com.example.driverservice.entity.DriverSession;

import java.util.List;
import java.util.UUID;

public interface IDriverSessonService {

    void create(UUID driverId);

    List<DriverSession> findAll();
}
