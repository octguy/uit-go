package com.example.driver_service.dto;

import java.math.BigDecimal;

public class CreateDriverRequest {
    private Long userId;
    private String vehiclePlate;
    private String vehicleModel;
    private BigDecimal currentLatitude;
    private BigDecimal currentLongitude;

    // Constructors
    public CreateDriverRequest() {}

    public CreateDriverRequest(Long userId, String vehiclePlate, String vehicleModel, 
                             BigDecimal currentLatitude, BigDecimal currentLongitude) {
        this.userId = userId;
        this.vehiclePlate = vehiclePlate;
        this.vehicleModel = vehicleModel;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
    }

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

    public BigDecimal getCurrentLatitude() { return currentLatitude; }
    public void setCurrentLatitude(BigDecimal currentLatitude) { this.currentLatitude = currentLatitude; }

    public BigDecimal getCurrentLongitude() { return currentLongitude; }
    public void setCurrentLongitude(BigDecimal currentLongitude) { this.currentLongitude = currentLongitude; }
}