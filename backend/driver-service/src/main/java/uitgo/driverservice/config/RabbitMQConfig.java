package uitgo.driverservice.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String DRIVER_EXCHANGE = "driver.events";
    public static final String TRIP_EXCHANGE = "trip.events";

    // Queue names
    public static final String DRIVER_LOCATION_QUEUE = "driver.location.updates";
    public static final String DRIVER_STATUS_QUEUE = "driver.status.changes";
    public static final String DRIVER_ONLINE_QUEUE = "driver.online";
    public static final String DRIVER_OFFLINE_QUEUE = "driver.offline";

    // Routing keys
    public static final String DRIVER_LOCATION_ROUTING_KEY = "driver.location.updated";
    public static final String DRIVER_STATUS_ROUTING_KEY = "driver.status.changed";
    public static final String DRIVER_ONLINE_ROUTING_KEY = "driver.online";
    public static final String DRIVER_OFFLINE_ROUTING_KEY = "driver.offline";
    public static final String TRIP_CREATED_ROUTING_KEY = "trip.created";

    @Value("${spring.rabbitmq.host:localhost}")
    private String host;

    @Value("${spring.rabbitmq.port:5672}")
    private int port;

    // ==================== DRIVER EVENTS EXCHANGE & QUEUES ====================

    @Bean
    public TopicExchange driverExchange() {
        return new TopicExchange(DRIVER_EXCHANGE, true, false);
    }

    @Bean
    public Queue driverLocationQueue() {
        return QueueBuilder.durable(DRIVER_LOCATION_QUEUE)
                .ttl(3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue driverStatusQueue() {
        return QueueBuilder.durable(DRIVER_STATUS_QUEUE)
                .ttl(3600000)
                .build();
    }

    @Bean
    public Queue driverOnlineQueue() {
        return QueueBuilder.durable(DRIVER_ONLINE_QUEUE)
                .ttl(3600000)
                .build();
    }

    @Bean
    public Queue driverOfflineQueue() {
        return QueueBuilder.durable(DRIVER_OFFLINE_QUEUE)
                .ttl(3600000)
                .build();
    }

    // Bindings for driver events
    @Bean
    public Binding driverLocationBinding(Queue driverLocationQueue, TopicExchange driverExchange) {
        return BindingBuilder.bind(driverLocationQueue)
                .to(driverExchange)
                .with(DRIVER_LOCATION_ROUTING_KEY);
    }

    @Bean
    public Binding driverStatusBinding(Queue driverStatusQueue, TopicExchange driverExchange) {
        return BindingBuilder.bind(driverStatusQueue)
                .to(driverExchange)
                .with(DRIVER_STATUS_ROUTING_KEY);
    }

    @Bean
    public Binding driverOnlineBinding(Queue driverOnlineQueue, TopicExchange driverExchange) {
        return BindingBuilder.bind(driverOnlineQueue)
                .to(driverExchange)
                .with(DRIVER_ONLINE_ROUTING_KEY);
    }

    @Bean
    public Binding driverOfflineBinding(Queue driverOfflineQueue, TopicExchange driverExchange) {
        return BindingBuilder.bind(driverOfflineQueue)
                .to(driverExchange)
                .with(DRIVER_OFFLINE_ROUTING_KEY);
    }

    // ==================== TRIP EVENTS EXCHANGE & QUEUES ====================

    @Bean
    public TopicExchange tripExchange() {
        return new TopicExchange(TRIP_EXCHANGE, true, false);
    }

    @Bean
    public Queue tripCreatedQueue() {
        return QueueBuilder.durable("trip.created.queue")
                .ttl(3600000)
                .build();
    }

    @Bean
    public Binding tripCreatedBinding(Queue tripCreatedQueue, TopicExchange tripExchange) {
        return BindingBuilder.bind(tripCreatedQueue)
                .to(tripExchange)
                .with(TRIP_CREATED_ROUTING_KEY);
    }
}
