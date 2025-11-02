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
public class DriverStatusUpdateDTO {
    private UUID driverId;
    private String status;
    private Long updatedAt;
    private Boolean success;
    private String message;
}
