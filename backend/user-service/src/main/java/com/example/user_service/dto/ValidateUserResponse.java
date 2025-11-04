package com.example.user_service.dto;

public class ValidateUserResponse {
    private boolean valid;
    private String userType;
    private String userName;
    private String message;

    public ValidateUserResponse() {}

    public ValidateUserResponse(boolean valid, String userType, String userName, String message) {
        this.valid = valid;
        this.userType = userType;
        this.userName = userName;
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}