package com.example.user_service.controller;

import com.example.user_service.dto.response.UserResponse;
import com.example.user_service.dto.response.UserValidationResponse;
import com.example.user_service.jwt.JwtUtil;
import com.example.user_service.service.IUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("api/internal/auth")
public class InternalController {

    private final JwtUtil jwtUtil;

    private final IUserService userService;

    public InternalController(JwtUtil jwtUtil, IUserService userService) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/validate")
    public ResponseEntity<UserValidationResponse> validate(@RequestHeader("Authorization") String token) {
        try {
            System.out.println("In validate of InternalController (User-service): " + token);
            UUID userId = UUID.fromString(jwtUtil.extractUserId(token));
            UserResponse user = userService.getUserById(userId);
            return ResponseEntity.ok(new UserValidationResponse(userId, user.getRole(), true));
        }
        catch (Exception e) {
            System.out.println("Failed in validate of InternalController (User-service): " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserValidationResponse(null, null, false));
        }
    }

}
