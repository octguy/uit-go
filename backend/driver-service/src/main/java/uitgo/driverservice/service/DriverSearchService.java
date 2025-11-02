package uitgo.driverservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uitgo.driverservice.dto.DriverInfoDTO;
import uitgo.driverservice.dto.DriverLocationDTO;
import uitgo.driverservice.dto.NearbyDriversDTO;
import uitgo.driverservice.entity.Driver;
import uitgo.driverservice.entity.DriverLocation;
import uitgo.driverservice.exception.DriverServiceException;
import uitgo.driverservice.repository.DriverLocationRepository;
import uitgo.driverservice.repository.DriverRepository;
import uitgo.driverservice.util.DistanceCalculator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DriverSearchService {

    private final DriverRepository driverRepository;
    private final DriverLocationRepository driverLocationRepository;
    private final DriverLocationService driverLocationService;
    private final DistanceCalculator distanceCalculator;

    @Autowired
    public DriverSearchService(DriverRepository driverRepository,
                               DriverLocationRepository driverLocationRepository,
                               DriverLocationService driverLocationService,
                               DistanceCalculator distanceCalculator) {
        this.driverRepository = driverRepository;
        this.driverLocationRepository = driverLocationRepository;
        this.driverLocationService = driverLocationService;
        this.distanceCalculator = distanceCalculator;
    }

    public NearbyDriversDTO searchNearbyDrivers(Double latitude, Double longitude,
                                                Double radiusKm, Integer limit,
                                                String tripType) {
        try {
            validateSearchParameters(latitude, longitude, radiusKm, limit);

            // Get all available drivers
            List<Driver> availableDrivers = driverRepository
                    .findByStatus(Driver.DriverStatus.AVAILABLE);

            if (availableDrivers.isEmpty()) {
                return NearbyDriversDTO.builder()
                        .drivers(new ArrayList<>())
                        .count(0)
                        .success(true)
                        .message("No available drivers found")
                        .build();
            }

            // Filter and rank by distance
            List<DriverInfoDTO> nearbyDrivers = availableDrivers.stream()
                    .map(driver -> {
                        Optional<DriverLocation> location = driverLocationRepository
                                .findLatestLocationByDriverId(driver.getDriverId());

                        if (location.isPresent()) {
                            DriverLocation driverLoc = location.get();
                            double distance = distanceCalculator.calculateDistance(
                                    latitude, longitude,
                                    driverLoc.getLatitude(), driverLoc.getLongitude());

                            if (distance <= radiusKm) {
                                return DriverInfoDTO.builder()
                                        .driverId(driver.getDriverId())
                                        .name("Driver " + driver.getDriverId().toString().substring(0, 8))
                                        .latitude(driverLoc.getLatitude())
                                        .longitude(driverLoc.getLongitude())
                                        .rating(driver.getRating())
                                        .status(driver.getStatus().toString())
                                        .distanceKm(distance)
                                        .totalCompletedTrips(driver.getTotalCompletedTrips())
                                        .vehicleCapacity(driver.getVehicleCapacity())
                                        .vehicleModel(driver.getVehicleModel())
                                        .vehiclePlate(driver.getVehiclePlate())
                                        .build();
                            }
                        }
                        return null;
                    })
                    .filter(driver -> driver != null)
                    .sorted(Comparator.comparingDouble(DriverInfoDTO::getDistanceKm)
                            .thenComparingDouble(DriverInfoDTO::getRating).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());

            log.debug("Found {} nearby drivers for location ({}, {})",
                    nearbyDrivers.size(), latitude, longitude);

            return NearbyDriversDTO.builder()
                    .drivers(nearbyDrivers)
                    .count(nearbyDrivers.size())
                    .success(true)
                    .message("Found " + nearbyDrivers.size() + " nearby drivers")
                    .build();

        } catch (DriverServiceException e) {
            log.error("Search error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during nearby driver search", e);
            throw new DriverServiceException("SEARCH_ERROR", "Failed to search nearby drivers", e.getMessage());
        }
    }

    public List<DriverInfoDTO> getAvailableDrivers(Integer limit, Integer offset) {
        try {
            List<Driver> drivers = driverRepository.findAvailableDriversPaginated(limit, offset);

            return drivers.stream()
                    .map(driver -> {
                        Optional<DriverLocation> location = driverLocationRepository
                                .findLatestLocationByDriverId(driver.getDriverId());

                        return DriverInfoDTO.builder()
                                .driverId(driver.getDriverId())
                                .name("Driver " + driver.getDriverId().toString().substring(0, 8))
                                .latitude(location.map(DriverLocation::getLatitude).orElse(null))
                                .longitude(location.map(DriverLocation::getLongitude).orElse(null))
                                .rating(driver.getRating())
                                .status(driver.getStatus().toString())
                                .totalCompletedTrips(driver.getTotalCompletedTrips())
                                .vehicleCapacity(driver.getVehicleCapacity())
                                .vehicleModel(driver.getVehicleModel())
                                .vehiclePlate(driver.getVehiclePlate())
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error retrieving available drivers", e);
            throw new DriverServiceException("RETRIEVAL_ERROR", "Failed to retrieve available drivers", e.getMessage());
        }
    }

    private void validateSearchParameters(Double latitude, Double longitude,
                                          Double radiusKm, Integer limit) {
        if (latitude == null || longitude == null) {
            throw new DriverServiceException("INVALID_INPUT", "Latitude and longitude are required", null);
        }
        if (radiusKm == null || radiusKm <= 0) {
            throw new DriverServiceException("INVALID_INPUT", "Radius must be greater than 0", null);
        }
        if (limit == null || limit <= 0) {
            throw new DriverServiceException("INVALID_INPUT", "Limit must be greater than 0", null);
        }
    }
}
