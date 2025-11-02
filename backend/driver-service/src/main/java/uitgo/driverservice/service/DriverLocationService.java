package uitgo.driverservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uitgo.driverservice.dto.DriverLocationDTO;
import uitgo.driverservice.entity.DriverLocation;
import uitgo.driverservice.event.LocationUpdateCommittedEvent;
import uitgo.driverservice.exception.DriverServiceException;
import uitgo.driverservice.repository.DriverLocationRepository;
import uitgo.driverservice.util.DistanceCalculator;
import uitgo.driverservice.util.GeohashUtil;
import uitgo.driverservice.util.CoordinateValidator;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DriverLocationService {

    private final DriverLocationRepository driverLocationRepository;
    private final DriverCacheService driverCacheService;
    private final DistanceCalculator distanceCalculator;
    private final ApplicationEventPublisher eventPublisher;
    private final GeohashUtil geohashUtil;
    private final CoordinateValidator coordinateValidator;
    private final RetryableOperationService retryableOperationService;
    private final CompensatingTransactionService compensatingTransactionService;

    @Autowired
    public DriverLocationService(DriverLocationRepository driverLocationRepository,
                                 DriverCacheService driverCacheService,
                                 DistanceCalculator distanceCalculator,
                                 ApplicationEventPublisher eventPublisher,
                                 GeohashUtil geohashUtil,
                                 CoordinateValidator coordinateValidator,
                                 RetryableOperationService retryableOperationService,
                                 CompensatingTransactionService compensatingTransactionService) {
        this.driverLocationRepository = driverLocationRepository;
        this.driverCacheService = driverCacheService;
        this.distanceCalculator = distanceCalculator;
        this.eventPublisher = eventPublisher;
        this.geohashUtil = geohashUtil;
        this.coordinateValidator = coordinateValidator;
        this.retryableOperationService = retryableOperationService;
        this.compensatingTransactionService = compensatingTransactionService;
    }

    @Transactional
    public DriverLocationDTO updateDriverLocation(UUID driverId, Double latitude, Double longitude) {
        try {
            // Null safety checks
            if (driverId == null) {
                throw new DriverServiceException("INVALID_INPUT", "Driver ID cannot be null", null);
            }
            
            // Use centralized coordinate validation
            coordinateValidator.validateCoordinates(latitude, longitude);
            
            // Generate single timestamp and geohash for the entire operation
            long timestamp = System.currentTimeMillis();
            String geohash = geohashUtil.encode(latitude, longitude);

            String status = driverCacheService.getDriverStatus(driverId);
            if (status == null || status.isEmpty()) {
                status = "AVAILABLE";
            }

            // Use optimized cache method with pre-calculated geohash to avoid duplicate calculations
            driverCacheService.cacheDriverLocationWithGeohash(driverId, latitude, longitude, status, timestamp, geohash);
            saveLocationToDatabase(driverId, latitude, longitude, timestamp, geohash);
            
            // Publish internal event that will trigger RabbitMQ publishing after transaction commits
            publishLocationUpdateEventAfterCommit(driverId, latitude, longitude, status, timestamp, geohash);

            log.debug("Updated location for driver: {} at ({}, {})", driverId, latitude, longitude);

            return DriverLocationDTO.builder()
                    .driverId(driverId)
                    .latitude(latitude)
                    .longitude(longitude)
                    .status(status)
                    .timestamp(timestamp)
                    .build();
        } catch (DriverServiceException e) {
            log.error("Error updating driver location: {}", driverId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating driver location: {}", driverId, e);
            throw new DriverServiceException("UPDATE_ERROR", "Failed to update driver location", e.getMessage());
        }
    }

    public DriverLocationDTO getDriverLocation(UUID driverId) {
        try {
            // Null safety check
            if (driverId == null) {
                throw new DriverServiceException("INVALID_INPUT", "Driver ID cannot be null", null);
            }
            
            DriverLocationDTO cachedLocation = driverCacheService.getDriverLocation(driverId);
            if (cachedLocation != null) {
                log.debug("Retrieved driver location from cache: {}", driverId);
                return cachedLocation;
            }

            Optional<DriverLocation> dbLocation = driverLocationRepository.findLatestLocationByDriverId(driverId);
            if (dbLocation.isPresent()) {
                DriverLocation location = dbLocation.get();
                String status = driverCacheService.getDriverStatus(driverId);

                // Use optimized cache method with geohash from database to avoid recalculation
                driverCacheService.cacheDriverLocationWithGeohash(driverId, location.getLatitude(),
                        location.getLongitude(), status != null ? status : "OFFLINE", location.getTimestamp(), location.getGeohash());

                return DriverLocationDTO.builder()
                        .driverId(driverId)
                        .latitude(location.getLatitude())
                        .longitude(location.getLongitude())
                        .timestamp(location.getTimestamp())
                        .status(status)
                        .build();
            }

            return null;
        } catch (DriverServiceException e) {
            // Re-throw DriverServiceException (including null safety checks)
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving driver location: {}", driverId, e);
            throw new DriverServiceException("RETRIEVAL_ERROR", "Failed to retrieve driver location", e.getMessage());
        }
    }

    public Double calculateDistance(UUID driverId, Double targetLat, Double targetLon) {
        // Null safety checks
        if (driverId == null) {
            throw new DriverServiceException("INVALID_INPUT", "Driver ID cannot be null", null);
        }
        if (targetLat == null) {
            throw new DriverServiceException("INVALID_INPUT", "Target latitude cannot be null", null);
        }
        if (targetLon == null) {
            throw new DriverServiceException("INVALID_INPUT", "Target longitude cannot be null", null);
        }
        
        DriverLocationDTO location = getDriverLocation(driverId);
        if (location == null) {
            throw new DriverServiceException("NOT_FOUND", "Driver location not found", driverId.toString());
        }
        return distanceCalculator.calculateDistance(location.getLatitude(), location.getLongitude(),
                targetLat, targetLon);
    }

    public boolean isDriverWithinRadius(UUID driverId, Double centerLat, Double centerLon, Double radiusKm) {
        if (driverId == null) {
            throw new DriverServiceException("INVALID_INPUT", "Driver ID cannot be null", null);
        }
        if (centerLat == null) {
            throw new DriverServiceException("INVALID_INPUT", "Center latitude cannot be null", null);
        }
        if (centerLon == null) {
            throw new DriverServiceException("INVALID_INPUT", "Center longitude cannot be null", null);
        }
        if (radiusKm == null) {
            throw new DriverServiceException("INVALID_INPUT", "Radius cannot be null", null);
        }
        
        coordinateValidator.validateCoordinates(centerLat, centerLon);
        if (radiusKm <= 0) {
            throw new DriverServiceException("INVALID_INPUT", "Radius must be positive", null);
        }
        
        DriverLocationDTO location = getDriverLocation(driverId);
        if (location == null) {
            return false;
        }
        return distanceCalculator.isWithinRadius(centerLat, centerLon,
                location.getLatitude(), location.getLongitude(), radiusKm);
    }

    /**
     * Find nearby drivers using optimized spatial queries
     * Uses geohash indexing for performance when possible
     */
    public List<DriverLocationDTO> findNearbyDrivers(Double centerLat, Double centerLon, 
                                                    Double radiusKm, Integer limit) {
        coordinateValidator.validateCoordinates(centerLat, centerLon);
        if (radiusKm == null || radiusKm <= 0) {
            throw new DriverServiceException("INVALID_INPUT", "Radius must be positive", null);
        }
        if (limit == null || limit <= 0) {
            limit = 10; // Default limit
        }

        List<DriverLocation> candidateLocations;
        
        try {
            // Strategy 1: Use geohash-based lookup for efficiency
            String centerGeohash = geohashUtil.encode(centerLat, centerLon);
            int precision = distanceCalculator.getGeohashPrecisionForRadius(radiusKm);
            String geohashPrefix = centerGeohash.substring(0, Math.min(precision, centerGeohash.length()));
            
            candidateLocations = driverLocationRepository.findNearbyDriversByGeohashPrefix(geohashPrefix);
            
            // If geohash results are insufficient, fallback to bounding box
            if (candidateLocations.size() < limit * 2) {
                double[] boundingBox = distanceCalculator.getBoundingBox(centerLat, centerLon, radiusKm);
                List<DriverLocation> boundingBoxResults = driverLocationRepository.findDriversInBoundingBox(
                    boundingBox[0], boundingBox[1], boundingBox[2], boundingBox[3]);
                
                // Merge results, avoiding duplicates
                Set<UUID> existingDriverIds = candidateLocations.stream()
                    .map(DriverLocation::getDriverId)
                    .collect(Collectors.toSet());
                
                boundingBoxResults.stream()
                    .filter(location -> !existingDriverIds.contains(location.getDriverId()))
                    .forEach(candidateLocations::add);
            }
            
        } catch (Exception e) {
            log.warn("Spatial optimization failed, falling back to full scan: {}", e.getMessage());
            // Fallback: get all latest locations and filter
            candidateLocations = driverLocationRepository.findAllLatestDriverLocations();
        }

        // Calculate exact distances and filter within radius
        return candidateLocations.stream()
            .map(location -> {
                double distance = distanceCalculator.calculateDistance(
                    centerLat, centerLon, location.getLatitude(), location.getLongitude());
                
                if (distance <= radiusKm) {
                    DriverLocationDTO dto = convertToDTO(location);
                    dto.setDistance(distance); // Set calculated distance
                    return dto;
                }
                return null;
            })
            .filter(Objects::nonNull)
            .sorted((a, b) -> Double.compare(a.getDistance(), b.getDistance())) // Sort by distance
            .limit(limit)
            .collect(Collectors.toList());
    }

    private DriverLocationDTO convertToDTO(DriverLocation location) {
        return DriverLocationDTO.builder()
            .driverId(location.getDriverId())
            .latitude(location.getLatitude())
            .longitude(location.getLongitude())
            .timestamp(location.getTimestamp())
            .geohash(location.getGeohash())
            .build();
    }

    @Transactional
    private void saveLocationToDatabase(UUID driverId, Double latitude, Double longitude, long timestamp, String geohash) {
        try {
            DriverLocation location = DriverLocation.builder()
                    .driverId(driverId)
                    .latitude(latitude)
                    .longitude(longitude)
                    .timestamp(timestamp)
                    .geohash(geohash)
                    .build();
            driverLocationRepository.save(location);
        } catch (Exception e) {
            log.warn("Failed to save location to database for driver: {} - scheduling retry with compensation", driverId, e);
            
            // Use retry mechanism with compensating transaction
            retryableOperationService.executeWithRetry("database-save", () -> {
                DriverLocation location = DriverLocation.builder()
                        .driverId(driverId)
                        .latitude(latitude)
                        .longitude(longitude)
                        .timestamp(timestamp)
                        .geohash(geohash)
                        .build();
                driverLocationRepository.save(location);
                return null;
            }, driverId).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    // If all retries fail, trigger compensating transaction
                    compensatingTransactionService.compensateFailedDatabaseSave(
                            driverId, latitude, longitude, "AVAILABLE", timestamp, geohash);
                }
            });
        }
    }

    private void publishLocationUpdateEventAfterCommit(UUID driverId, Double latitude, Double longitude, String status, long timestamp, String geohash) {
        try {
            LocationUpdateCommittedEvent event = LocationUpdateCommittedEvent.builder()
                    .driverId(driverId)
                    .latitude(latitude)
                    .longitude(longitude)
                    .status(status)
                    .timestamp(timestamp)
                    .geohash(geohash)
                    .build();
            
            eventPublisher.publishEvent(event);
            log.debug("Published internal location update event for driver: {}", driverId);
        } catch (Exception e) {
            log.warn("Failed to publish internal location update event for driver: {} - scheduling retry with compensation", driverId, e);
            
            // Use retry mechanism with compensating transaction
            retryableOperationService.executeWithRetry("event-publishing", () -> {
                LocationUpdateCommittedEvent event = LocationUpdateCommittedEvent.builder()
                        .driverId(driverId)
                        .latitude(latitude)
                        .longitude(longitude)
                        .status(status)
                        .timestamp(timestamp)
                        .geohash(geohash)
                        .build();
                
                eventPublisher.publishEvent(event);
                log.debug("Retry: Published internal location update event for driver: {}", driverId);
                return null;
            }, driverId).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    // If all retries fail, trigger compensating transaction
                    compensatingTransactionService.compensateFailedEventPublishing(
                            driverId, latitude, longitude, status, timestamp, geohash);
                }
            });
        }
    }
}
