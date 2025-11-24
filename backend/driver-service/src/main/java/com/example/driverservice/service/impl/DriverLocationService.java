package com.example.driverservice.service.impl;

import com.example.driverservice.enums.DriverStatus;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DriverLocationService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String DRIVER_LOCATION_KEY = "driver:location";
    private static final String DRIVER_STATUS_KEY_PREFIX = "driver_status:";

    public DriverLocationService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateLocation(UUID driverId, double latitude, double longitude) {
        redisTemplate.opsForGeo()
                .add(DRIVER_LOCATION_KEY, new Point(latitude, longitude), driverId.toString());

        redisTemplate.opsForValue()
                .set(DRIVER_STATUS_KEY_PREFIX + driverId, DriverStatus.ONLINE.toString());
    }

}
