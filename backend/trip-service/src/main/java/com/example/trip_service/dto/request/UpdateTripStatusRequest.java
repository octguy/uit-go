package com.example.trip_service.dto.request;

import com.example.trip_service.enums.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTripStatusRequest {

    private TripStatus status;
}