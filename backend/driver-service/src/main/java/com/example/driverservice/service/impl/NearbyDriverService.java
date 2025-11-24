package com.example.driverservice.service.impl;

import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class NearbyDriverService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String DRIVER_LOCATION_KEY = "driver:location";

    public NearbyDriverService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public List<String> findNearbyDrivers(double latitude, double longitude, double radiusInKm, int limit) {
        Circle circle = new Circle(new Point(latitude, longitude),
                new Distance(radiusInKm, Metrics.KILOMETERS));

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(DRIVER_LOCATION_KEY, circle);

        if (results == null) return List.of();

        return results.getContent().stream()
                .sorted(Comparator.comparingDouble(a -> a.getDistance().getValue()))
                .limit(limit)
                .map(r -> r.getContent().getName())
                .toList();
    }
}
