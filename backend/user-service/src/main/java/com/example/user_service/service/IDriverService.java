package com.example.user_service.service;

import com.example.user_service.dto.request.RegisterDriverRequest;
import com.example.user_service.dto.response.DriverResponse;

public interface IDriverService {

    DriverResponse createDriver(RegisterDriverRequest request);
}
