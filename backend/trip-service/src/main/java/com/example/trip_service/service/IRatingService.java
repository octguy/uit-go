package com.example.trip_service.service;

import com.example.trip_service.dto.CreateRatingRequest;
import com.example.trip_service.dto.RatingResponse;
import java.util.UUID;

public interface IRatingService {
    
    /**
     * Create a new rating for a trip
     * @param request the rating creation request
     * @return the created rating response
     */
    RatingResponse createRating(CreateRatingRequest request);
    
    /**
     * Get rating for a specific trip
     * @param tripId the UUID of the trip
     * @return rating for the trip
     */
    RatingResponse getRatingByTripId(UUID tripId);
}