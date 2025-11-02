package uitgo.driverservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import uitgo.driverservice.dto.DriverLocationDTO;
import uitgo.driverservice.exception.DriverServiceException;
import uitgo.driverservice.util.GeohashUtil;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DriverCacheService {

    private static final String DRIVER_LOCATION_KEY_PREFIX = "driver:location:";
    private static final String DRIVER_STATUS_KEY_PREFIX = "driver:status:";
    private static final String DRIVER_GEOHASH_KEY = "driver:geohash:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final GeohashUtil geohashUtil;

    @Value("${driver.cache.ttl:900}")
    private long cacheTTL;

    @Autowired
    public DriverCacheService(RedisTemplate<String, Object> redisTemplate, GeohashUtil geohashUtil) {
        this.redisTemplate = redisTemplate;
        this.geohashUtil = geohashUtil;
    }

    public void cacheDriverLocation(UUID driverId, Double latitude, Double longitude, String status, long timestamp) {
        if (driverId == null) {
            throw new DriverServiceException("INVALID_INPUT", "Driver ID cannot be null", null);
        }
        if (latitude == null) {
            throw new DriverServiceException("INVALID_INPUT", "Latitude cannot be null", null);
        }
        if (longitude == null) {
            throw new DriverServiceException("INVALID_INPUT", "Longitude cannot be null", null);
        }
        if (status == null) {
            throw new DriverServiceException("INVALID_INPUT", "Status cannot be null", null);
        }
        
        // Calculate geohash and delegate to the optimized method
        String geohash = geohashUtil.encode(latitude, longitude);
        cacheDriverLocationWithGeohash(driverId, latitude, longitude, status, timestamp, geohash);
    }

    /**
     * Cache driver location with pre-calculated geohash to avoid duplicate calculations.
     * This method should be used when geohash is already calculated to improve performance.
     *
     * @param driverId The driver's unique identifier
     * @param latitude The driver's latitude
     * @param longitude The driver's longitude
     * @param status The driver's status
     * @param timestamp The timestamp of the location update
     * @param geohash Pre-calculated geohash to avoid recalculation
     */
    public void cacheDriverLocationWithGeohash(UUID driverId, Double latitude, Double longitude, String status, long timestamp, String geohash) {
        if (driverId == null) {
            throw new DriverServiceException("INVALID_INPUT", "Driver ID cannot be null", null);
        }
        if (latitude == null) {
            throw new DriverServiceException("INVALID_INPUT", "Latitude cannot be null", null);
        }
        if (longitude == null) {
            throw new DriverServiceException("INVALID_INPUT", "Longitude cannot be null", null);
        }
        if (status == null) {
            throw new DriverServiceException("INVALID_INPUT", "Status cannot be null", null);
        }
        if (geohash == null || geohash.trim().isEmpty()) {
            throw new DriverServiceException("INVALID_INPUT", "Geohash cannot be null or empty", null);
        }
        
        try {
            DriverLocationDTO locationDTO = DriverLocationDTO.builder()
                    .driverId(driverId)
                    .latitude(latitude)
                    .longitude(longitude)
                    .status(status)
                    .timestamp(timestamp)
                    .build();

            String locationKey = DRIVER_LOCATION_KEY_PREFIX + driverId;
            redisTemplate.opsForValue().set(locationKey, locationDTO, cacheTTL, TimeUnit.SECONDS);

            String geohashKey = DRIVER_GEOHASH_KEY + driverId;
            redisTemplate.opsForValue().set(geohashKey, geohash, cacheTTL, TimeUnit.SECONDS);

            log.debug("Cached location for driver: {} with pre-calculated geohash", driverId);
        } catch (Exception e) {
            log.error("Error caching driver location: {}", driverId, e);
            throw new DriverServiceException("CACHE_ERROR", "Failed to cache driver location", e.getMessage());
        }
    }

    public DriverLocationDTO getDriverLocation(UUID driverId) {
        if (driverId == null) {
            throw new DriverServiceException("INVALID_INPUT", "Driver ID cannot be null", null);
        }
        
        try {
            String locationKey = DRIVER_LOCATION_KEY_PREFIX + driverId;
            Object cached = redisTemplate.opsForValue().get(locationKey);

            if (cached instanceof DriverLocationDTO) {
                return (DriverLocationDTO) cached;
            }
            return null;
        } catch (Exception e) {
            log.error("Error retrieving driver location from cache: {}", driverId, e);
            return null;
        }
    }

    public void cacheDriverStatus(UUID driverId, String status) {
        if (driverId == null) {
            throw new DriverServiceException("INVALID_INPUT", "Driver ID cannot be null", null);
        }
        if (status == null) {
            throw new DriverServiceException("INVALID_INPUT", "Status cannot be null", null);
        }
        
        try {
            String statusKey = DRIVER_STATUS_KEY_PREFIX + driverId;
            redisTemplate.opsForValue().set(statusKey, status, cacheTTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error caching driver status: {}", driverId, e);
        }
    }

    public String getDriverStatus(UUID driverId) {
        if (driverId == null) {
            throw new DriverServiceException("INVALID_INPUT", "Driver ID cannot be null", null);
        }
        
        try {
            String statusKey = DRIVER_STATUS_KEY_PREFIX + driverId;
            Object cached = redisTemplate.opsForValue().get(statusKey);
            return (String) cached;
        } catch (Exception e) {
            log.error("Error retrieving driver status from cache: {}", driverId, e);
            return null;
        }
    }

    public void invalidateDriverCache(UUID driverId) {
        if (driverId == null) {
            throw new DriverServiceException("INVALID_INPUT", "Driver ID cannot be null", null);
        }
        
        try {
            String locationKey = DRIVER_LOCATION_KEY_PREFIX + driverId;
            String statusKey = DRIVER_STATUS_KEY_PREFIX + driverId;
            String geohashKey = DRIVER_GEOHASH_KEY + driverId;

            redisTemplate.delete(locationKey);
            redisTemplate.delete(statusKey);
            redisTemplate.delete(geohashKey);

            log.debug("Invalidated cache for driver: {}", driverId);
        } catch (Exception e) {
            log.error("Error invalidating driver cache: {}", driverId, e);
        }
    }

    public boolean isCached(UUID driverId) {
        if (driverId == null) {
            throw new DriverServiceException("INVALID_INPUT", "Driver ID cannot be null", null);
        }
        
        try {
            String locationKey = DRIVER_LOCATION_KEY_PREFIX + driverId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(locationKey));
        } catch (Exception e) {
            log.error("Error checking driver cache: {}", driverId, e);
            return false;
        }
    }
}
