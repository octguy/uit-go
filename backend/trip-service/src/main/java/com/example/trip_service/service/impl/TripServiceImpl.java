package com.example.trip_service.service.impl;

import com.example.trip_service.dto.*;
import com.example.trip_service.entity.Trip;
import com.example.trip_service.repository.TripRepository;
import com.example.trip_service.service.ITripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TripServiceImpl implements ITripService {

    @Autowired
    private TripRepository tripRepository;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${user.grpc.service.url:localhost:50051}")
    private String userGrpcServiceUrl;

    @Override
    public TripResponse createTrip(CreateTripRequest request) {
        System.out.println("🚀 Pattern 1 POC: Create Trip Request - " + request.getPickupLocation() + " → " + request.getDestination());
        
        // Step 1: Validate user via User gRPC Service
        ValidateUserRequest userRequest = new ValidateUserRequest();
        userRequest.setUserId(request.getPassengerId().toString());
        
        ValidateUserResponse userResponse = validateUserViaGrpc(userRequest);
        
        if (!userResponse.isValid()) {
            throw new RuntimeException("❌ User validation failed: " + userResponse.getMessage());
        }
        
        System.out.println("✅ User validated: " + userResponse.getUserName());
        
        // Step 2: Create trip in database
        Trip trip = new Trip();
        trip.setId(UUID.randomUUID());
        trip.setPassengerId(request.getPassengerId());
        trip.setPickupLocation(request.getPickupLocation());
        trip.setDestination(request.getDestination());
        trip.setStatus("REQUESTED");
        trip.setCreatedAt(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());
        
        // Save trip to database
        Trip savedTrip = tripRepository.save(trip);
        System.out.println("✅ Trip created in database: " + savedTrip.getId());
        
        // Step 3: Return response
        TripResponse response = new TripResponse();
        response.setId(savedTrip.getId());
        response.setPassengerId(savedTrip.getPassengerId());
        response.setPickupLocation(savedTrip.getPickupLocation());
        response.setDestination(savedTrip.getDestination());
        response.setStatus(savedTrip.getStatus());
        response.setCreatedAt(savedTrip.getCreatedAt());
        response.setUpdatedAt(savedTrip.getUpdatedAt());
        
        return response;
    }
    
    private ValidateUserResponse validateUserViaGrpc(ValidateUserRequest request) {
        System.out.println("📞 Making gRPC call to User Service for validation");
        
        try {
            String grpcUrl = userGrpcServiceUrl + "/validateUser";
            ResponseEntity<ValidateUserResponse> response = restTemplate.postForEntity(
                grpcUrl, request, ValidateUserResponse.class);
                
            System.out.println("✅ gRPC response received from User Service");
            return response.getBody();
            
        } catch (Exception e) {
            System.err.println("❌ gRPC call failed: " + e.getMessage());
            ValidateUserResponse errorResponse = new ValidateUserResponse();
            errorResponse.setValid(false);
            errorResponse.setMessage("gRPC call failed: " + e.getMessage());
            return errorResponse;
        }
    }

    @Override
    public TripResponse getTripById(UUID tripId) {
        // TODO: Find trip by ID
        // TODO: Handle not found case
        // TODO: Convert to response
        return null;
    }

    @Override
    public TripResponse updateTripStatus(UUID tripId, UpdateTripStatusRequest request) {
        // TODO: Find trip by ID
        // TODO: Update status field
        // TODO: Update timestamp
        // TODO: Save changes
        return null;
    }

    @Override
    public TripResponse assignDriver(UUID tripId, AssignDriverRequest request) {
        // TODO: Find trip by ID
        // TODO: Assign driver ID
        // TODO: Update status to ACCEPTED
        // TODO: Save changes
        return null;
    }

    @Override
    public List<TripResponse> getTripsByPassenger(UUID passengerId) {
        // TODO: Query trips by passenger ID
        // TODO: Convert to response DTOs
        return null;
    }

    @Override
    public List<TripResponse> getTripsByDriver(UUID driverId) {
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
        TripResponse response = new TripResponse();
        response.setId(trip.getId());
        response.setPassengerId(trip.getPassengerId());
        response.setDriverId(trip.getDriverId());
        response.setPickupLocation(trip.getPickupLocation());
        response.setDestination(trip.getDestination());
        response.setStatus(trip.getStatus());
        response.setFare(trip.getFare());
        response.setCreatedAt(trip.getCreatedAt());
        response.setUpdatedAt(trip.getUpdatedAt());
        return response;
    }
}

// Inner classes for gRPC communication with User Service
class ValidateUserRequest {
    private String userId;
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}

class ValidateUserResponse {
    private boolean valid;
    private String userName;
    private String message;
    
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}