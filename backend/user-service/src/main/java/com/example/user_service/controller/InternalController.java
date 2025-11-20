package com.example.user_service.controller;

import com.example.user_service.dto.UserValidationResponse;
import com.example.user_service.entity.User;
import com.example.user_service.jwt.JwtUtil;
import com.example.user_service.util.SecurityUtil;
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

    public InternalController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/validate")
    public ResponseEntity<UserValidationResponse> validate(@RequestHeader("Authorization") String token) {
        try {
            System.out.println("In validate of InternalController (User-service): " + token);
            UUID userId = UUID.fromString(jwtUtil.extractUserId(token));
            return ResponseEntity.ok(new UserValidationResponse(userId, true));
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserValidationResponse(null, false));
        }
    }

}
