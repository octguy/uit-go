package com.example.trip_service.service;

import com.example.trip_service.dto.*;

public interface ITripService {
    
    TripResponse createTrip(CreateTripRequest request);
}