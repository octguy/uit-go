package com.example.trip_service.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class AssignDriverRequest {

    private UUID driverId;
}