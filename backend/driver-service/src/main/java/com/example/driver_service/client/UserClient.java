package com.example.driver_service.client;

import com.example.driver_service.dto.DriverResponse;
import com.example.driver_service.dto.UserValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(
        name = "user-service",
        url = "http://user-service:8081"
)
public interface UserClient {

    @GetMapping("/api/internal/auth/validate")
    UserValidationResponse validate(@RequestHeader("Authorization") String token);

    @GetMapping("/api/internal/drivers")
    List<DriverResponse> getAllDrivers();
}