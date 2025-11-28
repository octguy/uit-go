package com.example.trip_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripNotificationRequest {

    private UUID tripId;

    private UUID passengerId;

    private String passengerName;

    private Double pickupLatitude;

    private Double pickupLongitude;

    private Double destinationLatitude;

    private Double destinationLongitude;

    private BigDecimal estimatedFare;

    private Double distanceKm;

    private String requestedAt;
}
