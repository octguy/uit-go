package com.example.trip_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponse {
    private UUID id;
    private UUID tripId;
    private UUID raterId;
    private UUID ratedEntityId;
    private String ratingType;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}