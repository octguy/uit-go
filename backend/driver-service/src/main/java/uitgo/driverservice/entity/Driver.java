package uitgo.driverservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "drivers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID driverId;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "license_number", length = 50, unique = true)
    private String licenseNumber;

    @Column(name = "vehicle_model", length = 255)
    private String vehicleModel;

    @Column(name = "vehicle_plate", length = 20, unique = true)
    private String vehiclePlate;

    @Column(name = "rating", columnDefinition = "DOUBLE PRECISION")
    private Double rating;

    @Column(name = "total_completed_trips")
    private Integer totalCompletedTrips;

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private DriverStatus status;

    @Column(name = "vehicle_capacity")
    private Integer vehicleCapacity;

    @Column(name = "created_at")
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = System.currentTimeMillis();
        updatedAt = System.currentTimeMillis();
        if (rating == null) {
            rating = 0.0;
        }
        if (totalCompletedTrips == null) {
            totalCompletedTrips = 0;
        }
        if (vehicleCapacity == null) {
            vehicleCapacity = 4;
        }
        if (status == null) {
            status = DriverStatus.OFFLINE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public enum DriverStatus {
        AVAILABLE, BUSY, OFFLINE, ON_BREAK
    }
}
