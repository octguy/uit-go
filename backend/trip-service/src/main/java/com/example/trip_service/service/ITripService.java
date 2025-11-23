package com.example.trip_service.service;

import com.example.trip_service.dto.request.CreateTripRequest;
import com.example.trip_service.dto.request.EstimateFareRequest;
import com.example.trip_service.dto.response.EstimateFareResponse;
import com.example.trip_service.dto.response.TripResponse;

import java.util.List;
import java.util.UUID;

public interface ITripService {

    UUID getUserId();

    EstimateFareResponse estimateFare(EstimateFareRequest request);

    TripResponse createTrip(CreateTripRequest request);

    UUID getPassengerId();

    UUID getDriverId();

    TripResponse getTripById(UUID id);

    TripResponse cancelTrip(UUID id);

    TripResponse acceptTrip(UUID id);

    TripResponse completeTrip(UUID id);

    TripResponse startTrip(UUID id);

    List<TripResponse> getAllTrips();

}
