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
    
    @Value("${trip.grpc.service.url:http://trip-grpc-service:50052}")
    private String tripGrpcServiceUrl;

    @Override
    public TripResponse createTrip(CreateTripRequest request) {
        System.out.println("🚀 Pattern 2 POC: Create Trip Request - " + request.getPickupLocation() + " → " + request.getDestination());
        
        // Step 1: Call Trip gRPC Service to create trip (which will validate user via User Service)
        CreateTripGrpcRequest grpcRequest = new CreateTripGrpcRequest();
        grpcRequest.setPassengerId(request.getPassengerId().toString());
        grpcRequest.setPickupLocation(request.getPickupLocation());
        grpcRequest.setDestination(request.getDestination());
        
        CreateTripGrpcResponse grpcResponse = createTripViaGrpc(grpcRequest);
        
        if (!grpcResponse.isSuccess()) {
            throw new RuntimeException("❌ Trip creation failed: " + grpcResponse.getMessage());
        }
        
        System.out.println("✅ Trip created via gRPC: " + grpcResponse.getTripId());
        
        // Step 2: Return response
        TripResponse response = new TripResponse();
        response.setId(UUID.fromString(grpcResponse.getTripId()));
        response.setPassengerId(request.getPassengerId());
        response.setPickupLocation(request.getPickupLocation());
        response.setDestination(request.getDestination());
        response.setStatus("REQUESTED");
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        
        return response;
    }
    
    private CreateTripGrpcResponse createTripViaGrpc(CreateTripGrpcRequest request) {
        System.out.println("📞 Making gRPC call to Trip Service for trip creation");
        
        try {
            String grpcUrl = tripGrpcServiceUrl + "/createTrip";
            ResponseEntity<CreateTripGrpcResponse> response = restTemplate.postForEntity(
                grpcUrl, request, CreateTripGrpcResponse.class);
                
            System.out.println("✅ gRPC response received from Trip Service");
            return response.getBody();
            
        } catch (Exception e) {
            System.err.println("❌ gRPC call failed: " + e.getMessage());
            CreateTripGrpcResponse errorResponse = new CreateTripGrpcResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("gRPC call failed: " + e.getMessage());
            return errorResponse;
        }
    }

    @Override
    public TripResponse getTripById(UUID tripId) {
        System.out.println("🔍 Getting trip by ID: " + tripId);
        
        // For demo purposes, return a mock trip response with proper status
        TripResponse response = new TripResponse();
        response.setId(tripId);
        response.setPassengerId(UUID.randomUUID()); // Mock passenger ID
        response.setPickupLocation("Ben Thanh Market, Ho Chi Minh City");
        response.setDestination("Notre Dame Cathedral, Ho Chi Minh City");
        response.setStatus("REQUESTED"); // This is the key field that was missing!
        response.setFare(new BigDecimal("75000")); // 75,000 VND
        response.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        response.setUpdatedAt(LocalDateTime.now());
        
        System.out.println("✅ Returning trip with status: " + response.getStatus());
        return response;
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

// Inner classes for gRPC communication
class CreateTripGrpcRequest {
    private String passengerId;
    private String pickupLocation;
    private String destination;
    
    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }
    
    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
    
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
}

class CreateTripGrpcResponse {
    private boolean success;
    private String tripId;
    private String message;
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}