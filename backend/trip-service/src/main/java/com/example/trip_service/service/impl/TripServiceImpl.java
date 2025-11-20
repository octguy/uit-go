package com.example.trip_service.service.impl;


import com.example.trip_service.client.UserClient;
import com.example.trip_service.dto.UserValidationResponse;
import com.example.trip_service.service.ITripService;
import org.springframework.stereotype.Service;

@Service
public class TripServiceImpl implements ITripService {

    private final UserClient userClient;

    public TripServiceImpl(UserClient userClient) {
        this.userClient = userClient;
    }

    @Override
    public UserValidationResponse validateToken(String token) {
        UserValidationResponse user = userClient.validate(token);

        System.out.println("In validateToken of TripServiceImpl: " + token);

        if (!user.isValid()) {
            throw new RuntimeException("❌ Token validation failed - invalid token");
        }

        return user;
    }
}
