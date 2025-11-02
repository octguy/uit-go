package com.example.trip_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimatedFareResponse {
    private BigDecimal estimatedFare;
    private String currency;
    private Double estimatedDistance;
    private Integer estimatedDuration; // in minutes
    private String routeSummary;
}