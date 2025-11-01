package com.example.driver_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class DriverResponse {
    private UUID id;
    private UUID userId;
    private String vehiclePlate;
    private String vehicleModel;
    private String status;
    private BigDecimal currentLatitude;
    private BigDecimal currentLongitude;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

    // Constructors
    public DriverResponse() {}

    public DriverResponse(UUID id, UUID userId, String vehiclePlate, String vehicleModel, 
                         String status, BigDecimal currentLatitude, BigDecimal currentLongitude,
                         LocalDateTime createdAt, LocalDateTime lastUpdated) {
        this.id = id;
        this.userId = userId;
        this.vehiclePlate = vehiclePlate;
        this.vehicleModel = vehicleModel;
        this.status = status;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.createdAt = createdAt;
        this.lastUpdated = lastUpdated;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getCurrentLatitude() { return currentLatitude; }
    public void setCurrentLatitude(BigDecimal currentLatitude) { this.currentLatitude = currentLatitude; }

    public BigDecimal getCurrentLongitude() { return currentLongitude; }
    public void setCurrentLongitude(BigDecimal currentLongitude) { this.currentLongitude = currentLongitude; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}