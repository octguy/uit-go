package com.example.trip_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {

    private UUID id;

    private UUID passengerId;

    private UUID driverId;

    private String status;

    private String pickupLocation;

    private String destination;

    private BigDecimal pickupLatitude;

    private BigDecimal pickupLongitude;

    private BigDecimal destinationLatitude;

    private BigDecimal destinationLongitude;

    private BigDecimal fare;

    private LocalDateTime requestedAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}