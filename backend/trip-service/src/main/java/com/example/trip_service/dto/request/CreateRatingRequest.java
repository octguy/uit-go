package com.example.trip_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRatingRequest {

    private UUID tripId;
    private UUID raterId; // User who is rating
    private UUID ratedEntityId; // Driver or passenger being rated
    private String ratingType; // "driver", "passenger"
    private Integer rating; // 1-5 stars
    private String comment;
}