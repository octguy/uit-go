package com.example.driver_service.service;

import com.example.driver_service.dto.TripNotificationRequest;
import com.example.driver_service.dto.TripNotificationResponse;
import com.example.driver_service.entity.PendingTripNotification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ITripNotificationService {

    void handleTripNotification(TripNotificationRequest notification);

    TripNotificationResponse acceptTrip(UUID tripId, UUID driverId);

    TripNotificationResponse declineTrip(UUID tripId, UUID driverId);

    Optional<PendingTripNotification> getPendingNotification(UUID tripId);

    List<PendingTripNotification> getPendingNotificationsForDriver(UUID driverId);

    void expirePendingNotifications();
}
