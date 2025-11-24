package com.example.driverservice.service;

import com.example.driverservice.repository.RedisDriverRepository;
import org.springframework.stereotype.Service;

@Service
public class DriverLocationService {

    private final RedisDriverRepository redisDriverRepository;

    public DriverLocationService(RedisDriverRepository redisDriverRepository) {
        this.redisDriverRepository = redisDriverRepository;
    }

    public void updateDriverLocation(String driverId, double latitude, double longitude) {
        redisDriverRepository.updateLocation(driverId, latitude, longitude);
    }
}
