package com.example.trip_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class TripServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TripServiceApplication.class, args);
	}
}
