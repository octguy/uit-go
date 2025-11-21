package com.example.trip_service.controller;

import com.example.trip_service.dto.request.CreateTripRequest;
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
    public ResponseEntity<?> getUserRequest() {
        return ResponseEntity.ok(tripService.getUserId());
    }

    @GetMapping("/get-estimated-fare")
    public ResponseEntity<?> getEstimatedFare(@RequestBody @Valid EstimateFareRequest request) {
        return ResponseEntity.ok(tripService.estimateFare(request));
    }

    @PostMapping("/create")
    public ResponseEntity<?> createTrip(@RequestBody @Valid CreateTripRequest request) {
        return ResponseEntity.ok(tripService.createTrip(request));
    }
}