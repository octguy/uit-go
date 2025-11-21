package com.example.trip_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelTripRequest {
    private String reason;
    private String cancelledBy; // "passenger", "driver", "system"
    private String additionalNotes;
}