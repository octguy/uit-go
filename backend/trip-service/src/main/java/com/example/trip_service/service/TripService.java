package com.example.trip_service.service;

import com.example.trip_service.dto.*;
import com.example.trip_service.entity.Trip;
import com.example.trip_service.repository.TripRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TripService {

    @Autowired
    private TripRepository tripRepository;

    public TripResponse createTrip(CreateTripRequest request) {
        // TODO: Validate trip data
        // TODO: Create new trip entity
        // TODO: Calculate fare based on distance
        // TODO: Save to database
        // TODO: Return trip response
        return null;
    }

    public TripResponse getTripById(Long tripId) {
        // TODO: Find trip by ID
        // TODO: Handle not found case
        // TODO: Convert to response
        return null;
    }

    public TripResponse updateTripStatus(Long tripId, UpdateTripStatusRequest request) {
        // TODO: Find trip by ID
        // TODO: Update status field
        // TODO: Update timestamp
        // TODO: Save changes
        return null;
    }

    public TripResponse assignDriver(Long tripId, AssignDriverRequest request) {
        // TODO: Find trip by ID
        // TODO: Assign driver ID
        // TODO: Update status to ACCEPTED
        // TODO: Save changes
        return null;
    }

    public List<TripResponse> getTripsByPassenger(Long passengerId) {
        // TODO: Query trips by passenger ID
        // TODO: Convert to response DTOs
        return null;
    }

    public List<TripResponse> getTripsByDriver(Long driverId) {
        // TODO: Query trips by driver ID
        // TODO: Convert to response DTOs
        return null;
    }

    private BigDecimal calculateFare(Trip trip) {
        // TODO: Calculate fare based on distance
        // TODO: Apply surge pricing if applicable
        // TODO: Consider time of day multipliers
        return null;
    }

    private TripResponse convertToResponse(Trip trip) {
        // TODO: Map entity fields to DTO
        return null;
    }
}