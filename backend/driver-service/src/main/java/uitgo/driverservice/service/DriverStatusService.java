package uitgo.driverservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uitgo.driverservice.dto.DriverStatusUpdateDTO;
import uitgo.driverservice.entity.Driver;
import uitgo.driverservice.entity.DriverSession;
import uitgo.driverservice.exception.DriverServiceException;
import uitgo.driverservice.repository.DriverRepository;
import uitgo.driverservice.repository.DriverSessionRepository;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class DriverStatusService {

    private final DriverRepository driverRepository;
    private final DriverSessionRepository driverSessionRepository;
    private final DriverCacheService driverCacheService;
    private final RabbitMQPublisher rabbitMQPublisher;

    @Autowired
    public DriverStatusService(DriverRepository driverRepository,
                               DriverSessionRepository driverSessionRepository,
                               DriverCacheService driverCacheService,
                               RabbitMQPublisher rabbitMQPublisher) {
        this.driverRepository = driverRepository;
        this.driverSessionRepository = driverSessionRepository;
        this.driverCacheService = driverCacheService;
        this.rabbitMQPublisher = rabbitMQPublisher;
    }

    @Transactional
    public DriverStatusUpdateDTO updateDriverStatus(UUID driverId, String statusStr) {
        try {
            Optional<Driver> driverOpt = driverRepository.findById(driverId);
            if (driverOpt.isEmpty()) {
                throw new DriverServiceException("NOT_FOUND", "Driver not found", driverId.toString());
            }

            Driver.DriverStatus newStatus = Driver.DriverStatus.valueOf(statusStr.toUpperCase());
            Driver driver = driverOpt.get();
            String previousStatus = driver.getStatus().toString();
            driver.setStatus(newStatus);
            driver.setUpdatedAt(System.currentTimeMillis());
            driverRepository.save(driver);

            // Update cache
            driverCacheService.cacheDriverStatus(driverId, newStatus.toString());

            // Handle session management and publish events
            if (newStatus == Driver.DriverStatus.OFFLINE) {
                handleDriverGoingOffline(driverId);
                rabbitMQPublisher.publishDriverOffline(driverId);
            } else if (newStatus == Driver.DriverStatus.AVAILABLE) {
                handleDriverGoingOnline(driverId);
                rabbitMQPublisher.publishDriverOnline(driverId);
            } else {
                rabbitMQPublisher.publishStatusChange(driverId, previousStatus, newStatus.toString(), "Status changed");
            }

            log.debug("Updated driver status: {} -> {}", driverId, newStatus);

            return DriverStatusUpdateDTO.builder()
                    .driverId(driverId)
                    .status(newStatus.toString())
                    .updatedAt(System.currentTimeMillis())
                    .success(true)
                    .message("Status updated successfully")
                    .build();

        } catch (DriverServiceException e) {
            log.error("Error updating driver status: {}", driverId, e);
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", statusStr);
            throw new DriverServiceException("INVALID_STATUS", "Invalid driver status", statusStr);
        } catch (Exception e) {
            log.error("Unexpected error updating driver status: {}", driverId, e);
            throw new DriverServiceException("UPDATE_ERROR", "Failed to update driver status", e.getMessage());
        }
    }

    public DriverStatusUpdateDTO getDriverStatus(UUID driverId) {
        try {
            Optional<Driver> driverOpt = driverRepository.findById(driverId);
            if (driverOpt.isEmpty()) {
                throw new DriverServiceException("NOT_FOUND", "Driver not found", driverId.toString());
            }

            Driver driver = driverOpt.get();

            return DriverStatusUpdateDTO.builder()
                    .driverId(driverId)
                    .status(driver.getStatus().toString())
                    .updatedAt(driver.getUpdatedAt())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving driver status: {}", driverId, e);
            throw new DriverServiceException("RETRIEVAL_ERROR", "Failed to retrieve driver status", e.getMessage());
        }
    }

    @Transactional
    private void handleDriverGoingOffline(UUID driverId) {
        try {
            Optional<DriverSession> activeSession = driverSessionRepository.findActiveSessionByDriverId(driverId);
            if (activeSession.isPresent()) {
                DriverSession session = activeSession.get();
                session.setOfflineAt(System.currentTimeMillis());
                session.setIsActive(false);
                driverSessionRepository.save(session);
                log.debug("Closed driver session for: {}", driverId);
            }
        } catch (Exception e) {
            log.warn("Error handling driver going offline: {}", driverId, e);
        }
    }

    @Transactional
    private void handleDriverGoingOnline(UUID driverId) {
        try {
            DriverSession newSession = DriverSession.builder()
                    .driverId(driverId)
                    .onlineAt(System.currentTimeMillis())
                    .isActive(true)
                    .totalDistanceKm(0.0)
                    .totalEarnings(0.0)
                    .build();
            driverSessionRepository.save(newSession);
            log.debug("Created new driver session for: {}", driverId);
        } catch (Exception e) {
            log.warn("Error handling driver going online: {}", driverId, e);
        }
    }
}
