package uitgo.driverservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uitgo.driverservice.entity.Driver;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {

    Optional<Driver> findByDriverId(UUID driverId);

    Optional<Driver> findByUserId(UUID userId);

    Optional<Driver> findByLicenseNumber(String licenseNumber);

    Optional<Driver> findByVehiclePlate(String vehiclePlate);

    @Query("SELECT d FROM Driver d WHERE d.status = :status")
    List<Driver> findByStatus(@Param("status") Driver.DriverStatus status);

    @Query("SELECT d FROM Driver d WHERE d.status = 'AVAILABLE' ORDER BY d.rating DESC")
    List<Driver> findAvailableDriversSortedByRating();

    @Query(value = "SELECT COUNT(*) FROM drivers.drivers WHERE status = :status",
            nativeQuery = true)
    long countByStatus(@Param("status") String status);

    @Query("SELECT d FROM Driver d WHERE d.rating >= :minRating AND d.status = :status")
    List<Driver> findByRatingAndStatus(@Param("minRating") Double minRating,
                                       @Param("status") Driver.DriverStatus status);

    @Query(value = "SELECT d.* FROM drivers.drivers d LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<Driver> findAvailableDriversPaginated(@Param("limit") int limit,
                                               @Param("offset") int offset);
}
