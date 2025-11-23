package com.example.trip_service.service.impl;

import com.example.trip_service.aop.driverAuth.RequireDriver;
import com.example.trip_service.aop.passengerAuth.RequirePassenger;
import com.example.trip_service.aop.userAuth.RequireUser;
import com.example.trip_service.dto.request.CreateTripRequest;
import com.example.trip_service.dto.request.EstimateFareRequest;
import com.example.trip_service.dto.response.EstimateFareResponse;
import com.example.trip_service.dto.response.TripResponse;
import com.example.trip_service.entity.Trip;
import com.example.trip_service.enums.TripStatus;
import com.example.trip_service.repository.TripRepository;
import com.example.trip_service.service.ITripService;
import com.example.trip_service.util.PricingUtils;
import com.example.trip_service.util.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TripServiceImpl implements ITripService {

    private final TripRepository tripRepository;

    public TripServiceImpl(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @Override
    @RequireUser
    public UUID getUserId() {
        return SecurityUtil.getCurrentUserId();
    }

    @Override
    @RequirePassenger
    public EstimateFareResponse estimateFare(EstimateFareRequest request) {
        Double distance = PricingUtils.calculateDistanceInKm(request);

        BigDecimal fare = PricingUtils.calculateFareCents(request);

        return EstimateFareResponse.builder()
                .distance(distance)
                .fare(fare)
                .build();
    }

    @Override
    @RequirePassenger
    @Transactional
    public TripResponse createTrip(CreateTripRequest request) {
        Trip trip = new Trip();

        trip.setPassengerId(SecurityUtil.getCurrentUserId());
        trip.setPickupLatitude(request.getPickupLatitude());
        trip.setPickupLongitude(request.getPickupLongitude());
        trip.setDestinationLatitude(request.getDestinationLatitude());
        trip.setDestinationLongitude(request.getDestinationLongitude());
        trip.setFare(request.getEstimatedFare());
        trip.setStatus(TripStatus.SEARCHING_DRIVER);

        System.out.println("Trip before save: " + trip.getId() + ", request: " + trip.getRequestedAt());

        return getTripResponse(trip);
    }

    @Override
    @RequirePassenger
    public UUID getPassengerId() {
        return SecurityUtil.getCurrentUserId();
    }

    @Override
    @RequireDriver
    public UUID getDriverId() {
        return SecurityUtil.getCurrentUserId();
    }

    @Override
    @RequireUser
    public TripResponse getTripById(UUID id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));

        return TripResponse.builder()
                .id(trip.getId())
                .passengerId(trip.getPassengerId())
                .driverId(trip.getDriverId())
                .status(trip.getStatus().name())
                .pickupLatitude(trip.getPickupLatitude())
                .pickupLongitude(trip.getPickupLongitude())
                .destinationLatitude(trip.getDestinationLatitude())
                .destinationLongitude(trip.getDestinationLongitude())
                .fare(trip.getFare())
                .requestedAt(trip.getRequestedAt())
                .startedAt(trip.getStartedAt())
                .completedAt(trip.getCompletedAt())
                .cancelledAt(trip.getCancelledAt())
                .build();
    }

    @Override
    @RequirePassenger
    @Transactional
    public TripResponse cancelTrip(UUID id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));

        if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
            throw new RuntimeException("Cannot cancel a completed or already cancelled trip");
        }

        trip.setStatus(TripStatus.CANCELLED);
        trip.setCancelledAt(LocalDateTime.now());

        return getTripResponse(trip);
    }

    @Override
    @RequireDriver
    @Transactional
    public TripResponse acceptTrip(UUID id) {
        UUID driverId = SecurityUtil.getCurrentUserId();

        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));

        if (trip.getStatus() != TripStatus.SEARCHING_DRIVER) {
            throw new RuntimeException("Trip is not available for acceptance");
        }

        trip.setDriverId(driverId);
        trip.setStatus(TripStatus.ACCEPTED);

        return getTripResponse(trip);
    }

    @Override
    @RequireDriver
    @Transactional
    public TripResponse completeTrip(UUID id) {
        UUID driverId = SecurityUtil.getCurrentUserId();

        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));

        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new RuntimeException("Trip is not in progress and cannot be completed");
        }

        if (!trip.getDriverId().equals(driverId)) {
            throw new RuntimeException("You are not authorized to complete this trip");
        }

        trip.setStatus(TripStatus.COMPLETED);
        trip.setCompletedAt(LocalDateTime.now());

        return getTripResponse(trip);
    }

    @Override
    @RequireDriver
    @Transactional
    public TripResponse startTrip(UUID id) {
        UUID driverId = SecurityUtil.getCurrentUserId();

        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));

        if (trip.getStatus() != TripStatus.ACCEPTED) {
            throw new RuntimeException("Trip is not accepted and cannot be started");
        }

        if (!trip.getDriverId().equals(driverId)) {
            throw new RuntimeException("You are not authorized to start this trip");
        }

        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setStartedAt(LocalDateTime.now());

        return getTripResponse(trip);
    }

    private TripResponse getTripResponse(Trip trip) {
        trip = tripRepository.save(trip);

        return TripResponse.builder()
                .id(trip.getId())
                .passengerId(trip.getPassengerId())
                .driverId(trip.getDriverId())
                .status(trip.getStatus().name())
                .pickupLatitude(trip.getPickupLatitude())
                .pickupLongitude(trip.getPickupLongitude())
                .destinationLatitude(trip.getDestinationLatitude())
                .destinationLongitude(trip.getDestinationLongitude())
                .fare(trip.getFare())
                .requestedAt(trip.getRequestedAt())
                .startedAt(trip.getStartedAt())
                .completedAt(trip.getCompletedAt())
                .cancelledAt(trip.getCancelledAt())
                .build();
    }
}
