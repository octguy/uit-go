package com.example.trip_service.service;

import com.example.trip_service.dto.response.TripRatingResponse;
import com.example.trip_service.entity.Rating;

import java.util.List;
import java.util.UUID;

public interface IRatingService {

    TripRatingResponse rateTrip(UUID tripId, int score);

    List<Rating> getAllRatings();
}