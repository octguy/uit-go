package com.example.user_service.dto;

public class CreateUserRequest {
    private String email;
    private String name;
    private String userType; // "PASSENGER" or "DRIVER"
    private String phone;

    // Constructors
    public CreateUserRequest() {}

    public CreateUserRequest(String email, String name, String userType, String phone) {
        this.email = email;
        this.name = name;
        this.userType = userType;
        this.phone = phone;
    }

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}