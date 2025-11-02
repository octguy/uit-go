package uitgo.driverservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uitgo.driverservice.entity.DriverLocation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverLocationRepository extends JpaRepository<DriverLocation, UUID> {

    @Query(value = "SELECT * FROM drivers.driver_locations WHERE driver_id = :driverId ORDER BY timestamp DESC LIMIT 1",
            nativeQuery = true)
    Optional<DriverLocation> findLatestLocationByDriverId(@Param("driverId") UUID driverId);

    @Query(value = "SELECT * FROM drivers.driver_locations WHERE driver_id = :driverId AND timestamp >= :startTime ORDER BY timestamp DESC",
            nativeQuery = true)
    List<DriverLocation> findLocationsByDriverIdAndTimeRange(@Param("driverId") UUID driverId,
                                                             @Param("startTime") Long startTime);

    @Query(value = "SELECT * FROM drivers.driver_locations WHERE driver_id = :driverId ORDER BY timestamp DESC LIMIT :limit",
            nativeQuery = true)
    List<DriverLocation> findRecentLocationsByDriverId(@Param("driverId") UUID driverId,
                                                       @Param("limit") int limit);

    @Query(value = "SELECT * FROM drivers.driver_locations WHERE timestamp >= :timestamp ORDER BY timestamp DESC",
            nativeQuery = true)
    List<DriverLocation> findLocationsByTimestamp(@Param("timestamp") Long timestamp);

    /**
     * Find nearby drivers using geohash prefix matching for efficient spatial queries
     * This leverages the geohash index for fast proximity searches
     */
    @Query(value = "SELECT dl.* FROM drivers.driver_locations dl " +
            "INNER JOIN ( " +
            "    SELECT driver_id, MAX(timestamp) as latest_timestamp " +
            "    FROM drivers.driver_locations " +
            "    WHERE geohash LIKE :geohashPrefix% " +
            "    GROUP BY driver_id " +
            ") latest ON dl.driver_id = latest.driver_id AND dl.timestamp = latest.latest_timestamp " +
            "ORDER BY dl.timestamp DESC",
            nativeQuery = true)
    List<DriverLocation> findNearbyDriversByGeohashPrefix(@Param("geohashPrefix") String geohashPrefix);

    /**
     * Find nearby drivers within bounding box for precise distance calculation
     * Use this as fallback when geohash prefix is not specific enough
     */
    @Query(value = "SELECT dl.* FROM drivers.driver_locations dl " +
            "INNER JOIN ( " +
            "    SELECT driver_id, MAX(timestamp) as latest_timestamp " +
            "    FROM drivers.driver_locations " +
            "    WHERE latitude BETWEEN :minLat AND :maxLat " +
            "    AND longitude BETWEEN :minLon AND :maxLon " +
            "    GROUP BY driver_id " +
            ") latest ON dl.driver_id = latest.driver_id AND dl.timestamp = latest.latest_timestamp " +
            "ORDER BY dl.timestamp DESC",
            nativeQuery = true)
    List<DriverLocation> findDriversInBoundingBox(@Param("minLat") double minLat,
                                                  @Param("maxLat") double maxLat,
                                                  @Param("minLon") double minLon,
                                                  @Param("maxLon") double maxLon);

    /**
     * Find all latest driver locations for distance calculation
     * Used when spatial optimization is not applicable
     */
    @Query(value = "SELECT dl.* FROM drivers.driver_locations dl " +
            "INNER JOIN ( " +
            "    SELECT driver_id, MAX(timestamp) as latest_timestamp " +
            "    FROM drivers.driver_locations " +
            "    GROUP BY driver_id " +
            ") latest ON dl.driver_id = latest.driver_id AND dl.timestamp = latest.latest_timestamp " +
            "ORDER BY dl.timestamp DESC",
            nativeQuery = true)
    List<DriverLocation> findAllLatestDriverLocations();
}
