package com.example.trip_service.service.impl;

import com.example.trip_service.dto.*;
import com.example.trip_service.entity.Trip;
import com.example.trip_service.enums.TripStatus;
import com.example.trip_service.repository.TripRepository;
import com.example.trip_service.service.ITripService;
import com.example.trip_service.utility.PricingUtils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
// Fix the protobuf imports - use the new generated classes
import com.example.user.UserServiceGrpc;
import com.example.user.ValidateUserRequest;
import com.example.user.ValidateUserResponse;
import com.example.user.HealthCheckRequest;
import com.example.user.HealthCheckResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Fix annotation imports for newer Java/Spring versions
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class TripServiceImpl implements ITripService {
    private static final Logger logger = Logger.getLogger(TripServiceImpl.class.getName());

    private final TripRepository tripRepository;
    
    @Value("${user.grpc.service.host:localhost}")
    private String userGrpcServiceHost;
    
    @Value("${user.grpc.service.port:50051}")
    private int userGrpcServicePort;

    private ManagedChannel channel;
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    public TripServiceImpl(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @PostConstruct
    public void init() {
        // Create gRPC channel to the Golang service
        this.channel = ManagedChannelBuilder.forAddress(userGrpcServiceHost, userGrpcServicePort)
                .usePlaintext() // Use plaintext for development
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .maxInboundMessageSize(1024 * 1024) // 1MB
                .build();
        
        // Create blocking stub for synchronous calls
        this.userServiceStub = UserServiceGrpc.newBlockingStub(channel);
        
        logger.info("🔌 gRPC channel initialized to " + userGrpcServiceHost + ":" + userGrpcServicePort);
        
        // Test the connection
        testGrpcConnection();
    }
    
    private void testGrpcConnection() {
        try {
            logger.info("🧪 Testing gRPC connection...");
            
            // Try a health check first
            HealthCheckRequest healthRequest = HealthCheckRequest.newBuilder()
                    .setService("user-service-grpc")
                    .build();
                    
            HealthCheckResponse healthResponse = userServiceStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .healthCheck(healthRequest);
                    
            logger.info("✅ gRPC Health Check successful: " + healthResponse.getStatus());
            
        } catch (Exception e) {
            logger.warning("⚠️ gRPC Health Check failed: " + e.getMessage());
            logger.warning("Service may still work for other operations...");
        }
    }

    @PreDestroy
    public void cleanup() throws InterruptedException {
        if (channel != null) {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            logger.info("🔌 gRPC channel closed");
        }
    }

    @Override
    @Transactional
    public TripResponse createTrip(CreateTripRequest request) {
        System.out.println("🚀 Create Trip Request - Validating passenger: " + request.getPassengerId());
        
        // Step 1: Validate passenger via gRPC call to User Service
        try {
            ValidateUserRequest grpcRequest = ValidateUserRequest.newBuilder()
                    .setUserId(request.getPassengerId().toString())
                    .build();
            
            ValidateUserResponse grpcResponse = validatePassengerViaGrpc(grpcRequest);
            
            System.out.println("📋 gRPC Response Details:");
            System.out.println("   Valid: " + grpcResponse.getValid());
            System.out.println("   User ID: " + grpcResponse.getUserId());
            System.out.println("   Success: " + grpcResponse.getSuccess());
            System.out.println("   Status: " + grpcResponse.getStatus());
            System.out.println("   Message: " + grpcResponse.getMessage());
            
            // Check both valid and success flags for comprehensive validation
            if (!grpcResponse.getValid() || !grpcResponse.getSuccess()) {
                String errorMessage = grpcResponse.getMessage().isEmpty() ? 
                    "User validation failed - user not found or inactive" : 
                    grpcResponse.getMessage();
                throw new RuntimeException("❌ Passenger validation failed: " + errorMessage);
            }
            
            System.out.println("✅ Passenger validated via gRPC: " + request.getPassengerId());
            System.out.println("   Status: " + grpcResponse.getStatus());
            
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "❌ gRPC call failed: {0}", e.getStatus());
            throw new RuntimeException("❌ Passenger validation failed: " + e.getMessage());
        }
        
        // Step 2: Validate coordinates
        validateCoordinates(request);

        // Step 3: Calculate estimated fare
        BigDecimal fareCents = PricingUtils.calculateFareCents(
                request.getPickupLatitude().doubleValue(),
                request.getPickupLongitude().doubleValue(),
                request.getDestinationLatitude().doubleValue(),
                request.getDestinationLongitude().doubleValue()
        );

        // Convert cents to dollars
        BigDecimal fareInDollars = fareCents.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Step 4: Create trip entity
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

        // Step 5: Save trip
        Trip savedTrip = tripRepository.save(trip);
        
        System.out.println("✅ Trip created locally: " + savedTrip.getId());

        return toTripResponse(savedTrip);
    }
    
    private ValidateUserResponse validatePassengerViaGrpc(ValidateUserRequest request) {
        System.out.println("📞 Making gRPC call to User Service for passenger validation");
        System.out.println("🔗 Target: " + userGrpcServiceHost + ":" + userGrpcServicePort);
        System.out.println("📝 Request: user_id=" + request.getUserId());
        
        try {
            ValidateUserResponse response = userServiceStub
                    .withDeadlineAfter(10, TimeUnit.SECONDS) // Add timeout
                    .validateUser(request);
            System.out.println("✅ gRPC response received from User Service");
            System.out.println("   Response proto fields:");
            System.out.println("   - valid: " + response.getValid());
            System.out.println("   - user_id: " + response.getUserId());
            System.out.println("   - status: " + response.getStatus());
            System.out.println("   - success: " + response.getSuccess());
            System.out.println("   - message: " + response.getMessage());
            return response;
            
        } catch (StatusRuntimeException e) {
            System.err.println("❌ gRPC call failed with status: " + e.getStatus());
            System.err.println("❌ Error details: " + e.getStatus().getDescription());
            logger.log(Level.WARNING, "gRPC call failed", e);
            throw e;
        }
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
