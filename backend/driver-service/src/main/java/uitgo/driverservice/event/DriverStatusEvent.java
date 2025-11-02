package uitgo.driverservice.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverStatusEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("driver_id")
    private UUID driverId;

    @JsonProperty("previous_status")
    private String previousStatus;

    @JsonProperty("new_status")
    private String newStatus;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("event_type")
    @Builder.Default
    private String eventType = "STATUS_CHANGE";
}
