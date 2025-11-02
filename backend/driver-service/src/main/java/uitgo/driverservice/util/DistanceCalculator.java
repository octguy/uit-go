package uitgo.driverservice.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;
    
    private final CoordinateValidator coordinateValidator;

    @Autowired
    public DistanceCalculator(CoordinateValidator coordinateValidator) {
        this.coordinateValidator = coordinateValidator;
    }

    /**
     * Calculate distance between two points using Haversine formula
     * Returns distance in kilometers
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Use centralized coordinate validation
        coordinateValidator.validateCoordinates(lat1, lon1);
        coordinateValidator.validateCoordinates(lat2, lon2);
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Check if a point is within a radius
     */
    public boolean isWithinRadius(double centerLat, double centerLon,
                                  double pointLat, double pointLon,
                                  double radiusKm) {
        if (radiusKm <= 0) {
            throw new IllegalArgumentException("Radius must be positive");
        }
        
        double distance = calculateDistance(centerLat, centerLon, pointLat, pointLon);
        return distance <= radiusKm;
    }

    /**
     * Get bounding box coordinates for a radius search
     * Returns [minLat, maxLat, minLon, maxLon]
     */
    public double[] getBoundingBox(double latitude, double longitude, double radiusKm) {
        coordinateValidator.validateCoordinates(latitude, longitude);
        if (radiusKm <= 0) {
            throw new IllegalArgumentException("Radius must be positive");
        }
        
        double latChange = radiusKm / 111.0;
        double lonChange = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude)));

        return new double[]{
                latitude - latChange,   // minLat
                latitude + latChange,   // maxLat
                longitude - lonChange,  // minLon
                longitude + lonChange   // maxLon
        };
    }

    /**
     * Calculate the appropriate geohash precision for a given radius
     * Smaller radius requires higher precision (longer geohash)
     */
    public int getGeohashPrecisionForRadius(double radiusKm) {
        if (radiusKm <= 0.61) return 7;  // ~0.61km
        if (radiusKm <= 2.4) return 6;   // ~2.4km  
        if (radiusKm <= 20) return 5;    // ~20km
        if (radiusKm <= 78) return 4;    // ~78km
        if (radiusKm <= 630) return 3;   // ~630km
        return 2;                        // ~2500km
    }

    /**
     * Calculate distance and return result with the drivers sorted by distance
     * Optimized for bulk distance calculations
     */
    public double[] calculateDistances(double centerLat, double centerLon, 
                                     double[] latitudes, double[] longitudes) {
        if (latitudes.length != longitudes.length) {
            throw new IllegalArgumentException("Latitude and longitude arrays must have same length");
        }
        
        double[] distances = new double[latitudes.length];
        for (int i = 0; i < latitudes.length; i++) {
            distances[i] = calculateDistance(centerLat, centerLon, latitudes[i], longitudes[i]);
        }
        return distances;
    }
}
