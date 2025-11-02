package uitgo.driverservice.util;

import org.springframework.stereotype.Component;
import uitgo.driverservice.exception.DriverServiceException;

/**
 * Centralized coordinate validation utility
 * Provides consistent validation logic across all components
 */
@Component
public class CoordinateValidator {

    /**
     * Validate coordinates and throw exception if invalid
     * @param latitude The latitude to validate
     * @param longitude The longitude to validate
     * @throws DriverServiceException if coordinates are invalid
     */
    public void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null) {
            throw new DriverServiceException("INVALID_INPUT", "Latitude cannot be null", null);
        }
        if (longitude == null) {
            throw new DriverServiceException("INVALID_INPUT", "Longitude cannot be null", null);
        }
        
        validateCoordinates(latitude.doubleValue(), longitude.doubleValue());
    }

    /**
     * Validate coordinates and throw exception if invalid
     * @param latitude The latitude to validate (primitive)
     * @param longitude The longitude to validate (primitive)
     * @throws DriverServiceException if coordinates are invalid
     */
    public void validateCoordinates(double latitude, double longitude) {
        // Check for special floating point values
        if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
            throw new DriverServiceException("INVALID_INPUT", "Coordinates cannot be NaN", null);
        }
        
        if (Double.isInfinite(latitude) || Double.isInfinite(longitude)) {
            throw new DriverServiceException("INVALID_INPUT", "Coordinates cannot be infinite", null);
        }
        
        // Check coordinate ranges
        if (latitude < -90.0 || latitude > 90.0) {
            throw new DriverServiceException("INVALID_INPUT", 
                String.format("Latitude must be between -90 and 90, got: %.6f", latitude), null);
        }
        
        if (longitude < -180.0 || longitude > 180.0) {
            throw new DriverServiceException("INVALID_INPUT", 
                String.format("Longitude must be between -180 and 180, got: %.6f", longitude), null);
        }
    }

    /**
     * Check if coordinates are valid without throwing exception
     * @param latitude The latitude to check
     * @param longitude The longitude to check
     * @return true if coordinates are valid, false otherwise
     */
    public boolean isValidCoordinate(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        return isValidCoordinate(latitude.doubleValue(), longitude.doubleValue());
    }

    /**
     * Check if coordinates are valid without throwing exception
     * @param latitude The latitude to check (primitive)
     * @param longitude The longitude to check (primitive)
     * @return true if coordinates are valid, false otherwise
     */
    public boolean isValidCoordinate(double latitude, double longitude) {
        // Check for special floating point values
        if (Double.isNaN(latitude) || Double.isNaN(longitude) ||
            Double.isInfinite(latitude) || Double.isInfinite(longitude)) {
            return false;
        }
        
        // Check coordinate ranges
        return latitude >= -90.0 && latitude <= 90.0 && 
               longitude >= -180.0 && longitude <= 180.0;
    }

    /**
     * Validate single latitude value
     * @param latitude The latitude to validate
     * @throws DriverServiceException if latitude is invalid
     */
    public void validateLatitude(Double latitude) {
        if (latitude == null) {
            throw new DriverServiceException("INVALID_INPUT", "Latitude cannot be null", null);
        }
        validateLatitude(latitude.doubleValue());
    }

    /**
     * Validate single latitude value
     * @param latitude The latitude to validate (primitive)
     * @throws DriverServiceException if latitude is invalid
     */
    public void validateLatitude(double latitude) {
        if (Double.isNaN(latitude) || Double.isInfinite(latitude)) {
            throw new DriverServiceException("INVALID_INPUT", "Latitude cannot be NaN or infinite", null);
        }
        
        if (latitude < -90.0 || latitude > 90.0) {
            throw new DriverServiceException("INVALID_INPUT", 
                String.format("Latitude must be between -90 and 90, got: %.6f", latitude), null);
        }
    }

    /**
     * Validate single longitude value
     * @param longitude The longitude to validate
     * @throws DriverServiceException if longitude is invalid
     */
    public void validateLongitude(Double longitude) {
        if (longitude == null) {
            throw new DriverServiceException("INVALID_INPUT", "Longitude cannot be null", null);
        }
        validateLongitude(longitude.doubleValue());
    }

    /**
     * Validate single longitude value
     * @param longitude The longitude to validate (primitive)
     * @throws DriverServiceException if longitude is invalid
     */
    public void validateLongitude(double longitude) {
        if (Double.isNaN(longitude) || Double.isInfinite(longitude)) {
            throw new DriverServiceException("INVALID_INPUT", "Longitude cannot be NaN or infinite", null);
        }
        
        if (longitude < -180.0 || longitude > 180.0) {
            throw new DriverServiceException("INVALID_INPUT", 
                String.format("Longitude must be between -180 and 180, got: %.6f", longitude), null);
        }
    }
}