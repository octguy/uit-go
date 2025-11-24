package com.example.user_service.service;

import com.example.user_service.dto.request.RegisterDriverRequest;
import com.example.user_service.dto.response.DriverResponse;

import java.util.List;

public interface IDriverService {

    DriverResponse createDriver(RegisterDriverRequest request);

    List<DriverResponse> getAllDrivers();
}
