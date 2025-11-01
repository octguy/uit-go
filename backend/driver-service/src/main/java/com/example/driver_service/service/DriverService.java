package com.example.driver_service.service;

import com.example.driver_service.dto.*;
import com.example.driver_service.entity.Driver;
import com.example.driver_service.repository.DriverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DriverService {

    @Autowired
    private DriverRepository driverRepository;

    public DriverResponse createDriver(CreateDriverRequest request) {
        if (driverRepository.existsByUserId(request.getUserId())) {
            throw new RuntimeException("Driver already exists for this user");
        }
        
        if (driverRepository.existsByVehiclePlate(request.getVehiclePlate())) {
            throw new RuntimeException("Vehicle plate already registered");
        }

        Driver driver = new Driver(
            request.getUserId(),
            request.getVehiclePlate(),
            request.getVehicleModel(),
            request.getCurrentLatitude(),
            request.getCurrentLongitude()
        );

        Driver savedDriver = driverRepository.save(driver);
        return convertToResponse(savedDriver);
    }

    public DriverResponse getDriverById(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found"));
        return convertToResponse(driver);
    }

    public DriverResponse updateDriverStatus(Long driverId, UpdateDriverStatusRequest request) {
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found"));

        driver.setStatus(request.getStatus());
        driver.setLastUpdated(LocalDateTime.now());

        Driver updatedDriver = driverRepository.save(driver);
        return convertToResponse(updatedDriver);
    }

    public DriverResponse updateDriverLocation(Long driverId, UpdateLocationRequest request) {
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found"));

        driver.setCurrentLatitude(request.getLatitude());
        driver.setCurrentLongitude(request.getLongitude());
        driver.setLastUpdated(LocalDateTime.now());

        Driver updatedDriver = driverRepository.save(driver);
        return convertToResponse(updatedDriver);
    }

    public List<DriverResponse> getAvailableDrivers() {
        List<Driver> drivers = driverRepository.findByStatus("AVAILABLE");
        return drivers.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public List<DriverResponse> getNearbyDrivers(Double latitude, Double longitude, Double radiusKm) {
        List<Driver> drivers = driverRepository.findNearbyAvailableDrivers(latitude, longitude, radiusKm);
        return drivers.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    private DriverResponse convertToResponse(Driver driver) {
        return new DriverResponse(
            driver.getId(),
            driver.getUserId(),
            driver.getVehiclePlate(),
            driver.getVehicleModel(),
            driver.getStatus(),
            driver.getCurrentLatitude(),
            driver.getCurrentLongitude(),
            driver.getCreatedAt(),
            driver.getLastUpdated()
        );
    }
}