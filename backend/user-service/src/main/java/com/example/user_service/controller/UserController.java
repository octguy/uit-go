package com.example.user_service.controller;

import com.example.user_service.dto.request.CreateUserRequest;
import com.example.user_service.dto.request.LoginRequest;
import com.example.user_service.dto.request.RegisterDriverRequest;
import com.example.user_service.dto.response.AuthResponse;
import com.example.user_service.dto.response.DriverResponse;
import com.example.user_service.dto.response.UserResponse;
import com.example.user_service.service.IDriverService;
import com.example.user_service.service.IUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final IUserService userService;

    private final IDriverService driverService;

    public UserController(IUserService userService, IDriverService driverService) {
        this.driverService = driverService;
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

    @PostMapping("/register-driver")
    public ResponseEntity<?> registerDriver(@RequestBody RegisterDriverRequest request) {
        try {
            DriverResponse driver = driverService.createDriver(request);
            System.out.println(driver.getId());
            return ResponseEntity.ok(driver);
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

    @GetMapping
    public  ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
}