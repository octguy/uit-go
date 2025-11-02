package uitgo.driverservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import uitgo.driverservice.event.LocationUpdateCommittedEvent;

/**
 * Handles events that should be published after database transactions commit
 * This ensures proper transaction boundary separation between database operations and messaging
 */
@Slf4j
@Component
public class TransactionalEventPublisher {

    private final RabbitMQPublisher rabbitMQPublisher;

    @Autowired
    public TransactionalEventPublisher(RabbitMQPublisher rabbitMQPublisher) {
        this.rabbitMQPublisher = rabbitMQPublisher;
    }

    /**
     * Publishes location update to RabbitMQ after the database transaction commits successfully
     * This prevents database rollbacks if messaging fails
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLocationUpdateCommitted(LocationUpdateCommittedEvent event) {
        try {
            log.debug("Publishing location update event after transaction commit for driver: {}", event.getDriverId());
            rabbitMQPublisher.publishLocationUpdateWithGeohash(
                    event.getDriverId(),
                    event.getLatitude(),
                    event.getLongitude(),
                    event.getStatus(),
                    event.getTimestamp(),
                    event.getGeohash() // Use pre-calculated geohash to avoid recalculation
            );
        } catch (Exception e) {
            log.error("Failed to publish location update event after transaction commit for driver: {}", 
                    event.getDriverId(), e);
            // Note: We don't re-throw here as the database transaction has already committed
            // Consider implementing a retry mechanism or dead letter queue for failed events
        }
    }
}