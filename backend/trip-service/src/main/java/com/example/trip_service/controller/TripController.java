package com.example.trip_service.controller;

import com.example.trip_service.dto.request.CreateTripRequest;
import com.example.trip_service.dto.request.EstimateFareRequest;
import com.example.trip_service.service.ITripService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    @GetMapping("/user")
    public ResponseEntity<?> getUser() {
        return ResponseEntity.ok(tripService.getPassengerId());
    }

    @GetMapping("/driver")
    public ResponseEntity<?> getDriver() {
        return ResponseEntity.ok(tripService.getDriverId());
    }

    @GetMapping("/get-estimated-fare")
    public ResponseEntity<?> getEstimatedFare(@RequestBody @Valid EstimateFareRequest request) {
        return ResponseEntity.ok(tripService.estimateFare(request));
    }

    @PostMapping("/create")
    public ResponseEntity<?> createTrip(@RequestBody @Valid CreateTripRequest request) {
        return ResponseEntity.ok(tripService.createTrip(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTripById(@PathVariable("id") String id) {
        return ResponseEntity.ok(tripService.getTripById(UUID.fromString(id)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelTrip(@PathVariable("id") String id) {
        return ResponseEntity.ok(tripService.cancelTrip(UUID.fromString(id)));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> acceptTrip(@PathVariable("id") String id) {
        return ResponseEntity.ok(tripService.acceptTrip(UUID.fromString(id)));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startTrip(@PathVariable("id") String id) {
        return ResponseEntity.ok(tripService.startTrip(UUID.fromString(id)));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeTrip(@PathVariable("id") String id) {
        return ResponseEntity.ok(tripService.completeTrip(UUID.fromString(id)));
    }
}