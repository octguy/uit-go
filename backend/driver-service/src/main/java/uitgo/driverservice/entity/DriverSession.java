package uitgo.driverservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "driver_sessions",
        indexes = {
                @Index(name = "idx_driver_session_active", columnList = "driver_id, is_active"),
                @Index(name = "idx_online_at", columnList = "online_at DESC")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID sessionId;

    @Column(name = "driver_id", nullable = false, columnDefinition = "uuid")
    private UUID driverId;

    @Column(name = "online_at", nullable = false)
    private Long onlineAt;

    @Column(name = "offline_at")
    private Long offlineAt;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "total_distance_km", columnDefinition = "DOUBLE PRECISION")
    private Double totalDistanceKm;

    @Column(name = "total_earnings", columnDefinition = "DOUBLE PRECISION")
    private Double totalEarnings;

    @PrePersist
    protected void onCreate() {
        if (sessionId == null) {
            sessionId = UUID.randomUUID();
        }
        if (onlineAt == null) {
            onlineAt = System.currentTimeMillis();
        }
        if (isActive == null) {
            isActive = true;
        }
        if (totalDistanceKm == null) {
            totalDistanceKm = 0.0;
        }
        if (totalEarnings == null) {
            totalEarnings = 0.0;
        }
    }
}
