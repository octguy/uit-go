package com.example.user_service.dto;

public class ValidateUserResponse {
    private boolean valid;
    private String userType;
    private String message;

    public ValidateUserResponse() {}

    public ValidateUserResponse(boolean valid, String userType, String message) {
        this.valid = valid;
        this.userType = userType;
        this.message = message;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}