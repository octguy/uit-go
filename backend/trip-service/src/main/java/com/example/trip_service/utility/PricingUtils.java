package com.example.trip_service.utility;

import java.math.BigDecimal;

public final class PricingUtils {

    // Basic pricing constants (in cents)
    private static final int BASE_FARE_CENTS = 200;   // base fare = $2.00
    private static final int PER_KM_CENTS = 150;      // $1.50 per km

    private PricingUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Calculate great-circle distance (in kilometers) between two coordinates using the Haversine formula.
     *
     * @param lat1 latitude of the origin point
     * @param lon1 longitude of the origin point
     * @param lat2 latitude of the destination point
     * @param lon2 longitude of the destination point
     * @return distance in kilometers
     */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth's radius in km

        double latRad1 = Math.toRadians(lat1);
        double latRad2 = Math.toRadians(lat2);
        double dLat = latRad2 - latRad1;
        double dLon = Math.toRadians(lon2 - lon1);

        double sinDlat = Math.sin(dLat / 2);
        double sinDlon = Math.sin(dLon / 2);
        double hav = sinDlat * sinDlat + Math.cos(latRad1) * Math.cos(latRad2) * sinDlon * sinDlon;
        double c = 2 * Math.atan2(Math.sqrt(hav), Math.sqrt(1 - hav));

        return R * c;
    }

    /**
     * Calculate fare in cents between two coordinates.
     * Fare = base fare + per-km * distance. Rounded to nearest cent.
     *
     * @param lat1 latitude of the origin
     * @param lon1 longitude of the origin
     * @param lat2 latitude of the destination
     * @param lon2 longitude of the destination
     * @return fare in cents as BigDecimal
     */
    public static BigDecimal calculateFareCents(double lat1, double lon1, double lat2, double lon2) {
        double km = distanceKm(lat1, lon1, lat2, lon2);
        double fare = BASE_FARE_CENTS + (PER_KM_CENTS * km);
        return BigDecimal.valueOf(Math.round(fare));
    }
}
