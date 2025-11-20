package com.example.trip_service.entity;

import com.example.trip_service.enums.TripStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="trip")
@Getter
@Setter
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name="passenger_id", columnDefinition = "uuid", nullable = false)
    private UUID passengerId;

    @Column(name="driver_id", columnDefinition = "uuid")
    private UUID driverId;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false)
    private TripStatus status;

    @Column(name="pickup_location", nullable = false)
    private String pickupLocation;

    @Column(name="destination", nullable = false)
    private String destination;

    @Column(name="pickup_latitude", nullable = false)
    private Double pickupLatitude;

    @Column(name="pickup_longitude", nullable = false)
    private Double pickupLongitude;

    @Column(name="destination_latitude", nullable = false)
    private Double destinationLatitude;

    @Column(name="destination_longitude", nullable = false)
    private Double destinationLongitude;

    @Column(name="fare")
    private BigDecimal fare;

    @Column(name="requested_at", nullable = false)
    private LocalDateTime requestedAt; // always not null

    @Column(name="started_at")
    private LocalDateTime startedAt; // null if it is cancelled

    @Column(name="completed_at")
    private LocalDateTime completedAt; // null if it is cancelled

}