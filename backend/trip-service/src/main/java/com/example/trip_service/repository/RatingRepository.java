package com.example.trip_service.repository;

import com.example.trip_service.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {
    
    /**
     * Find rating by trip ID
     * @param tripId the UUID of the trip
     * @return optional rating for the trip
     */
    Optional<Rating> findByTripId(UUID tripId);
    
    /**
     * Check if rating exists for trip
     * @param tripId the UUID of the trip
     * @return true if rating exists
     */
    boolean existsByTripId(UUID tripId);
}