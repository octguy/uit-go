package com.example.driverservice.entity;

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
public class PendingTripNotification {

    private UUID tripId;

    private UUID passengerId;

    private String passengerName;

    private Double pickupLatitude;

    private Double pickupLongitude;

    private Double destinationLatitude;

    private Double destinationLongitude;

    private BigDecimal estimatedFare;

    private Double distanceKm;

    private LocalDateTime notifiedAt;

    private LocalDateTime expiresAt;

    private boolean expired;

    private boolean accepted;

    private UUID acceptedByDriverId;
}
