package com.example.driverservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripNotificationResponse {

    private UUID tripId;

    private UUID driverId;

    private boolean accepted;

    private String message;
}
