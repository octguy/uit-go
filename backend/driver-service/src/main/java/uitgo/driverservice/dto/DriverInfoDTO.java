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
public class DriverInfoDTO {
    private UUID driverId;
    private String name;
    private Double latitude;
    private Double longitude;
    private Double rating;
    private String status;
    private Double distanceKm;
    private Integer totalCompletedTrips;
    private Integer vehicleCapacity;
    private String vehicleModel;
    private String vehiclePlate;
}
