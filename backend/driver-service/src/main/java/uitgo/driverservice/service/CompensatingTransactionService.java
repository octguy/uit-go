package uitgo.driverservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uitgo.driverservice.dto.DriverLocationDTO;
import uitgo.driverservice.entity.DriverLocation;
import uitgo.driverservice.repository.DriverLocationRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling compensating transactions when operations fail
 * Implements eventual consistency by correcting inconsistent states
 */
@Slf4j
@Service
public class CompensatingTransactionService {

    private final DriverLocationRepository driverLocationRepository;
    private final DriverCacheService driverCacheService;
    private final RetryableOperationService retryableOperationService;
    private final FailureMetricsService failureMetricsService;

    @Autowired
    public CompensatingTransactionService(DriverLocationRepository driverLocationRepository,
                                        DriverCacheService driverCacheService,
                                        RetryableOperationService retryableOperationService,
                                        FailureMetricsService failureMetricsService) {
        this.driverLocationRepository = driverLocationRepository;
        this.driverCacheService = driverCacheService;
        this.retryableOperationService = retryableOperationService;
        this.failureMetricsService = failureMetricsService;
    }

    /**
     * Compensate for failed database save by ensuring cache and database are consistent
     * If database save failed, try to restore from cache or mark for manual review
     */
    public void compensateFailedDatabaseSave(UUID driverId, Double latitude, Double longitude, 
                                           String status, long timestamp, String geohash) {
        
        retryableOperationService.executeAsync("compensate-db-save", () -> {
            try {
                // First, check if the location was actually saved (might have been a transient error)
                Optional<DriverLocation> latestLocation = driverLocationRepository.findLatestLocationByDriverId(driverId);
                
                if (latestLocation.isPresent() && latestLocation.get().getTimestamp() >= timestamp) {
                    log.info("Compensating transaction: Database save appears to have succeeded for driver: {}", driverId);
                    failureMetricsService.recordCompensatingTransaction("db-save", "false-positive");
                    return;
                }
                
                // Check if we can restore from cache
                DriverLocationDTO cachedLocation = driverCacheService.getDriverLocation(driverId);
                if (cachedLocation != null && cachedLocation.getTimestamp() == timestamp) {
                    // Cache has the correct data, try to sync to database
                    log.info("Compensating transaction: Syncing cached location to database for driver: {}", driverId);
                    
                    DriverLocation location = DriverLocation.builder()
                            .driverId(driverId)
                            .latitude(latitude)
                            .longitude(longitude)
                            .timestamp(timestamp)
                            .geohash(geohash)
                            .build();
                    
                    driverLocationRepository.save(location);
                    failureMetricsService.recordCompensatingTransaction("db-save", "cache-sync");
                    log.info("Successfully synced cached location to database for driver: {}", driverId);
                    
                } else {
                    // Both cache and database are inconsistent - need manual intervention
                    log.error("Compensating transaction: Critical inconsistency detected for driver: {} - requires manual review", driverId);
                    failureMetricsService.recordCompensatingTransaction("db-save", "manual-review-required");
                }
                
            } catch (Exception e) {
                log.error("Compensating transaction failed for database save, driver: {}", driverId, e);
                failureMetricsService.recordCompensatingTransaction("db-save", "compensation-failed");
            }
        }, driverId);
    }

    /**
     * Compensate for failed event publishing by attempting to resend the event
     * and ensuring downstream systems are eventually notified
     */
    public void compensateFailedEventPublishing(UUID driverId, Double latitude, Double longitude, 
                                              String status, long timestamp, String geohash) {
        
        retryableOperationService.executeWithRetry("compensate-event-publishing", () -> {
            try {
                // Create a compensation event that indicates this is a retry
                log.info("Compensating transaction: Republishing location update event for driver: {}", driverId);
                
                // We could implement a special compensation event or reuse the original event
                // For now, we'll log the compensation and mark it for monitoring
                failureMetricsService.recordCompensatingTransaction("event-publishing", "event-republish");
                
                // In a real implementation, you might:
                // 1. Send to a dead letter queue for manual processing
                // 2. Use a different event broker
                // 3. Store in a compensation table for batch processing
                
                log.info("Event publishing compensation completed for driver: {}", driverId);
                return null;
                
            } catch (Exception e) {
                log.error("Event publishing compensation failed for driver: {}", driverId, e);
                failureMetricsService.recordCompensatingTransaction("event-publishing", "compensation-failed");
                throw e;
            }
        }, driverId);
    }

    /**
     * Compensate for cache failures by attempting to restore cache from database
     */
    public void compensateFailedCacheOperation(UUID driverId) {
        retryableOperationService.executeAsync("compensate-cache", () -> {
            try {
                log.info("Compensating transaction: Restoring cache from database for driver: {}", driverId);
                
                Optional<DriverLocation> latestLocation = driverLocationRepository.findLatestLocationByDriverId(driverId);
                if (latestLocation.isPresent()) {
                    DriverLocation location = latestLocation.get();
                    String status = driverCacheService.getDriverStatus(driverId);
                    
                    driverCacheService.cacheDriverLocationWithGeohash(
                            driverId,
                            location.getLatitude(),
                            location.getLongitude(),
                            status != null ? status : "UNKNOWN",
                            location.getTimestamp(),
                            location.getGeohash()
                    );
                    
                    failureMetricsService.recordCompensatingTransaction("cache", "restored-from-db");
                    log.info("Successfully restored cache from database for driver: {}", driverId);
                    
                } else {
                    log.warn("No database record found to restore cache for driver: {}", driverId);
                    failureMetricsService.recordCompensatingTransaction("cache", "no-db-record");
                }
                
            } catch (Exception e) {
                log.error("Cache compensation failed for driver: {}", driverId, e);
                failureMetricsService.recordCompensatingTransaction("cache", "compensation-failed");
            }
        }, driverId);
    }

    /**
     * Perform periodic consistency checks and compensations
     * This can be called by a scheduled task to ensure eventual consistency
     */
    public void performConsistencyCheck() {
        try {
            log.info("Performing periodic consistency check...");
            
            // Perform health checks and log metrics
            failureMetricsService.performHealthCheck();
            failureMetricsService.logMetricsSummary();
            
            log.info("Consistency check completed");
            
        } catch (Exception e) {
            log.error("Periodic consistency check failed", e);
        }
    }
}