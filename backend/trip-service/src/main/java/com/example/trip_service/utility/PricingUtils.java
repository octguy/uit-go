package com.example.trip_service.utility;

import com.example.trip_service.dto.request.EstimateFareRequest;

import java.math.BigDecimal;

public final class PricingUtils {

    private static final int BASE_FARE_CENTS = 200;   // base fare = $2.00
    private static final int PER_KM_CENTS = 150;      // $1.50 per km

    private PricingUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Double calculateDistanceInKm(EstimateFareRequest request) {
        final double R = 6371.0; // Earth's radius in km

        double latRad1 = Math.toRadians(request.getPickupLatitude());
        double latRad2 = Math.toRadians(request.getDestinationLatitude());

        double dLat = latRad2 - latRad1;
        double dLon = Math.toRadians(request.getDestinationLongitude() - request.getPickupLongitude());

        double sinDlat = Math.sin(dLat / 2);
        double sinDlon = Math.sin(dLon / 2);

        double hav = sinDlat * sinDlat + Math.cos(latRad1) *
                Math.cos(latRad2) * sinDlon * sinDlon;

        double c = 2 * Math.atan2(Math.sqrt(hav), Math.sqrt(1 - hav));
        return R * c;
    }

    public static BigDecimal calculateFareCents(EstimateFareRequest request) {
        double km = calculateDistanceInKm(request);
        double fare = BASE_FARE_CENTS + (PER_KM_CENTS * km);
        return BigDecimal.valueOf(Math.round(fare));
    }
}
