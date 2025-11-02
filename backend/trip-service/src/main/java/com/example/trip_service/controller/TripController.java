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

    private final ITripService tripService;

    public TripController(ITripService tripService) {
        this.tripService = tripService;
    }

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

//    @PutMapping("/{tripId}/status")
//    public ResponseEntity<TripResponse> updateTripStatus(@PathVariable UUID tripId, @RequestBody UpdateTripStatusRequest request) {
//        TripResponse trip = tripService.updateTripStatus(tripId, request);
//        return ResponseEntity.ok(trip);
//    }

    @PutMapping("/{tripId}/assign-driver")
    public ResponseEntity<TripResponse> assignDriver(@PathVariable UUID tripId, @RequestBody AssignDriverRequest request) {
        TripResponse trip = tripService.assignDriver(tripId, request);
        return ResponseEntity.ok(trip);
    }
}