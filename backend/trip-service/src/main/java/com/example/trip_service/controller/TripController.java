package com.example.trip_service.controller;

import com.example.trip_service.dto.*;
import com.example.trip_service.service.TripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    @Autowired
    private TripService tripService;

    @PostMapping("/request")
    public ResponseEntity<TripResponse> requestTrip(@RequestBody CreateTripRequest request) {
        TripResponse trip = tripService.createTrip(request);
        return ResponseEntity.ok(trip);
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<TripResponse> getTrip(@PathVariable Long tripId) {
        TripResponse trip = tripService.getTripById(tripId);
        return ResponseEntity.ok(trip);
    }

    @PutMapping("/{tripId}/status")
    public ResponseEntity<TripResponse> updateTripStatus(@PathVariable Long tripId, @RequestBody UpdateTripStatusRequest request) {
        TripResponse trip = tripService.updateTripStatus(tripId, request);
        return ResponseEntity.ok(trip);
    }

    @PutMapping("/{tripId}/assign-driver")
    public ResponseEntity<TripResponse> assignDriver(@PathVariable Long tripId, @RequestBody AssignDriverRequest request) {
        TripResponse trip = tripService.assignDriver(tripId, request);
        return ResponseEntity.ok(trip);
    }

    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<List<TripResponse>> getTripsByPassenger(@PathVariable Long passengerId) {
        List<TripResponse> trips = tripService.getTripsByPassenger(passengerId);
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<TripResponse>> getTripsByDriver(@PathVariable Long driverId) {
        List<TripResponse> trips = tripService.getTripsByDriver(driverId);
        return ResponseEntity.ok(trips);
    }
}