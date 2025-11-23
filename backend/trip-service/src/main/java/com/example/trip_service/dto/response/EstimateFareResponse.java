package com.example.trip_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EstimateFareResponse {

    BigDecimal fare;

    Double distance;
}
