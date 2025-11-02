package com.example.trip_service.repository;

import com.example.trip_service.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {
    
    List<Trip> findByPassengerId(UUID passengerId);
    
    List<Trip> findByDriverId(UUID driverId);
    
    @Query("SELECT t FROM Trip t WHERE t.passengerId = :passengerId ORDER BY t.createdAt DESC")
    List<Trip> findByPassengerIdOrderByCreatedAtDesc(@Param("passengerId") UUID passengerId);
    
    @Query("SELECT t FROM Trip t WHERE t.driverId = :driverId ORDER BY t.createdAt DESC")
    List<Trip> findByDriverIdOrderByCreatedAtDesc(@Param("driverId") UUID driverId);
}