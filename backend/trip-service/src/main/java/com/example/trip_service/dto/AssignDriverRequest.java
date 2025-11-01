package com.example.trip_service.dto;

public class AssignDriverRequest {
    private Long driverId;

    // Constructors
    public AssignDriverRequest() {}

    public AssignDriverRequest(Long driverId) {
        this.driverId = driverId;
    }

    // Getters and Setters
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
}