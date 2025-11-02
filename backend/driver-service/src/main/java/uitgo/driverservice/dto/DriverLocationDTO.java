package uitgo.driverservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverLocationDTO {
    private UUID driverId;
    private Double latitude;
    private Double longitude;
    private Long timestamp;
    private String status;
    private Double distanceKm;
    private String geohash;
    private Double distance; // Distance from search center (calculated field)
}
