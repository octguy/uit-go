package com.example.trip_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripRequest {

    private UUID passengerId;

    private String pickupLocation;

    private String destination;

    private BigDecimal pickupLatitude;

    private BigDecimal pickupLongitude;

    private BigDecimal destinationLatitude;

    private BigDecimal destinationLongitude;
}