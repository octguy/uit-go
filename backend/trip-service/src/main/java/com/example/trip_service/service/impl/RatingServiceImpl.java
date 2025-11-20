package com.example.trip_service.service.impl;

import com.example.trip_service.dto.CreateRatingRequest;
import com.example.trip_service.dto.RatingResponse;
import com.example.trip_service.entity.Rating;
import com.example.trip_service.entity.Trip;
import com.example.trip_service.enums.TripStatus;
import com.example.trip_service.repository.RatingRepository;
import com.example.trip_service.repository.TripRepository;
import com.example.trip_service.service.IRatingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RatingServiceImpl implements IRatingService {

}
