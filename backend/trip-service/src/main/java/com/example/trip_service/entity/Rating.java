package com.example.trip_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="rating")
@Getter
@Setter
public class Rating {

    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="trip_id", columnDefinition = "uuid", nullable = false, unique = true)
    private Trip trip;

    @Column(name="score", nullable = false)
    private int score; // e.g., 1 to 5 stars

    @Column(name="comment", length = 500)
    private String comment;

    @Column(name="created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        this.setCreatedAt(now);
    }
}