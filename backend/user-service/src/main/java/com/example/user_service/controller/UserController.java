package com.example.user_service.controller;

import com.example.user_service.dto.*;
import com.example.user_service.service.IUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final IUserService userService;

//    // gRPC client fields
//    @Value("${trip.service.host:localhost}")
//    private String tripServiceHost;
//
//    @Value("${trip.service.port:6565}")
//    private int tripServicePort;
//
//    private ManagedChannel channel;
//    private TripServiceGrpc.TripServiceBlockingStub tripStub;
//
    public UserController(IUserService userService) {
        this.userService = userService;
    }

//    @PostConstruct
//    public void initGrpc() {
//        this.channel = ManagedChannelBuilder.forAddress(tripServiceHost, tripServicePort)
//                .usePlaintext()
//                .build();
//        this.tripStub = TripServiceGrpc.newBlockingStub(channel);
//    }
//
//    @PreDestroy
//    public void shutdownGrpc() {
//        if (this.channel != null && !this.channel.isShutdown()) {
//            this.channel.shutdownNow();
//        }
//    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@RequestBody @Valid CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody @Valid LoginRequest request) {
        try {
            UserResponse user = userService.login(request);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            // invalid credentials
            return ResponseEntity.status(401).build();
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userId) {
        UUID uid = UUID.fromString(userId);
        UserResponse user = userService.getUserById(uid);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable String userId, @RequestBody UpdateUserRequest request) {
        UUID uid = UUID.fromString(userId);
        UserResponse user = userService.updateUser(uid, request);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/type/{userType}")
    public ResponseEntity<List<UserResponse>> getUsersByType(@PathVariable String userType) {
        List<UserResponse> users = userService.getUsersByType(userType);
        return ResponseEntity.ok(users);
    }

    // New: call trip-service via gRPC to list trips for a user
    @GetMapping("/{userId}/trips")
    public ResponseEntity<?> getTripsForUser(@PathVariable String userId) {
//        try {
//            UUID uid = UUID.fromString(userId);
//
//            // Build and send gRPC request - adjust field names to match your proto
//            TripRequest req = TripRequest.newBuilder()
//                    .setUserId(uid.toString())
//                    .build();
//
//            TripListResponse resp = tripStub.getTripsForUser(req); // adjust method name to match proto
//
//            // Return the raw trip list response (or map to DTOs as needed)
//            return ResponseEntity.ok(resp);
//        } catch (IllegalArgumentException ie) {
//            return ResponseEntity.badRequest().body("Invalid userId format");
//        } catch (Exception e) {
//            // log and return a 502-ish response indicating upstream failure
//            System.err.println("Error calling trip-service: " + e.getMessage());
//            return ResponseEntity.status(502).body("Failed to fetch trips from trip-service: " + e.getMessage());
//        }
        return null;
    }
}