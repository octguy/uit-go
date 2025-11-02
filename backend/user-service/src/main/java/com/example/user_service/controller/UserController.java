package com.example.user_service.controller;

import com.example.user_service.dto.*;
import com.example.user_service.service.IUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @GetMapping("/status/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "user-service");
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "User Service is running");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody @Valid CreateUserRequest request) {
        try {
            UserResponse user = userService.createUser(request);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Invalid registration data: " + e.getMessage(),
                "error", "INVALID_REQUEST"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error creating user account: " + e.getMessage(),
                "error", "INTERNAL_ERROR"
            ));
        }
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

    @GetMapping("/{userId}/validate")
    public ResponseEntity<Map<String, Object>> validateUser(@PathVariable String userId) {
        try {
            UUID uid = UUID.fromString(userId);
            UserResponse user = userService.validateUser(uid);
            
            // Return the format expected by gRPC service matching ValidateUserResponse proto
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("user_id", userId);
            response.put("status", "ACTIVE");
            response.put("success", true);
            response.put("message", "User is valid and active");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // Return error format expected by gRPC service
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("user_id", userId);
            response.put("status", "NOT_FOUND");
            response.put("success", false);
            response.put("message", "User not found or inactive: " + e.getMessage());
            
            return ResponseEntity.ok(response); // Return 200 OK with valid=false instead of error status
        }
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