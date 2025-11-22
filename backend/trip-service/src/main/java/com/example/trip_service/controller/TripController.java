package com.example.trip_service.controller;

import com.example.trip_service.dto.*;
import com.example.trip_service.service.ITripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}