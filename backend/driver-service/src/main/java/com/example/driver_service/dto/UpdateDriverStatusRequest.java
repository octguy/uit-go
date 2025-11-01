package com.example.driver_service.dto;

public class UpdateDriverStatusRequest {
    private String status;

    // Constructors
    public UpdateDriverStatusRequest() {}

    public UpdateDriverStatusRequest(String status) {
        this.status = status;
    }

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}