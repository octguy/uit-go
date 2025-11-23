package com.example.user_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "driver")
@Getter
@Setter
public class Driver {

    @Id
    @Column(name="id", columnDefinition = "uuid")
    private UUID id;

    @OneToOne
    @MapsId
    @JoinColumn(name="id")
    private User user;

    @Column(name="vehicle_model", nullable = false)
    private String vehicleModel;

    @Column(name="vehicle_number", nullable = false)
    private String vehicleNumber;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.setCreatedAt(LocalDateTime.now());
    }
}
