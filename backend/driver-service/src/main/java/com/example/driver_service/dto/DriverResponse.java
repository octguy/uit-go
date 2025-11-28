package com.example.driver_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class DriverResponse {

    private UUID id;

    private String email;

    private String vehicleModel;

    private String vehicleNumber;

    private LocalDateTime createdAt;
}
