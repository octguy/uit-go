package com.example.trip_service.service.impl;

import com.example.trip_service.aop.RequireUser;
import com.example.trip_service.dto.request.CreateTripRequest;
import com.example.trip_service.dto.request.EstimateFareRequest;
import com.example.trip_service.dto.response.EstimateFareResponse;
import com.example.trip_service.dto.response.TripResponse;
import com.example.trip_service.service.ITripService;
import com.example.trip_service.utility.PricingUtils;
import com.example.trip_service.utility.SecurityUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import java.util.UUID;

@Service
public class TripServiceImpl implements ITripService {

    @Override
    @RequireUser
    public UUID getUserId() {
        return SecurityUtil.getCurrentUserId();
    }

    @Override
    @RequireUser
    public EstimateFareResponse estimateFare(EstimateFareRequest request) {
        Double distance = PricingUtils.calculateDistanceInKm(request);

        BigDecimal fare = PricingUtils.calculateFareCents(request);

        return EstimateFareResponse.builder()
                .distance(distance)
                .fare(fare)
                .build();
    }

    @Override
    public TripResponse createTrip(CreateTripRequest request) {
        return null;
    }
}
