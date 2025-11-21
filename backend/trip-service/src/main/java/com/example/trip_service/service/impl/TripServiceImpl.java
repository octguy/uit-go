package com.example.trip_service.service.impl;


import com.example.trip_service.client.UserClient;
import com.example.trip_service.dto.request.EstimateFareRequest;
import com.example.trip_service.dto.response.EstimateFareResponse;
import com.example.trip_service.dto.response.UserValidationResponse;
import com.example.trip_service.service.ITripService;
import com.example.trip_service.utility.PricingUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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

    @Override
    public EstimateFareResponse estimateFare(EstimateFareRequest request) {
        Double distance = PricingUtils.calculateDistanceInKm(request);

        BigDecimal fare = PricingUtils.calculateFareCents(request);

        return EstimateFareResponse.builder()
                .distance(distance)
                .fare(fare)
                .build();
    }
}
