package com.example.driver_service.service;

import com.example.driver_service.aop.RequireDriver;
import com.example.driver_service.client.UserClient;
import com.example.driver_service.dto.DriverResponse;
import com.example.driver_service.enums.DriverStatus;
import com.example.driver_service.repository.RedisDriverRepository;
import com.example.driver_service.utils.SecurityUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverStatusService {

    private final RedisDriverRepository redisDriverRepository;

    private final UserClient userClient;

    public DriverStatusService(RedisDriverRepository redisDriverRepository, UserClient userClient) {
        this.userClient = userClient;
        this.redisDriverRepository = redisDriverRepository;
    }

    public void setAllDriversOnline() {
        List<DriverResponse> drivers = userClient.getAllDrivers();
        for (DriverResponse driver : drivers) {
            redisDriverRepository.setStatus(driver.getId().toString(), DriverStatus.ONLINE);
        }
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
