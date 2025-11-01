package com.example.trip_service.repository;

import com.example.trip_service.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {
    
    List<Trip> findByPassengerId(UUID passengerId);
    
    List<Trip> findByDriverId(UUID driverId);
    
    List<Trip> findByStatus(String status);
    
    List<Trip> findByPassengerIdAndStatus(UUID passengerId, String status);
    
    List<Trip> findByDriverIdAndStatus(UUID driverId, String status);
}