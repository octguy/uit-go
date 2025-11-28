package com.example.driverservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserValidationResponse {

    private UUID userId;

    private String role;

    private boolean valid;
}
