package com.example.trip_service.controller;

import com.example.trip_service.dto.request.EstimateFareRequest;
import com.example.trip_service.dto.response.UserValidationResponse;
import com.example.trip_service.service.ITripService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final ITripService tripService;

    public TripController(ITripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping("/get-user-request")
    public ResponseEntity<?> getUserRequest(@RequestHeader("Authorization") String token) {
        System.out.println("In getUserRequest of TripController: " + token);
        String cleanedToken = token.replaceFirst("(?i)^Bearer\\s+", "");
        UserValidationResponse user = tripService.validateToken(cleanedToken);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/get-estimated-fare")
    public ResponseEntity<?> getEstimatedFare(@RequestHeader("Authorization") String token,
                                              @RequestBody @Valid EstimateFareRequest request) {
        System.out.println("In getEstimatedFare of TripController: " + token);
        String cleanedToken = token.replaceFirst("(?i)^Bearer\\s+", "");

        try {
            tripService.validateToken(cleanedToken);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("❌ Unauthorized when get estimate fare: " + e.getMessage());
        }

        return ResponseEntity.ok(tripService.estimateFare(request));
    }
}