package com.example.trip_service.service.impl;

import com.example.trip_service.dto.*;
import com.example.trip_service.entity.Trip;
import com.example.trip_service.enums.TripStatus;
import com.example.trip_service.repository.TripRepository;
import com.example.trip_service.service.ITripService;
import com.example.trip_service.utility.PricingUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TripServiceImpl implements ITripService {

    private final TripRepository tripRepository;

    public TripServiceImpl(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @Override
    @Transactional
    public TripResponse createTrip(CreateTripRequest request) {
        // Validate coordinates
        validateCoordinates(request);

        // Calculate estimated fare
        BigDecimal fareCents = PricingUtils.calculateFareCents(
                request.getPickupLatitude().doubleValue(),
                request.getPickupLongitude().doubleValue(),
                request.getDestinationLatitude().doubleValue(),
                request.getDestinationLongitude().doubleValue()
        );

        // Convert cents to dollars
        BigDecimal fareInDollars = fareCents.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Create trip entity
        Trip trip = new Trip();
        trip.setPassengerId(request.getPassengerId());
        trip.setStatus(TripStatus.SEARCHING_DRIVER);
        trip.setPickupLocation(request.getPickupLocation());
        trip.setDestination(request.getDestination());
        trip.setPickupLatitude(request.getPickupLatitude().doubleValue());
        trip.setPickupLongitude(request.getPickupLongitude().doubleValue());
        trip.setDestinationLatitude(request.getDestinationLatitude().doubleValue());
        trip.setDestinationLongitude(request.getDestinationLongitude().doubleValue());
        trip.setFare(fareInDollars);

        // Save trip
        Trip savedTrip = tripRepository.save(trip);

        return toTripResponse(savedTrip);
    }

    @Override
    public EstimatedFareResponse getEstimatedFare(CreateTripRequest request) {
        // Validate coordinates
        validateCoordinates(request);

        // Calculate distance
        double distanceKm = PricingUtils.distanceKm(
                request.getPickupLatitude().doubleValue(),
                request.getPickupLongitude().doubleValue(),
                request.getDestinationLatitude().doubleValue(),
                request.getDestinationLongitude().doubleValue()
        );

        // Calculate fare in cents
        BigDecimal fareCents = PricingUtils.calculateFareCents(
                request.getPickupLatitude().doubleValue(),
                request.getPickupLongitude().doubleValue(),
                request.getDestinationLatitude().doubleValue(),
                request.getDestinationLongitude().doubleValue()
        );

        // Convert cents to dollars
        BigDecimal fareInDollars = fareCents.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return EstimatedFareResponse.builder()
                .estimatedFare(fareInDollars)
                .currency("USD")
                .estimatedDistance(distanceKm)
                .estimatedDuration((int) Math.round(distanceKm * 60 / 40)) // Rough estimate: 40 km/h average speed
                .routeSummary("Direct route via main roads")
                .build();
    }

    @Override
    @Transactional
    public TripResponse cancelTrip(UUID tripId, CancelTripRequest request) {
        // Find trip
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + tripId));

        // Check if trip can be cancelled
        if (trip.getStatus() == TripStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed trip");
        }

        if (trip.getStatus() == TripStatus.CANCELLED) {
            throw new IllegalStateException("Trip is already cancelled");
        }

        // Update trip status
        trip.setStatus(TripStatus.CANCELLED);
        trip.setCancelledAt(LocalDateTime.now());

        // Save updated trip
        Trip updatedTrip = tripRepository.save(trip);

        return toTripResponse(updatedTrip);
    }

    @Override
    public TripResponse getTripById(UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + tripId));

        return toTripResponse(trip);
    }

    @Override
    public List<TripResponse> getTripsByPassenger(UUID passengerId) {
        List<Trip> trips = tripRepository.findByPassengerIdOrderByCreatedAtDesc(passengerId);
        return trips.stream()
                .map(this::toTripResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TripResponse> getTripsByDriver(UUID driverId) {
        List<Trip> trips = tripRepository.findByDriverIdOrderByCreatedAtDesc(driverId);
        return trips.stream()
                .map(this::toTripResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TripResponse updateTripStatus(UUID tripId, UpdateTripStatusRequest request) {
        // Find trip
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + tripId));

        // Validate status transition
        TripStatus currentStatus = trip.getStatus();
        TripStatus newStatus = request.getStatus();
        
        validateStatusTransition(currentStatus, newStatus);

        // Update status and corresponding timestamp
        trip.setStatus(newStatus);
        
        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case SEARCHING_DRIVER:
                // No specific timestamp for searching
                break;
            case ACCEPTED:
                trip.setAcceptedAt(now);
                break;
            case IN_PROGRESS:
                trip.setStartedAt(now);
                break;
            case COMPLETED:
                trip.setCompletedAt(now);
                break;
            case CANCELLED:
                trip.setCancelledAt(now);
                break;
        }

        // Save updated trip
        Trip updatedTrip = tripRepository.save(trip);

        return toTripResponse(updatedTrip);
    }

    private void validateStatusTransition(TripStatus currentStatus, TripStatus newStatus) {
        // Define valid status transitions
        boolean isValidTransition = false;
        
        switch (currentStatus) {
            case SEARCHING_DRIVER:
                isValidTransition = newStatus == TripStatus.ACCEPTED || 
                                  newStatus == TripStatus.CANCELLED;
                break;
            case ACCEPTED:
                isValidTransition = newStatus == TripStatus.IN_PROGRESS || 
                                  newStatus == TripStatus.CANCELLED;
                break;
            case IN_PROGRESS:
                isValidTransition = newStatus == TripStatus.COMPLETED || 
                                  newStatus == TripStatus.CANCELLED;
                break;
            case COMPLETED:
            case CANCELLED:
                // Final states - no transitions allowed
                isValidTransition = false;
                break;
        }
        
        if (!isValidTransition) {
            throw new IllegalStateException(
                String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
        }
    }

    @Override
    @Transactional
    public TripResponse assignDriver(UUID tripId, AssignDriverRequest request) {
        // Find trip
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + tripId));

        // Check if trip is in valid state for driver assignment
        if (trip.getStatus() != TripStatus.SEARCHING_DRIVER) {
            throw new IllegalStateException("Trip is not in SEARCHING_DRIVER status");
        }

        // Assign driver
        trip.setDriverId(request.getDriverId());
        trip.setStatus(TripStatus.ACCEPTED);
        trip.setAcceptedAt(LocalDateTime.now());

        // Save updated trip
        Trip updatedTrip = tripRepository.save(trip);

        return toTripResponse(updatedTrip);
    }

    private void validateCoordinates(CreateTripRequest request) {
        if (request.getPickupLatitude() == null || request.getPickupLongitude() == null) {
            throw new IllegalArgumentException("Pickup coordinates are required");
        }

        if (request.getDestinationLatitude() == null || request.getDestinationLongitude() == null) {
            throw new IllegalArgumentException("Destination coordinates are required");
        }

        // Validate latitude range (-90 to 90)
        if (request.getPickupLatitude().abs().compareTo(BigDecimal.valueOf(90)) > 0 ||
                request.getDestinationLatitude().abs().compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }

        // Validate longitude range (-180 to 180)
        if (request.getPickupLongitude().abs().compareTo(BigDecimal.valueOf(180)) > 0 ||
                request.getDestinationLongitude().abs().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    private TripResponse toTripResponse(Trip trip) {
        return TripResponse.builder()
                .id(trip.getId())
                .passengerId(trip.getPassengerId())
                .driverId(trip.getDriverId())
                .status(trip.getStatus().name())
                .pickupLocation(trip.getPickupLocation())
                .destination(trip.getDestination())
                .pickupLatitude(BigDecimal.valueOf(trip.getPickupLatitude()))
                .pickupLongitude(BigDecimal.valueOf(trip.getPickupLongitude()))
                .destinationLatitude(BigDecimal.valueOf(trip.getDestinationLatitude()))
                .destinationLongitude(BigDecimal.valueOf(trip.getDestinationLongitude()))
                .fare(trip.getFare())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .build();
    }
}
