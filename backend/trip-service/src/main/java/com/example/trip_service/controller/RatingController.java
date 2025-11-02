package com.example.trip_service.controller;

import com.example.trip_service.dto.CreateRatingRequest;
import com.example.trip_service.dto.RatingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/trip-service/ratings")
public class RatingController {

    @PostMapping
    public ResponseEntity<RatingResponse> createRating(@RequestBody CreateRatingRequest request) {
        // Mock rating implementation
        RatingResponse response = new RatingResponse();
        response.setId(UUID.randomUUID());
        response.setTripId(request.getTripId());
        response.setRaterId(request.getRaterId());
        response.setRatedEntityId(request.getRatedEntityId());
        response.setRatingType(request.getRatingType());
        response.setRating(request.getRating());
        response.setComment(request.getComment());
        response.setCreatedAt(LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<RatingResponse> getRatingByTrip(@PathVariable UUID tripId) {
        // Mock implementation
        RatingResponse response = new RatingResponse();
        response.setId(UUID.randomUUID());
        response.setTripId(tripId);
        response.setRaterId(UUID.randomUUID());
        response.setRatedEntityId(UUID.randomUUID());
        response.setRatingType("driver");
        response.setRating(5);
        response.setComment("Great trip!");
        response.setCreatedAt(LocalDateTime.now());
                
        return ResponseEntity.ok(response);
    }
}