package com.example.driverservice.service;

import com.example.driverservice.dto.TripNotificationRequest;
import com.example.driverservice.dto.TripNotificationResponse;
import com.example.driverservice.entity.PendingTripNotification;

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
