package com.example.driver_service.repository;

import com.example.driver_service.enums.DriverStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis Driver Repository with Master-Replica Support
 * - WRITE operations use master template
 * - READ operations use replica template
 */
@Repository
public class RedisDriverRepository {

    private final RedisTemplate<String, String> masterTemplate;  // For writes
    private final RedisTemplate<String, String> replicaTemplate; // For reads

    private static final String GEO_KEY = "drivers:locations";
    private static final String DRIVER_HASH_PREFIX = "driver:";
    private static final String DRIVER_STATUS_SUFFIX = ":status";

    public RedisDriverRepository(
            @Qualifier("redisMasterTemplate") RedisTemplate<String, String> masterTemplate,
            @Qualifier("redisReplicaTemplate") RedisTemplate<String, String> replicaTemplate
    ) {
        this.masterTemplate = masterTemplate;
        this.replicaTemplate = replicaTemplate;
    }

    /**
     * WRITE operation - uses MASTER
     */
    public void updateLocation(String driverId, double lat, double lng) {
        masterTemplate.opsForGeo().add(
                GEO_KEY,
                new RedisGeoCommands.GeoLocation<>(driverId, new org.springframework.data.geo.Point(lng, lat))
        );

        String driverKey = DRIVER_HASH_PREFIX + driverId;
        masterTemplate.opsForHash().put(driverKey, "lat", String.valueOf(lat));
        masterTemplate.opsForHash().put(driverKey, "lng", String.valueOf(lng));
        masterTemplate.opsForHash().put(driverKey, "updatedAt", String.valueOf(System.currentTimeMillis()));
    }

    /**
     * WRITE operation - uses MASTER
     */
    public void setStatus(String driverId, DriverStatus status) {
        masterTemplate.opsForValue()
                .set(DRIVER_HASH_PREFIX + driverId + DRIVER_STATUS_SUFFIX, status.name());
    }

    /**
     * READ operation - uses REPLICA
     */
    public String getStatus(String driverId) {
        return replicaTemplate.opsForValue()
                .get(DRIVER_HASH_PREFIX + driverId + DRIVER_STATUS_SUFFIX);
    }

    /**
     * READ operation - uses REPLICA
     * This is the main read-heavy operation that benefits from replicas
     */
    public GeoResults<RedisGeoCommands.GeoLocation<String>> findNearbyDrivers(
            double lat,
            double lng,
            double radiusKm,
            int limit
    ) {

        // Tạo vòng tròn tìm kiếm
        Circle within = new Circle(
                new org.springframework.data.geo.Point(lng, lat),
                new Distance(radiusKm, Metrics.KILOMETERS)
        );

        RedisGeoCommands.GeoRadiusCommandArgs args =
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                        .includeCoordinates()
                        .includeDistance()
                        .sortAscending()
                        .limit(limit * 3L);  // Lấy nhiều hơn để lọc

        // GEORADIUS from REPLICA
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = replicaTemplate.opsForGeo()
                .radius(GEO_KEY, within, args);

        // Lọc status (also from REPLICA)
        assert results != null;

        return new GeoResults<>(
                results.getContent().stream()
                        .filter(r -> {
                            String driverId = r.getContent().getName();
                            String status = getStatus(driverId);  // Uses replica

                            // Nếu status null => coi như offline
                            return status != null && status.equals(DriverStatus.ONLINE.name());
                        })
                        .limit(limit)  // Giữ đúng số lượng limit yêu cầu
                        .toList()
        );
    }

}
