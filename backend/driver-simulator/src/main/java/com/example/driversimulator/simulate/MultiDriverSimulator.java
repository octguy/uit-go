package com.example.driversimulator.simulate;

import com.example.driversimulator.client.DriverClient;
import com.example.driversimulator.dto.DriverResponse;
import com.example.driversimulator.entity.Point;
import org.springframework.stereotype.Component;

import java.util.List;

import java.util.UUID;

@Component
public class MultiDriverSimulator {

    private final DriverClient driverClient;
    private final DriverRunner driverRunner;

    public MultiDriverSimulator(DriverClient driverClient,
                                DriverRunner driverRunner) {
        this.driverClient = driverClient;
        this.driverRunner = driverRunner;
    }

    public void simulateAllDrivers(List<Point> path, long delayMillis) {

        // 1) Lấy toàn bộ driverId từ user-service
        List<String> driverIds = driverClient.getAllDrivers()
                .stream()
                .map(DriverResponse::getId)   // UUID
                .map(UUID::toString)          // convert to string
                .toList();

        System.out.println("Fetched driver IDs: " + driverIds);

        // 2) Loop từng đại ca tài xế, spawn thread chạy riêng
        for (String driverId : driverIds) {
            new Thread(() -> {
                System.out.println("Starting simulation for driver " + driverId);
                driverRunner.simulate(driverId, path, delayMillis);
            }).start();
        }
    }
}
