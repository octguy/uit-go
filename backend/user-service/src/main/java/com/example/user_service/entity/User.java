package com.example.user_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "\"user\"")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(unique = true, nullable = false)
    private String email;

    @Column(name="password", nullable = false)
    private String password; // not hashed for simplicity

    @Column(name="user_type", nullable = false)
    private String userType; // "PASSENGER" or "DRIVER"
    
    @Column(name="name", nullable = false)
    private String name;

    @Column(name="phone")
    private String phone;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name="deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    public void onCreate() {
        this.setCreatedAt(LocalDateTime.now());
    }
}