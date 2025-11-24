package com.example.driversimulator.simulate;

import com.example.driversimulator.client.DriverClient;
import com.example.driversimulator.dto.DriverResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TestConnection implements CommandLineRunner {

    private final DriverClient driverClient;

    public TestConnection(DriverClient driverClient) {
        this.driverClient = driverClient;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            List<DriverResponse> drivers = driverClient.getAllDrivers();
            System.out.println("Retrieved " + drivers.size() + " drivers from User Service.");
            System.out.println("Successfully connected to User Service.");
        } catch (Exception e) {
            System.err.println("Failed to connect to User Service: " + e.getMessage());
        }
    }
}
