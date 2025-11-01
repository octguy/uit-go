package com.example.driver_service.repository;

import com.example.driver_service.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    
    Optional<Driver> findByUserId(Long userId);
    
    List<Driver> findByStatus(String status);
    
    boolean existsByVehiclePlate(String vehiclePlate);
    
    boolean existsByUserId(Long userId);
    
    // Find drivers within a radius using Haversine formula (simplified)
    @Query("SELECT d FROM Driver d WHERE d.status = 'AVAILABLE' " +
           "AND (6371 * acos(cos(radians(:latitude)) * cos(radians(d.currentLatitude)) * " +
           "cos(radians(d.currentLongitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(d.currentLatitude)))) < :radiusKm")
    List<Driver> findNearbyAvailableDrivers(@Param("latitude") Double latitude, 
                                           @Param("longitude") Double longitude, 
                                           @Param("radiusKm") Double radiusKm);
}