package com.example.trip_service.service.impl;

import com.example.trip_service.aop.RequireDriver;
import com.example.trip_service.aop.RequirePassenger;
import com.example.trip_service.aop.RequireUser;
import com.example.trip_service.dto.request.CreateTripRequest;
import com.example.trip_service.dto.request.EstimateFareRequest;
import com.example.trip_service.dto.response.EstimateFareResponse;
import com.example.trip_service.dto.response.TripResponse;
import com.example.trip_service.entity.Trip;
import com.example.trip_service.enums.TripStatus;
import com.example.trip_service.repository.TripRepository;
import com.example.trip_service.service.ITripService;
import com.example.trip_service.utility.PricingUtils;
import com.example.trip_service.utility.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
                .build();
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
                .build();
    }
}
