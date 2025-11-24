package com.example.driversimulator.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class DriverResponse {

    private UUID id;

    private String email;

    private String vehicleModel;

    private String vehicleNumber;

    private LocalDateTime createdAt;
}
