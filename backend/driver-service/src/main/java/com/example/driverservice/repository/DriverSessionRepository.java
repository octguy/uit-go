package com.example.driverservice.repository;

import com.example.driverservice.entity.DriverSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DriverSessionRepository extends JpaRepository<DriverSession, UUID> {
}
