package com.example.trip_service.dto;

public class UpdateTripStatusRequest {
    private String status;

    // Constructors
    public UpdateTripStatusRequest() {}

    public UpdateTripStatusRequest(String status) {
        this.status = status;
    }

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}