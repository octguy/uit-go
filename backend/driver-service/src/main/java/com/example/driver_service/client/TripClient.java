package com.example.driverservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@FeignClient(
        name = "trip-service",
        url = "http://trip-service:8082"
)
public interface TripClient {

    @PostMapping("/api/trips/{id}/accept")
    Object acceptTrip(@PathVariable("id") UUID tripId);
}
