package com.example.driver_service.dto;

import java.math.BigDecimal;

public class UpdateLocationRequest {
    private BigDecimal latitude;
    private BigDecimal longitude;

    // Constructors
    public UpdateLocationRequest() {}

    public UpdateLocationRequest(BigDecimal latitude, BigDecimal longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and Setters
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
}