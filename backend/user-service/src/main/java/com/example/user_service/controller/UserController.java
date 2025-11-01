package com.example.user_service.controller;

import com.example.user_service.dto.*;
import com.example.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@RequestBody CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long userId, @RequestBody UpdateUserRequest request) {
        UserResponse user = userService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/type/{userType}")
    public ResponseEntity<List<UserResponse>> getUsersByType(@PathVariable String userType) {
        List<UserResponse> users = userService.getUsersByType(userType);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidateUserResponse> validateUser(@RequestBody ValidateUserRequest request) {
        System.out.println("👤 User Service: Received validation request for userId: " + request.getUserId());
        
        try {
            UUID userId = request.getUserIdAsUUID();
            UserResponse user = userService.getUserById(userId);
            
            if (user != null) {
                System.out.println("✅ User found: " + user.getEmail() + " (Type: " + user.getUserType() + ")");
                ValidateUserResponse response = new ValidateUserResponse(true, user.getUserType(), "User is valid");
                return ResponseEntity.ok(response);
            } else {
                System.out.println("❌ User not found");
                ValidateUserResponse response = new ValidateUserResponse(false, null, "User not found");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            System.err.println("❌ Validation error: " + e.getMessage());
            ValidateUserResponse response = new ValidateUserResponse(false, null, "Validation error: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}