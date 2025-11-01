package com.example.user_service.dto;

public class UpdateUserRequest {
    private String name;
    private String phone;

    // Constructors
    public UpdateUserRequest() {}

    public UpdateUserRequest(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}