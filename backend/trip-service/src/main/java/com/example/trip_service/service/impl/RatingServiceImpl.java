package com.example.trip_service.service.impl;

import com.example.trip_service.dto.CreateRatingRequest;
import com.example.trip_service.dto.RatingResponse;
import com.example.trip_service.entity.Rating;
import com.example.trip_service.entity.Trip;
import com.example.trip_service.enums.TripStatus;
import com.example.trip_service.repository.RatingRepository;
import com.example.trip_service.repository.TripRepository;
import com.example.trip_service.service.IRatingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RatingServiceImpl implements IRatingService {

    private final RatingRepository ratingRepository;
    private final TripRepository tripRepository;

    public RatingServiceImpl(RatingRepository ratingRepository, TripRepository tripRepository) {
        this.ratingRepository = ratingRepository;
        this.tripRepository = tripRepository;
    }

    @Override
    @Transactional
    public RatingResponse createRating(CreateRatingRequest request) {
//        // Validate rating range
//        if (request.getRating() < 1 || request.getRating() > 5) {
//            throw new IllegalArgumentException("Rating must be between 1 and 5");
//        }
//
//        // Check if trip exists
//        Trip trip = tripRepository.findById(request.getTripId())
//                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + request.getTripId()));
//
//        // Verify trip is completed
//        if (trip.getStatus() != TripStatus.COMPLETED) {
//            throw new IllegalStateException("Can only rate completed trips");
//        }
//
//        // Check if rating already exists for this trip
//        if (ratingRepository.existsByTripId(request.getTripId())) {
//            throw new IllegalStateException("Rating already exists for this trip");
//        }
//
//        // Create rating entity
//        Rating rating = new Rating();
//        rating.setTrip(trip);
//        rating.setScore(request.getRating());
//        rating.setComment(request.getComment());
//
//        // Save rating
//        Rating savedRating = ratingRepository.save(rating);
//
//        // Convert to response
//        return toRatingResponse(savedRating);
        return null;
    }

    @Override
    public RatingResponse getRatingByTripId(UUID tripId) {
//        Rating rating = ratingRepository.findByTripId(tripId)
//                .orElseThrow(() -> new RuntimeException("Rating not found for trip: " + tripId));
//
//        return toRatingResponse(rating);
        return null;
    }

    private RatingResponse toRatingResponse(Rating rating) {
//        RatingResponse response = new RatingResponse();
//        response.setId(rating.getId());
//        response.setTripId(rating.getTrip().getId());
//        response.setRaterId(null); // Not available in current entity
//        response.setRatedEntityId(null); // Not available in current entity
//        response.setRatingType("trip"); // Default value
//        response.setRating(rating.getScore());
//        response.setComment(rating.getComment());
//        response.setCreatedAt(rating.getCreatedAt());
//        return response;
        return null;
    }
}
