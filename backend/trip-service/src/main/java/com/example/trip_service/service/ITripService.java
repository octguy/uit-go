package com.example.trip_service.service;

import com.example.trip_service.dto.*;
import java.util.List;
import java.util.UUID;

public interface ITripService {

    UserValidationResponse validateToken(String token);

//    /**
//     * Create a new trip request with estimated fare
//     * @param request the trip creation request
//     * @return the created trip with estimated fare
//     */
//    TripResponse createTrip(CreateTripRequest request);
//
//    /**
//     * Get estimated fare for a trip route
//     * @param request the trip request with pickup and destination coordinates
//     * @return estimated fare information
//     */
//    EstimatedFareResponse getEstimatedFare(CreateTripRequest request);
//
//    /**
//     * Cancel an existing trip
//     * @param tripId the UUID of the trip to cancel
//     * @param request optional cancellation details
//     * @return the updated trip response
//     */
//    TripResponse cancelTrip(UUID tripId, CancelTripRequest request);
//
//    /**
//     * Get trip by ID
//     * @param tripId the UUID of the trip
//     * @return the trip response
//     */
//    TripResponse getTripById(UUID tripId);
//
//    /**
//     * Update trip status
//     * @param tripId the UUID of the trip
//     * @param request the status update request
//     * @return the updated trip response
//     */
//    TripResponse updateTripStatus(UUID tripId, UpdateTripStatusRequest request);
//
//    /**
//     * Get all trips for a passenger
//     * @param passengerId the UUID of the passenger
//     * @return list of trip responses
//     */
//    List<TripResponse> getTripsByPassenger(UUID passengerId);
//
//    /**
//     * Get all trips for a driver
//     * @param driverId the UUID of the driver
//     * @return list of trip responses
//     */
//    List<TripResponse> getTripsByDriver(UUID driverId);
//
//    /**
//     * Assign a driver to a trip
//     * @param tripId the UUID of the trip
//     * @param request the driver assignment request
//     * @return the updated trip response
//     */
//    TripResponse assignDriver(UUID tripId, AssignDriverRequest request);
}
