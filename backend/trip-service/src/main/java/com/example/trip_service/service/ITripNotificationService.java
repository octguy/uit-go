package com.example.trip_service.service;

import com.example.trip_service.dto.request.TripNotificationRequest;

public interface ITripNotificationService {

    void notifyNearbyDrivers(TripNotificationRequest notification);
}
