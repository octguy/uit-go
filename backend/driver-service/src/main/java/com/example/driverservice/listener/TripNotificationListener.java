package com.example.driverservice.listener;

import com.example.driverservice.dto.TripNotificationRequest;
import com.example.driverservice.service.ITripNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TripNotificationListener {

    private final ITripNotificationService tripNotificationService;

    public TripNotificationListener(ITripNotificationService tripNotificationService) {
        this.tripNotificationService = tripNotificationService;
    }

    @RabbitListener(queues = "${rabbitmq.queue.trip-notification}")
    public void handleTripNotification(TripNotificationRequest notification) {
        log.info("Received trip notification from RabbitMQ: tripId={}, passengerId={}", 
                notification.getTripId(), notification.getPassengerId());

        try {
            tripNotificationService.handleTripNotification(notification);
            log.info("Successfully processed trip notification: tripId={}", notification.getTripId());
        } catch (Exception e) {
            log.error("Error processing trip notification: tripId={}, error={}", 
                    notification.getTripId(), e.getMessage(), e);
            throw e; // Re-throw to let RabbitMQ handle retry
        }
    }
}
