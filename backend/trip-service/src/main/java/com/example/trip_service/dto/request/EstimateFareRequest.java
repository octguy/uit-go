package com.example.trip_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EstimateFareRequest {

    @NotNull(message = "Pickup latitude is required")
    Double pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    Double pickupLongitude;

    @NotNull(message = "Destination latitude is required")
    Double destinationLatitude;

    @NotNull(message = "Destination longitude is required")
    Double destinationLongitude;
}
