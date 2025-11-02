package uitgo.driverservice.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripCreatedEvent {
    @JsonProperty("trip_id")
    private UUID tripId;

    @JsonProperty("passenger_id")
    private UUID passengerId;

    @JsonProperty("pickup_latitude")
    private Double pickupLatitude;

    @JsonProperty("pickup_longitude")
    private Double pickupLongitude;

    @JsonProperty("dropoff_latitude")
    private Double dropoffLatitude;

    @JsonProperty("dropoff_longitude")
    private Double dropoffLongitude;

    @JsonProperty("trip_type")
    private String tripType;

    @JsonProperty("estimated_fare")
    private String estimatedFare;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("event_type")
    @Builder.Default
    private String eventType = "TRIP_CREATED";
}
