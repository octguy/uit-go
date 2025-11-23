package com.example.trip_service.service;

import com.example.trip_service.dto.response.TripRatingResponse;

import java.util.UUID;

public interface IRatingService {

    TripRatingResponse rateTrip(UUID tripId, int score);
}