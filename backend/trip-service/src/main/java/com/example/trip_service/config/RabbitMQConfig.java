package com.example.trip_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.trip}")
    private String tripExchange;

    @Value("${rabbitmq.queue.trip-notification}")
    private String tripNotificationQueue;

    @Value("${rabbitmq.routing-key.trip-notification}")
    private String tripNotificationRoutingKey;

    @Bean
    public TopicExchange tripExchange() {
        return new TopicExchange(tripExchange);
    }

    @Bean
    public Queue tripNotificationQueue() {
        return new Queue(tripNotificationQueue, true);
    }

    @Bean
    public Binding tripNotificationBinding() {
        return BindingBuilder
                .bind(tripNotificationQueue())
                .to(tripExchange())
                .with(tripNotificationRoutingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
