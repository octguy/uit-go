package uitgo.driverservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Internal event to trigger location update publishing after transaction commits
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateCommittedEvent {
    private UUID driverId;
    private Double latitude;
    private Double longitude;
    private String status;
    private long timestamp;
    private String geohash; // Include geohash to avoid recalculation
}