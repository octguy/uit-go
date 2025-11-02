package uitgo.driverservice.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GeohashUtil {

    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
    private static final int[] BITS = {16, 8, 4, 2, 1};
    private static final int PRECISION = 8; // ~40 meter precision

    private final CoordinateValidator coordinateValidator;

    @Autowired
    public GeohashUtil(CoordinateValidator coordinateValidator) {
        this.coordinateValidator = coordinateValidator;
    }

    /**
     * Generate geohash for given latitude and longitude
     * @param latitude Must be between -90 and 90
     * @param longitude Must be between -180 and 180
     * @return Geohash string
     * @throws IllegalArgumentException if coordinates are invalid
     */
    public String encode(double latitude, double longitude) {
        // Use centralized coordinate validation
        try {
            coordinateValidator.validateCoordinates(latitude, longitude);
        } catch (Exception e) {
            // Convert to IllegalArgumentException for backward compatibility
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        boolean isEven = true;
        int bit = 0;
        int ch = 0;
        StringBuilder geohash = new StringBuilder();

        double latMin = -90.0, latMax = 90.0;
        double lonMin = -180.0, lonMax = 180.0;

        while (geohash.length() < PRECISION) {
            if (isEven) {
                double mid = (lonMin + lonMax) / 2;
                if (longitude >= mid) {
                    ch |= BITS[bit];
                    lonMin = mid;
                } else {
                    lonMax = mid;
                }
            } else {
                double mid = (latMin + latMax) / 2;
                if (latitude >= mid) {
                    ch |= BITS[bit];
                    latMin = mid;
                } else {
                    latMax = mid;
                }
            }

            isEven = !isEven;
            if (bit < 4) {
                bit++;
            } else {
                geohash.append(BASE32.charAt(ch));
                bit = 0;
                ch = 0;
            }
        }

        return geohash.toString();
    }

    /**
     * Decode geohash to latitude and longitude
     * @param geohash The geohash string to decode
     * @return Array containing [latitude, longitude]
     * @throws IllegalArgumentException if geohash is invalid
     */
    public double[] decode(String geohash) {
        if (geohash == null || geohash.trim().isEmpty()) {
            throw new IllegalArgumentException("Geohash cannot be null or empty");
        }
        
        boolean isEven = true;
        double latMin = -90.0, latMax = 90.0;
        double lonMin = -180.0, lonMax = 180.0;

        for (char c : geohash.toCharArray()) {
            int idx = BASE32.indexOf(c);
            if (idx == -1) {
                throw new IllegalArgumentException("Invalid character in geohash: " + c);
            }
            
            for (int BITS_VALUE : BITS) {
                if (isEven) {
                    double mid = (lonMin + lonMax) / 2;
                    if ((idx & BITS_VALUE) != 0) {
                        lonMin = mid;
                    } else {
                        lonMax = mid;
                    }
                } else {
                    double mid = (latMin + latMax) / 2;
                    if ((idx & BITS_VALUE) != 0) {
                        latMin = mid;
                    } else {
                        latMax = mid;
                    }
                }
                isEven = !isEven;
            }
        }

        double latitude = (latMin + latMax) / 2;
        double longitude = (lonMin + lonMax) / 2;
        return new double[]{latitude, longitude};
    }
    
    /**
     * Get the bounding box for a geohash
     * @param geohash The geohash string
     * @return Array containing [minLat, maxLat, minLon, maxLon]
     */
    public double[] getBounds(String geohash) {
        if (geohash == null || geohash.trim().isEmpty()) {
            throw new IllegalArgumentException("Geohash cannot be null or empty");
        }
        
        boolean isEven = true;
        double latMin = -90.0, latMax = 90.0;
        double lonMin = -180.0, lonMax = 180.0;

        for (char c : geohash.toCharArray()) {
            int idx = BASE32.indexOf(c);
            if (idx == -1) {
                throw new IllegalArgumentException("Invalid character in geohash: " + c);
            }
            
            for (int BITS_VALUE : BITS) {
                if (isEven) {
                    double mid = (lonMin + lonMax) / 2;
                    if ((idx & BITS_VALUE) != 0) {
                        lonMin = mid;
                    } else {
                        lonMax = mid;
                    }
                } else {
                    double mid = (latMin + latMax) / 2;
                    if ((idx & BITS_VALUE) != 0) {
                        latMin = mid;
                    } else {
                        latMax = mid;
                    }
                }
                isEven = !isEven;
            }
        }
        
        return new double[]{latMin, latMax, lonMin, lonMax};
    }
}
