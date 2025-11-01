package com.example.trip_service.service;

import com.example.trip_service.dto.*;
import com.example.trip_service.entity.Trip;
import com.example.trip_service.repository.TripRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TripService {

    @Autowired
    private TripRepository tripRepository;

    public TripResponse createTrip(CreateTripRequest request) {
        Trip trip = new Trip(
            request.getPassengerId(),
            request.getPickupLocation(),
            request.getDestination(),
            request.getPickupLatitude(),
            request.getPickupLongitude(),
            request.getDestinationLatitude(),
            request.getDestinationLongitude()
        );

        // Calculate basic fare (simplified calculation)
        trip.setFare(calculateFare(trip));

        Trip savedTrip = tripRepository.save(trip);
        return convertToResponse(savedTrip);
    }

    public TripResponse getTripById(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new RuntimeException("Trip not found"));
        return convertToResponse(trip);
    }

    public TripResponse updateTripStatus(Long tripId, UpdateTripStatusRequest request) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new RuntimeException("Trip not found"));

        trip.setStatus(request.getStatus());
        trip.setUpdatedAt(LocalDateTime.now());

        Trip updatedTrip = tripRepository.save(trip);
        return convertToResponse(updatedTrip);
    }

    public TripResponse assignDriver(Long tripId, AssignDriverRequest request) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new RuntimeException("Trip not found"));

        trip.setDriverId(request.getDriverId());
        trip.setStatus("ACCEPTED");
        trip.setUpdatedAt(LocalDateTime.now());

        Trip updatedTrip = tripRepository.save(trip);
        return convertToResponse(updatedTrip);
    }

    public List<TripResponse> getTripsByPassenger(Long passengerId) {
        List<Trip> trips = tripRepository.findByPassengerId(passengerId);
        return trips.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public List<TripResponse> getTripsByDriver(Long driverId) {
        List<Trip> trips = tripRepository.findByDriverId(driverId);
        return trips.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    private BigDecimal calculateFare(Trip trip) {
        // Simplified fare calculation - base fare of 50
        return new BigDecimal("50.00");
    }

    private TripResponse convertToResponse(Trip trip) {
        return new TripResponse(
            trip.getId(),
            trip.getPassengerId(),
            trip.getDriverId(),
            trip.getStatus(),
            trip.getPickupLocation(),
            trip.getDestination(),
            trip.getPickupLatitude(),
            trip.getPickupLongitude(),
            trip.getDestinationLatitude(),
            trip.getDestinationLongitude(),
            trip.getFare(),
            trip.getCreatedAt(),
            trip.getUpdatedAt()
        );
    }
}