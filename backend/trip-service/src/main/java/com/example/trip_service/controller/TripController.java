package com.example.trip_service.controller;

import com.example.trip_service.dto.*;
import com.example.trip_service.service.ITripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    @Autowired
    private ITripService tripService;

    @PostMapping("/request")
    public ResponseEntity<TripResponse> requestTrip(@RequestBody CreateTripRequest request) {
        TripResponse trip = tripService.createTrip(request);
        return ResponseEntity.ok(trip);
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<TripResponse> getTrip(@PathVariable UUID tripId) {
        TripResponse trip = tripService.getTripById(tripId);
        return ResponseEntity.ok(trip);
    }

    @PutMapping("/{tripId}/status")
    public ResponseEntity<TripResponse> updateTripStatus(@PathVariable UUID tripId, @RequestBody UpdateTripStatusRequest request) {
        TripResponse trip = tripService.updateTripStatus(tripId, request);
        return ResponseEntity.ok(trip);
    }

    @PutMapping("/{tripId}/assign-driver")
    public ResponseEntity<TripResponse> assignDriver(@PathVariable UUID tripId, @RequestBody AssignDriverRequest request) {
        TripResponse trip = tripService.assignDriver(tripId, request);
        return ResponseEntity.ok(trip);
    }

    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<List<TripResponse>> getTripsByPassenger(@PathVariable UUID passengerId) {
        List<TripResponse> trips = tripService.getTripsByPassenger(passengerId);
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<TripResponse>> getTripsByDriver(@PathVariable UUID driverId) {
        List<TripResponse> trips = tripService.getTripsByDriver(driverId);
        return ResponseEntity.ok(trips);
    }
}