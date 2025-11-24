package com.example.driversimulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DriverSimulatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(DriverSimulatorApplication.class, args);
        System.out.println("Driver Simulator Service is running...");
	}
}
