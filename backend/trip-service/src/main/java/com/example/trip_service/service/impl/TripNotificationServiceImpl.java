package com.example.trip_service.service.impl;

import com.example.trip_service.dto.request.TripNotificationRequest;
import com.example.trip_service.service.ITripNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TripNotificationServiceImpl implements ITripNotificationService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.trip}")
    private String tripExchange;

    @Value("${rabbitmq.routing-key.trip-notification}")
    private String tripNotificationRoutingKey;

    public TripNotificationServiceImpl(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void notifyNearbyDrivers(TripNotificationRequest notification) {
        log.info("Publishing trip notification to RabbitMQ: tripId={}, passengerId={}", 
                notification.getTripId(), notification.getPassengerId());
        
        rabbitTemplate.convertAndSend(tripExchange, tripNotificationRoutingKey, notification);
        
        log.info("Trip notification published successfully to exchange={}, routingKey={}", 
                tripExchange, tripNotificationRoutingKey);
    }
}
