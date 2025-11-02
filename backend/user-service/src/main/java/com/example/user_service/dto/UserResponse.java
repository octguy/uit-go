package com.example.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private UUID id;

    private String email;

    private String name;

    private String userType;

    private LocalDateTime createdAt;
    
    // Additional fields to support validation response format
    private boolean valid;
    
    private String status;
    
    private boolean success;
    
    private String message;
}