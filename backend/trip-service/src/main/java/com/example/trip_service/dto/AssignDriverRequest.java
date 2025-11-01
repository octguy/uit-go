package com.example.trip_service.dto;

import java.util.UUID;

public class AssignDriverRequest {
    private UUID driverId;

    // Constructors
    public AssignDriverRequest() {}

    public AssignDriverRequest(UUID driverId) {
        this.driverId = driverId;
    }

    // Getters and Setters
    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }
}