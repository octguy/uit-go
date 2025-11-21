package com.example.trip_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripRequest {

    private String pickupLocation;

    private String destination;

    private BigDecimal pickupLatitude;

    private BigDecimal pickupLongitude;

    private BigDecimal destinationLatitude;

    private BigDecimal destinationLongitude;
}