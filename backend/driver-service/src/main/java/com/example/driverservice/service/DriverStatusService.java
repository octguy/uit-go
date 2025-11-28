package com.example.driverservice.service;

import com.example.driverservice.aop.RequireDriver;
import com.example.driverservice.enums.DriverStatus;
import com.example.driverservice.repository.RedisDriverRepository;
import com.example.driverservice.utils.SecurityUtil;
import org.springframework.stereotype.Service;

@Service
public class DriverStatusService {

    private final RedisDriverRepository redisDriverRepository;

    public DriverStatusService(RedisDriverRepository redisDriverRepository) {
        this.redisDriverRepository = redisDriverRepository;
    }

    @RequireDriver
    public void setOnline() {
        String driverId = SecurityUtil.getCurrentUserId().toString();
        redisDriverRepository.setStatus(driverId, DriverStatus.ONLINE);
    }

    @RequireDriver
    public void setOffline() {
        String driverId = SecurityUtil.getCurrentUserId().toString();
        redisDriverRepository.setStatus(driverId, DriverStatus.OFFLINE);
    }

    @RequireDriver
    public void setBusy(String driverId) {
        redisDriverRepository.setStatus(driverId, DriverStatus.BUSY);
    }

    public String getStatus(String driverId) {
        return redisDriverRepository.getStatus(driverId);
    }
}
