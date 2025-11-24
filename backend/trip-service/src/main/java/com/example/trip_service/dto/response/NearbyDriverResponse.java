package com.example.trip_service.dto.response;

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

