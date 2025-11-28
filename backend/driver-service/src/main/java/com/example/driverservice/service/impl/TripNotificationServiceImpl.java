package com.example.driverservice.service.impl;

import com.example.driverservice.client.TripClient;
import com.example.driverservice.dto.TripNotificationRequest;
import com.example.driverservice.dto.TripNotificationResponse;
import com.example.driverservice.entity.PendingTripNotification;
import com.example.driverservice.service.ITripNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TripNotificationServiceImpl implements ITripNotificationService {

    private static final String PENDING_TRIPS_KEY = "pending_trips:";
    private static final int NOTIFICATION_TIMEOUT_SECONDS = 15;

    private final RedisTemplate<String, Object> redisTemplate;
    private final TripClient tripClient;

    public TripNotificationServiceImpl(@Qualifier("redisObjectTemplate") RedisTemplate<String, Object> redisTemplate, 
                                      TripClient tripClient) {
        this.redisTemplate = redisTemplate;
        this.tripClient = tripClient;
    }

    @Override
    public void handleTripNotification(TripNotificationRequest notification) {
        log.info("Received trip notification: tripId={}, passengerId={}, fare={}", 
                notification.getTripId(), notification.getPassengerId(), notification.getEstimatedFare());

        PendingTripNotification pending = PendingTripNotification.builder()
                .tripId(notification.getTripId())
                .passengerId(notification.getPassengerId())
                .passengerName(notification.getPassengerName())
                .pickupLatitude(notification.getPickupLatitude())
                .pickupLongitude(notification.getPickupLongitude())
                .destinationLatitude(notification.getDestinationLatitude())
                .destinationLongitude(notification.getDestinationLongitude())
                .estimatedFare(notification.getEstimatedFare())
                .distanceKm(notification.getDistanceKm())
                .notifiedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(NOTIFICATION_TIMEOUT_SECONDS))
                .expired(false)
                .accepted(false)
                .build();

        // Store in Redis with TTL of 15 seconds
        String key = PENDING_TRIPS_KEY + notification.getTripId().toString();
        redisTemplate.opsForValue().set(key, pending, NOTIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        log.info("Stored pending trip notification in Redis: tripId={}, expiresAt={}", 
                notification.getTripId(), pending.getExpiresAt());
    }

    @Override
    public TripNotificationResponse acceptTrip(UUID tripId, UUID driverId) {
        String key = PENDING_TRIPS_KEY + tripId.toString();
        PendingTripNotification pending = (PendingTripNotification) redisTemplate.opsForValue().get(key);

        if (pending == null) {
            log.warn("Trip notification not found or expired: tripId={}", tripId);
            return TripNotificationResponse.builder()
                    .tripId(tripId)
                    .driverId(driverId)
                    .accepted(false)
                    .message("Trip notification not found or has expired")
                    .build();
        }

        if (pending.isExpired() || LocalDateTime.now().isAfter(pending.getExpiresAt())) {
            log.warn("Trip notification has expired: tripId={}", tripId);
            redisTemplate.delete(key);
            return TripNotificationResponse.builder()
                    .tripId(tripId)
                    .driverId(driverId)
                    .accepted(false)
                    .message("Trip notification has expired")
                    .build();
        }

        if (pending.isAccepted()) {
            log.warn("Trip already accepted by another driver: tripId={}, acceptedBy={}", 
                    tripId, pending.getAcceptedByDriverId());
            return TripNotificationResponse.builder()
                    .tripId(tripId)
                    .driverId(driverId)
                    .accepted(false)
                    .message("Trip already accepted by another driver")
                    .build();
        }

        // Mark as accepted
        pending.setAccepted(true);
        pending.setAcceptedByDriverId(driverId);
        redisTemplate.opsForValue().set(key, pending, 60, TimeUnit.SECONDS); // Keep for 1 minute

        log.info("Driver {} accepted trip {}", driverId, tripId);

        // Call trip-service to update trip status
        try {
            tripClient.acceptTrip(tripId);
            log.info("Successfully notified trip-service of acceptance: tripId={}, driverId={}", tripId, driverId);
        } catch (Exception e) {
            log.error("Failed to notify trip-service of acceptance: tripId={}, driverId={}, error={}", 
                    tripId, driverId, e.getMessage(), e);
            // Don't fail the acceptance, trip-service can be updated later
        }

        return TripNotificationResponse.builder()
                .tripId(tripId)
                .driverId(driverId)
                .accepted(true)
                .message("Trip accepted successfully")
                .build();
    }

    @Override
    public TripNotificationResponse declineTrip(UUID tripId, UUID driverId) {
        log.info("Driver {} declined trip {}", driverId, tripId);

        return TripNotificationResponse.builder()
                .tripId(tripId)
                .driverId(driverId)
                .accepted(false)
                .message("Trip declined by driver")
                .build();
    }

    @Override
    public Optional<PendingTripNotification> getPendingNotification(UUID tripId) {
        String key = PENDING_TRIPS_KEY + tripId.toString();
        PendingTripNotification pending = (PendingTripNotification) redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(pending);
    }

    @Override
    public List<PendingTripNotification> getPendingNotificationsForDriver(UUID driverId) {
        // Get all pending trip keys
        Set<String> keys = redisTemplate.keys(PENDING_TRIPS_KEY + "*");
        
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<PendingTripNotification> pendingTrips = new ArrayList<>();
        for (String key : keys) {
            PendingTripNotification pending = (PendingTripNotification) redisTemplate.opsForValue().get(key);
            if (pending != null && !pending.isExpired() && !pending.isAccepted() 
                    && LocalDateTime.now().isBefore(pending.getExpiresAt())) {
                pendingTrips.add(pending);
            }
        }

        return pendingTrips;
    }

    @Override
    @Scheduled(fixedRate = 5000) // Run every 5 seconds
    public void expirePendingNotifications() {
        Set<String> keys = redisTemplate.keys(PENDING_TRIPS_KEY + "*");
        
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            PendingTripNotification pending = (PendingTripNotification) redisTemplate.opsForValue().get(key);
            if (pending != null && LocalDateTime.now().isAfter(pending.getExpiresAt())) {
                pending.setExpired(true);
                redisTemplate.delete(key);
                log.info("Expired trip notification: tripId={}", pending.getTripId());
            }
        }
    }
}
