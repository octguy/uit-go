package com.example.trip_service.service.impl;

import com.example.trip_service.dto.*;
import com.example.trip_service.entity.Trip;
import com.example.trip_service.repository.TripRepository;
import com.example.trip_service.service.ITripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.PostConstruct;
import user.UserGrpc;
import user.UserOuterClass.ValidateUserRequest;
import user.UserOuterClass.ValidateUserResponse;

@Service
public class TripServiceImpl implements ITripService {

    @Autowired
    private TripRepository tripRepository;
    
    @Value("${user.grpc.service.url:user-grpc-service:50051}")
    private String userGrpcServiceUrl;

    // Added reusable ManagedChannel and Stub
    private ManagedChannel channel;
    private UserGrpc.UserBlockingStub userServiceStub;

    @PostConstruct
    public void initGrpcClient() {
        channel = ManagedChannelBuilder.forTarget(userGrpcServiceUrl)
            .usePlaintext()
            .build();
        userServiceStub = UserGrpc.newBlockingStub(channel);
    }

    @PreDestroy
    public void shutdownGrpcClient() {
        if (channel != null) {
            channel.shutdown();
        }
    }

    @Override
    public TripResponse createTrip(CreateTripRequest request) {
        System.out.println("🚀 Create Trip Request - " + request.getPickupLocation() + " → " + request.getDestination());
        
        // Step 1: Validate user via User gRPC Service
        ValidateUserRequest userRequest = ValidateUserRequest.newBuilder()
            .setUserId(request.getPassengerId().toString())
            .build();
        
        ValidateUserResponse userResponse = validateUserViaGrpc(userRequest);
        
        if (!userResponse.getValid()) {
            throw new RuntimeException("❌ User validation failed: " + userResponse.getMessage());
        }
        
        System.out.println("✅ User validated: " + userResponse.getUserName());
        
        // Step 2: Create trip in database
        Trip trip = new Trip();
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
    
    // Updated validateUserViaGrpc to use reusable Stub
    private ValidateUserResponse validateUserViaGrpc(ValidateUserRequest request) {
        System.out.println("📞 Making gRPC call to User Service for validation");

        try {
            ValidateUserResponse response = userServiceStub.validateUser(request);
            System.out.println("✅ gRPC response received from User Service");
            return response;
        } catch (Exception e) {
            System.err.println("❌ gRPC call failed: " + e.getMessage());
            return ValidateUserResponse.newBuilder()
                .setValid(false)
                .setMessage("gRPC call failed: " + e.getMessage())
                .build();
        }
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