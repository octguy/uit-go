package com.example.trip_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TripRatingResponse {

    private UUID tripId;

    private UUID passengerId;

    private UUID driverId;

    private int rating;

    private LocalDateTime createdAt;
}
