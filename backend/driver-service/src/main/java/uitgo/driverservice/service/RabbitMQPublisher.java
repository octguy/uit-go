package uitgo.driverservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uitgo.driverservice.config.RabbitMQConfig;
import uitgo.driverservice.event.DriverLocationEvent;
import uitgo.driverservice.event.DriverStatusEvent;
import uitgo.driverservice.exception.DriverServiceException;
import uitgo.driverservice.util.GeohashUtil;

import java.util.UUID;

@Slf4j
@Service
public class RabbitMQPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final GeohashUtil geohashUtil;

    @Autowired
    public RabbitMQPublisher(RabbitTemplate rabbitTemplate, GeohashUtil geohashUtil) {
        this.rabbitTemplate = rabbitTemplate;
        this.geohashUtil = geohashUtil;
    }

    /**
     * Publish driver location update event
     */
    public void publishLocationUpdate(UUID driverId, Double latitude, Double longitude, String status, long timestamp) {
        if (driverId == null) {
            throw new DriverServiceException("INVALID_INPUT", "Driver ID cannot be null", null);
        }
        if (latitude == null) {
            throw new DriverServiceException("INVALID_INPUT", "Latitude cannot be null", null);
        }
        if (longitude == null) {
            throw new DriverServiceException("INVALID_INPUT", "Longitude cannot be null", null);
        }
        if (status == null) {
            throw new DriverServiceException("INVALID_INPUT", "Status cannot be null", null);
        }
        
        try {
            String geohash = geohashUtil.encode(latitude, longitude);
            publishLocationUpdateWithGeohash(driverId, latitude, longitude, status, timestamp, geohash);
        } catch (Exception e) {
            log.error("Error publishing location update for driver: {}", driverId, e);
        }
    }

    /**
     * Publish driver location update event with pre-calculated geohash (avoids recalculation)
     */
    public void publishLocationUpdateWithGeohash(UUID driverId, Double latitude, Double longitude, String status, long timestamp, String geohash) {
        if (driverId == null) {
            throw new DriverServiceException("INVALID_INPUT", "Driver ID cannot be null", null);
        }
        if (latitude == null) {
            throw new DriverServiceException("INVALID_INPUT", "Latitude cannot be null", null);
        }
        if (longitude == null) {
            throw new DriverServiceException("INVALID_INPUT", "Longitude cannot be null", null);
        }
        if (status == null) {
            throw new DriverServiceException("INVALID_INPUT", "Status cannot be null", null);
        }
        if (geohash == null) {
            throw new DriverServiceException("INVALID_INPUT", "Geohash cannot be null", null);
        }
        
        try {
            DriverLocationEvent event = DriverLocationEvent.builder()
                    .driverId(driverId)
                    .latitude(latitude)
                    .longitude(longitude)
                    .status(status)
                    .timestamp(timestamp)
                    .geohash(geohash)
                    .eventType("LOCATION_UPDATE")
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DRIVER_EXCHANGE,
                    RabbitMQConfig.DRIVER_LOCATION_ROUTING_KEY,
                    event
            );

            log.debug("Published location update for driver: {}", driverId);
        } catch (Exception e) {
            log.error("Error publishing location update for driver: {}", driverId, e);
        }
    }

    /**
     * Publish driver status change event
     */
    public void publishStatusChange(UUID driverId, String previousStatus, String newStatus, String reason) {
        if (driverId == null) {
            throw new DriverServiceException("INVALID_INPUT", "Driver ID cannot be null", null);
        }
        if (newStatus == null) {
            throw new DriverServiceException("INVALID_INPUT", "New status cannot be null", null);
        }
        
        try {
            DriverStatusEvent event = DriverStatusEvent.builder()
                    .driverId(driverId)
                    .previousStatus(previousStatus)
                    .newStatus(newStatus)
                    .timestamp(System.currentTimeMillis())
                    .reason(reason)
                    .eventType("STATUS_CHANGE")
                    .build();

            String routingKey = newStatus.equals("OFFLINE")
                    ? RabbitMQConfig.DRIVER_OFFLINE_ROUTING_KEY
                    : RabbitMQConfig.DRIVER_ONLINE_ROUTING_KEY;

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DRIVER_EXCHANGE,
                    routingKey,
                    event
            );

            log.debug("Published status change for driver: {} -> {}", driverId, newStatus);
        } catch (Exception e) {
            log.error("Error publishing status change for driver: {}", driverId, e);
        }
    }

    /**
     * Publish driver online event
     */
    public void publishDriverOnline(UUID driverId) {
        if (driverId == null) {
            throw new DriverServiceException("INVALID_INPUT", "Driver ID cannot be null", null);
        }
        
        try {
            DriverStatusEvent event = DriverStatusEvent.builder()
                    .driverId(driverId)
                    .newStatus("AVAILABLE")
                    .timestamp(System.currentTimeMillis())
                    .reason("Driver came online")
                    .eventType("DRIVER_ONLINE")
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DRIVER_EXCHANGE,
                    RabbitMQConfig.DRIVER_ONLINE_ROUTING_KEY,
                    event
            );

            log.debug("Published driver online event for: {}", driverId);
        } catch (Exception e) {
            log.error("Error publishing driver online event for: {}", driverId, e);
        }
    }

    /**
     * Publish driver offline event
     */
    public void publishDriverOffline(UUID driverId) {
        if (driverId == null) {
            throw new DriverServiceException("INVALID_INPUT", "Driver ID cannot be null", null);
        }
        
        try {
            DriverStatusEvent event = DriverStatusEvent.builder()
                    .driverId(driverId)
                    .newStatus("OFFLINE")
                    .timestamp(System.currentTimeMillis())
                    .reason("Driver went offline")
                    .eventType("DRIVER_OFFLINE")
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DRIVER_EXCHANGE,
                    RabbitMQConfig.DRIVER_OFFLINE_ROUTING_KEY,
                    event
            );

            log.debug("Published driver offline event for: {}", driverId);
        } catch (Exception e) {
            log.error("Error publishing driver offline event for: {}", driverId, e);
        }
    }
}
