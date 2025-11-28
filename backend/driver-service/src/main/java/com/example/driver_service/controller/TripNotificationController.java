package com.example.driver_service.controller;

import com.example.driver_service.dto.TripNotificationResponse;
import com.example.driver_service.entity.PendingTripNotification;
import com.example.driver_service.service.ITripNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/drivers/trips")
@Slf4j
public class TripNotificationController {

    private final ITripNotificationService tripNotificationService;

    public TripNotificationController(ITripNotificationService tripNotificationService) {
        this.tripNotificationService = tripNotificationService;
    }

    @PostMapping("/{tripId}/accept")
    public ResponseEntity<TripNotificationResponse> acceptTrip(
            @PathVariable UUID tripId,
            @RequestParam UUID driverId) {
        
        log.info("Driver {} attempting to accept trip {}", driverId, tripId);
        
        TripNotificationResponse response = tripNotificationService.acceptTrip(tripId, driverId);
        
        if (response.isAccepted()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{tripId}/decline")
    public ResponseEntity<TripNotificationResponse> declineTrip(
            @PathVariable UUID tripId,
            @RequestParam UUID driverId) {
        
        log.info("Driver {} declining trip {}", driverId, tripId);
        
        TripNotificationResponse response = tripNotificationService.declineTrip(tripId, driverId);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PendingTripNotification>> getPendingTrips(
            @RequestParam UUID driverId) {
        
        log.info("Fetching pending trips for driver {}", driverId);
        
        List<PendingTripNotification> pendingTrips = 
                tripNotificationService.getPendingNotificationsForDriver(driverId);
        
        return ResponseEntity.ok(pendingTrips);
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<PendingTripNotification> getTripNotification(
            @PathVariable UUID tripId) {
        
        return tripNotificationService.getPendingNotification(tripId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
