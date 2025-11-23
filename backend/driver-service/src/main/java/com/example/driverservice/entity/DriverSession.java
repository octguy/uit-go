package com.example.driverservice.entity;

import com.example.driverservice.enums.DriverStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="driver_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverSession {

    @Id
    @Column(name="id", columnDefinition = "uuid")
    private UUID id;

    @Column(name="status", nullable = false)
    @Enumerated(EnumType.STRING)
    private DriverStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
