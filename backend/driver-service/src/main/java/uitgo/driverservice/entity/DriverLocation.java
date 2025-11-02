package uitgo.driverservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "driver_locations",
        indexes = {
                @Index(name = "idx_driver_timestamp", columnList = "driver_id, timestamp DESC"),
                @Index(name = "idx_timestamp", columnList = "timestamp DESC"),
                @Index(name = "idx_geohash", columnList = "geohash")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID locationId;

    @Column(name = "driver_id", nullable = false, columnDefinition = "uuid")
    private UUID driverId;

    @Column(name = "latitude", nullable = false, columnDefinition = "DOUBLE PRECISION")
    private Double latitude;

    @Column(name = "longitude", nullable = false, columnDefinition = "DOUBLE PRECISION")
    private Double longitude;

    @Column(name = "timestamp", nullable = false)
    private Long timestamp;

    @Column(name = "geohash", length = 50)
    private String geohash;
}
