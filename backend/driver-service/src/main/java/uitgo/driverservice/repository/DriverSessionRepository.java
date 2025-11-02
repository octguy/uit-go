package uitgo.driverservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uitgo.driverservice.entity.DriverSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverSessionRepository extends JpaRepository<DriverSession, UUID> {

    @Query(value = "SELECT * FROM drivers.driver_sessions WHERE driver_id = :driverId AND is_active = true",
            nativeQuery = true)
    Optional<DriverSession> findActiveSessionByDriverId(@Param("driverId") UUID driverId);

    @Query(value = "SELECT * FROM drivers.driver_sessions WHERE driver_id = :driverId ORDER BY online_at DESC LIMIT 1",
            nativeQuery = true)
    Optional<DriverSession> findLatestSessionByDriverId(@Param("driverId") UUID driverId);

    @Query(value = "SELECT * FROM drivers.driver_sessions WHERE is_active = true ORDER BY online_at DESC LIMIT :limit",
            nativeQuery = true)
    List<DriverSession> findActiveSessions(@Param("limit") int limit);

    @Query(value = "SELECT COUNT(*) FROM drivers.driver_sessions WHERE is_active = true",
            nativeQuery = true)
    long countActiveSessions();
}
