package com.example.driver_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearbyDriverResponse {

    private String driverId;

    private double latitude;

    private double longitude;

    private double distanceInMeters;
}
