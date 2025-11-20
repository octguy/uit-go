package com.example.user_service.controller;

import com.example.user_service.dto.*;
import com.example.user_service.service.IUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
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
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {
        try {
            AuthResponse user = userService.login(request);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid registration data: " + e.getMessage(),
                    "error", "INVALID_REQUEST"
            ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getUser() {
        UserResponse user = userService.getCurrentUser();
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String userId) {
        UserResponse user = userService.getUserById(UUID.fromString(userId));
        return ResponseEntity.ok(user);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    // New: call trip-service via gRPC to list trips for a user
//    @GetMapping("/{userId}/trips")
//    public ResponseEntity<?> getTripsForUser(@PathVariable String userId) {
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
//        return null;
//    }
}