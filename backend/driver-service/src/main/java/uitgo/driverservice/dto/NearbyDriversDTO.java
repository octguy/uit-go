package uitgo.driverservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyDriversDTO {
    private List<DriverInfoDTO> drivers;
    private Integer count;
    private Boolean success;
    private String message;
}
