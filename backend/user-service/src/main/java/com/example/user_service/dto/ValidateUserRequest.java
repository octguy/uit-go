package com.example.user_service.dto;

import java.util.UUID;

public class ValidateUserRequest {
    private String userId;

    public ValidateUserRequest() {}

    public ValidateUserRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public UUID getUserIdAsUUID() {
        return UUID.fromString(userId);
    }
}