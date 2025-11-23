package com.example.user_service.controller;

import com.example.user_service.client.DriverClient;
import com.example.user_service.dto.request.RegisterDriverRequest;
import com.example.user_service.dto.response.DriverResponse;
import com.example.user_service.dto.response.UserResponse;
import com.example.user_service.service.IDriverService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final IDriverService driverService;

    private final DriverClient driverClient;

    public DriverController(IDriverService driverService, DriverClient driverClient) {
        this.driverClient = driverClient;
        this.driverService = driverService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerDriver(@RequestBody RegisterDriverRequest request) {
        try {
            DriverResponse driver = driverService.createDriver(request);
            driverClient.createDriver(driver.getId());
            System.out.println(driver.getId());
            return ResponseEntity.ok(driver);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid registration data: " + e.getMessage(),
                    "error", "INVALID_REQUEST"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error creating user account: " + e.getMessage(),
                    "error", "INTERNAL_ERROR"
            ));
        }
    }
}
