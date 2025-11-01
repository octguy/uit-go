package com.example.user_service.dto;

import java.time.LocalDateTime;

public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private String userType;
    private String phone;
    private LocalDateTime createdAt;

    // Constructors
    public UserResponse() {}

    public UserResponse(Long id, String email, String name, String userType, String phone, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.userType = userType;
        this.phone = phone;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}