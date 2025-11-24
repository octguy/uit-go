package com.example.driverservice.service;

import com.example.driverservice.enums.DriverStatus;
import com.example.driverservice.repository.RedisDriverRepository;
import org.springframework.stereotype.Service;

@Service
public class DriverStatusService {

    private final RedisDriverRepository redisDriverRepository;

    public DriverStatusService(RedisDriverRepository redisDriverRepository) {
        this.redisDriverRepository = redisDriverRepository;
    }

    public void setOnline(String driverId) {
        redisDriverRepository.setStatus(driverId, DriverStatus.ONLINE);
    }

    public void setOffline(String driverId) {
        redisDriverRepository.setStatus(driverId, DriverStatus.OFFLINE);
    }

    public void setBusy(String driverId) {
        redisDriverRepository.setStatus(driverId, DriverStatus.BUSY);
    }

    public String getStatus(String driverId) {
        return redisDriverRepository.getStatus(driverId);
    }
}
