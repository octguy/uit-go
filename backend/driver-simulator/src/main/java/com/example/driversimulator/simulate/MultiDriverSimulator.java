package com.example.driversimulator.simulate;

import com.example.driversimulator.client.DriverClient;
import com.example.driversimulator.dto.DriverResponse;
import com.example.driversimulator.entity.Point;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    public void simulateAllDrivers(List<Point> basePath, long delayMillis) {

        // 1) Lấy danh sách driver ID
        List<String> driverIds = driverClient.getAllDrivers()
                .stream()
                .map(DriverResponse::getId)
                .map(UUID::toString)
                .toList();

        System.out.println("Fetched driver IDs: " + driverIds);

        // 2) Mỗi driver spawn thread + path riêng biệt
        for (String driverId : driverIds) {

            new Thread(() -> {
                System.out.println("Starting simulation for driver " + driverId);

                // Path riêng → offset từ path gốc
                List<Point> driverPath = generatePathWithOffset(basePath);

                driverRunner.simulate(driverId, driverPath, delayMillis);

            }).start();
        }
    }

    /** Tạo path lệch nhẹ 50–150m cho mỗi driver */
    private List<Point> generatePathWithOffset(List<Point> basePath) {
        // offset khoảng 1–3 km
        double maxKm = 3.0;
        double minKm = 1.0;

        // km → độ
        double kmToDegree = 1.0 / 111.0;

        double distanceKm = minKm + Math.random() * (maxKm - minKm);
        double offset = distanceKm * kmToDegree; // độ lệch

        // random hướng
        double angle = Math.random() * 2 * Math.PI;

        double offsetLat = offset * Math.cos(angle);
        double offsetLng = offset * Math.sin(angle);

        return basePath.stream()
                .map(p -> new Point(
                        p.latitude() + offsetLat,
                        p.longitude() + offsetLng
                ))
                .toList();
    }

}
