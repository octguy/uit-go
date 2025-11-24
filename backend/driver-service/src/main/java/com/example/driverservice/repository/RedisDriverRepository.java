package com.example.driverservice.repository;

import com.example.driverservice.enums.DriverStatus;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisDriverRepository {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String GEO_KEY = "drivers:locations";
    private static final String DRIVER_HASH_PREFIX = "driver:";
    private static final String DRIVER_STATUS_SUFFIX = ":status";

    public RedisDriverRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateLocation(String driverId, double lat, double lng) {
        redisTemplate.opsForGeo().add(
                GEO_KEY,
                new RedisGeoCommands.GeoLocation<>(driverId, new org.springframework.data.geo.Point(lng, lat))
        );

        String driverKey = DRIVER_HASH_PREFIX + driverId;
        redisTemplate.opsForHash().put(driverKey, "lat", String.valueOf(lat));
        redisTemplate.opsForHash().put(driverKey, "lng", String.valueOf(lng));
        redisTemplate.opsForHash().put(driverKey, "updatedAt", String.valueOf(System.currentTimeMillis()));
    }

    public void setStatus(String driverId, DriverStatus status) {
        redisTemplate.opsForValue()
                .set(DRIVER_HASH_PREFIX + driverId + DRIVER_STATUS_SUFFIX, status.name());
    }

    public String getStatus(String driverId) {
        return redisTemplate.opsForValue()
                .get(DRIVER_HASH_PREFIX + driverId + DRIVER_STATUS_SUFFIX);
    }

    public GeoResults<RedisGeoCommands.GeoLocation<String>> findNearbyDrivers(
            double lat, double lng, double radiusKm) {

        Circle circle = new Circle(
                new org.springframework.data.geo.Point(lng, lat),
                new Distance(radiusKm, Metrics.KILOMETERS)
        );

        return redisTemplate.opsForGeo().radius(GEO_KEY, circle);
    }
}
