package com.example.trip_service.service;

import com.example.trip_service.dto.*;
import java.util.List;
import java.util.UUID;

public interface ITripService {
    
    TripResponse createTrip(CreateTripRequest request);
    
    TripResponse getTripById(UUID tripId);
    
    TripResponse updateTripStatus(UUID tripId, UpdateTripStatusRequest request);
    
    TripResponse assignDriver(UUID tripId, AssignDriverRequest request);
    
    List<TripResponse> getTripsByPassenger(UUID passengerId);
    
    List<TripResponse> getTripsByDriver(UUID driverId);
}