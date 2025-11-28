package com.example.trip_service.service.impl;

import com.example.trip_service.aop.passengerAuth.RequirePassenger;
import com.example.trip_service.dto.response.TripRatingResponse;
import com.example.trip_service.entity.Rating;
import com.example.trip_service.entity.Trip;
import com.example.trip_service.enums.TripStatus;
import com.example.trip_service.repository.RatingRepository;
import com.example.trip_service.repository.TripRepository;
import com.example.trip_service.service.IRatingService;
import com.example.trip_service.util.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class RatingServiceImpl implements IRatingService {

    private final RatingRepository ratingRepository;

    private final TripRepository tripRepository;

    public RatingServiceImpl(RatingRepository ratingRepository, TripRepository tripRepository) {
        this.tripRepository = tripRepository;
        this.ratingRepository = ratingRepository;
    }

    @Override
    @RequirePassenger
    @Transactional
    public TripRatingResponse rateTrip(UUID tripId, int score, String comment) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + tripId));

        System.out.println("Passenger id: " + trip.getPassengerId());
        System.out.println("The request user: " + SecurityUtil.getCurrentUserId());

        if (!Objects.equals(trip.getPassengerId().toString(), SecurityUtil.getCurrentUserId().toString())) {
            throw new RuntimeException("Unauthorized to rate this trip");
        }

        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new RuntimeException("Trip status is not COMPLETED");
        }

        Rating rating = new Rating();

        rating.setTrip(trip);
        rating.setScore(score);

        ratingRepository.save(rating);

        return TripRatingResponse.builder()
                .tripId(tripId)
                .passengerId(trip.getPassengerId())
                .driverId(trip.getDriverId())
                .rating(score)
                .comment(comment)
                .createdAt(rating.getCreatedAt())
                .build();
    }

    @Override
    public List<Rating> getAllRatings() {
        return ratingRepository.findAll();
    }
}
