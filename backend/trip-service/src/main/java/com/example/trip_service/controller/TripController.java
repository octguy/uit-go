package com.example.trip_service.controller;

import com.example.trip_service.dto.*;
import com.example.trip_service.service.ITripService;
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


}