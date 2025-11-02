package uitgo.driverservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uitgo.driverservice.config.RabbitMQConfig;
import uitgo.driverservice.event.DriverLocationEvent;
import uitgo.driverservice.event.DriverStatusEvent;
import uitgo.driverservice.util.GeohashUtil;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitMQ Publisher Tests")
class RabbitMQPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private GeohashUtil geohashUtil;

    @InjectMocks
    private RabbitMQPublisher rabbitMQPublisher;

    @Captor
    private ArgumentCaptor<String> exchangeCaptor;

    @Captor
    private ArgumentCaptor<String> routingKeyCaptor;

    @Captor
    private ArgumentCaptor<Object> messageCaptor;

    private UUID testDriverId;
    private Double testLatitude;
    private Double testLongitude;
    private String testStatus;
    private String testGeohash;
    private Long testTimestamp;

    @BeforeEach
    void setUp() {
        testDriverId = UUID.randomUUID();
        testLatitude = 10.7769;
        testLongitude = 106.7009;
        testStatus = "AVAILABLE";
        testGeohash = "w3gvk1hp";
        testTimestamp = System.currentTimeMillis();
    }

    @Test
    @DisplayName("Should publish location update event successfully")
    void shouldPublishLocationUpdateEventSuccessfully() {
        // Given
        when(geohashUtil.encode(testLatitude, testLongitude)).thenReturn(testGeohash);

        // When
        rabbitMQPublisher.publishLocationUpdate(testDriverId, testLatitude, testLongitude, testStatus, testTimestamp);

        // Then
        verify(geohashUtil).encode(testLatitude, testLongitude);
        verify(rabbitTemplate).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                messageCaptor.capture()
        );

        assertEquals(RabbitMQConfig.DRIVER_EXCHANGE, exchangeCaptor.getValue());
        assertEquals(RabbitMQConfig.DRIVER_LOCATION_ROUTING_KEY, routingKeyCaptor.getValue());

        Object capturedMessage = messageCaptor.getValue();
        assertInstanceOf(DriverLocationEvent.class, capturedMessage);

        DriverLocationEvent event = (DriverLocationEvent) capturedMessage;
        assertEquals(testDriverId, event.getDriverId());
        assertEquals(testLatitude, event.getLatitude());
        assertEquals(testLongitude, event.getLongitude());
        assertEquals(testStatus, event.getStatus());
        assertEquals(testGeohash, event.getGeohash());
        assertEquals("LOCATION_UPDATE", event.getEventType());
        assertEquals(testTimestamp, event.getTimestamp()); // Verify exact timestamp match
    }

    @Test
    @DisplayName("Should publish driver status change event successfully")
    void shouldPublishDriverStatusChangeEventSuccessfully() {
        // Given
        String previousStatus = "OFFLINE";
        String newStatus = "AVAILABLE";
        String reason = "Driver came online";

        // When
        rabbitMQPublisher.publishStatusChange(testDriverId, previousStatus, newStatus, reason);

        // Then
        verify(rabbitTemplate).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                messageCaptor.capture()
        );

        assertEquals(RabbitMQConfig.DRIVER_EXCHANGE, exchangeCaptor.getValue());
        assertEquals(RabbitMQConfig.DRIVER_ONLINE_ROUTING_KEY, routingKeyCaptor.getValue()); // Should be "driver.online" for AVAILABLE status

        Object capturedMessage = messageCaptor.getValue();
        assertInstanceOf(DriverStatusEvent.class, capturedMessage);

        DriverStatusEvent event = (DriverStatusEvent) capturedMessage;
        assertEquals(testDriverId, event.getDriverId());
        assertEquals(previousStatus, event.getPreviousStatus());
        assertEquals(newStatus, event.getNewStatus());
        assertEquals(reason, event.getReason());
        assertEquals("STATUS_CHANGE", event.getEventType());
        assertNotNull(event.getTimestamp());
    }

    @Test
    @DisplayName("Should publish driver online event successfully")
    void shouldPublishDriverOnlineEventSuccessfully() {
        // When
        rabbitMQPublisher.publishDriverOnline(testDriverId);

        // Then
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.DRIVER_EXCHANGE),
                eq(RabbitMQConfig.DRIVER_ONLINE_ROUTING_KEY),
                any(DriverStatusEvent.class)
        );
    }

    @Test
    @DisplayName("Should publish driver offline event successfully")
    void shouldPublishDriverOfflineEventSuccessfully() {
        // When
        rabbitMQPublisher.publishDriverOffline(testDriverId);

        // Then
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.DRIVER_EXCHANGE),
                eq(RabbitMQConfig.DRIVER_OFFLINE_ROUTING_KEY),
                any(DriverStatusEvent.class)
        );
    }

    @Test
    @DisplayName("Should handle location update with different statuses")
    void shouldHandleLocationUpdateWithDifferentStatuses() {
        // Test different driver statuses
        String[] statuses = {"AVAILABLE", "BUSY", "OFFLINE", "ON_BREAK"};
        
        when(geohashUtil.encode(anyDouble(), anyDouble())).thenReturn(testGeohash);

        for (String status : statuses) {
            // When
            rabbitMQPublisher.publishLocationUpdate(testDriverId, testLatitude, testLongitude, status, testTimestamp);
        }

        // Then
        verify(rabbitTemplate, times(4)).convertAndSend(
                eq(RabbitMQConfig.DRIVER_EXCHANGE),
                eq(RabbitMQConfig.DRIVER_LOCATION_ROUTING_KEY),
                any(DriverLocationEvent.class)
        );
    }

    @Test
    @DisplayName("Should handle different geographic locations")
    void shouldHandleDifferentGeographicLocations() {
        // Test coordinates for major Vietnamese cities
        Double[][] locations = {
                {10.7769, 106.7009}, // Ho Chi Minh City
                {21.0285, 105.8542}, // Hanoi
                {16.0544, 108.2022}, // Da Nang
                {10.0452, 105.7469}  // Can Tho
        };

        String[] expectedGeohashes = {"hcm_hash", "hanoi_hash", "danang_hash", "cantho_hash"};

        for (int i = 0; i < locations.length; i++) {
            Double lat = locations[i][0];
            Double lng = locations[i][1];
            String expectedHash = expectedGeohashes[i];

            when(geohashUtil.encode(lat, lng)).thenReturn(expectedHash);

            // When
            rabbitMQPublisher.publishLocationUpdate(testDriverId, lat, lng, testStatus, testTimestamp);

            // Then
            verify(geohashUtil).encode(lat, lng);
        }

        verify(rabbitTemplate, times(4)).convertAndSend(
                eq(RabbitMQConfig.DRIVER_EXCHANGE),
                eq(RabbitMQConfig.DRIVER_LOCATION_ROUTING_KEY),
                any(DriverLocationEvent.class)
        );
    }

    @Test
    @DisplayName("Should handle RabbitTemplate exception gracefully")
    void shouldHandleRabbitTemplateExceptionGracefully() {
        // Given
        when(geohashUtil.encode(testLatitude, testLongitude)).thenReturn(testGeohash);
        doThrow(new RuntimeException("RabbitMQ connection error"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // When & Then
        assertDoesNotThrow(() -> {
            rabbitMQPublisher.publishLocationUpdate(testDriverId, testLatitude, testLongitude, testStatus, testTimestamp);
        });

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Should handle geohash encoding exception gracefully")
    void shouldHandleGeohashEncodingExceptionGracefully() {
        // Given
        when(geohashUtil.encode(testLatitude, testLongitude))
                .thenThrow(new RuntimeException("Geohash encoding error"));

        // When & Then
        assertDoesNotThrow(() -> {
            rabbitMQPublisher.publishLocationUpdate(testDriverId, testLatitude, testLongitude, testStatus, testTimestamp);
        });

        verify(geohashUtil).encode(testLatitude, testLongitude);
    }

    @Test
    @DisplayName("Should validate event message structure for location updates")
    void shouldValidateEventMessageStructureForLocationUpdates() {
        // Given
        when(geohashUtil.encode(testLatitude, testLongitude)).thenReturn(testGeohash);

        // When
        rabbitMQPublisher.publishLocationUpdate(testDriverId, testLatitude, testLongitude, testStatus, testTimestamp);

        // Then
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), messageCaptor.capture());

        DriverLocationEvent event = (DriverLocationEvent) messageCaptor.getValue();
        
        // Validate all required fields are present
        assertNotNull(event.getDriverId());
        assertNotNull(event.getLatitude());
        assertNotNull(event.getLongitude());
        assertNotNull(event.getStatus());
        assertNotNull(event.getGeohash());
        assertNotNull(event.getEventType());
        assertNotNull(event.getTimestamp());
        
        // Validate field values
        assertEquals(testDriverId, event.getDriverId());
        assertEquals(testLatitude, event.getLatitude());
        assertEquals(testLongitude, event.getLongitude());
        assertEquals(testStatus, event.getStatus());
        assertEquals(testGeohash, event.getGeohash());
        assertEquals("LOCATION_UPDATE", event.getEventType());
        assertEquals(testTimestamp, event.getTimestamp()); // Verify exact timestamp consistency
        assertTrue(event.getLatitude() >= -90.0 && event.getLatitude() <= 90.0);
        assertTrue(event.getLongitude() >= -180.0 && event.getLongitude() <= 180.0);
        assertTrue(event.getTimestamp() > 0);
    }

    @Test
    @DisplayName("Should validate event message structure for status changes")
    void shouldValidateEventMessageStructureForStatusChanges() {
        // Given
        String previousStatus = "OFFLINE";
        String newStatus = "AVAILABLE";
        String reason = "Driver came online";

        // When
        rabbitMQPublisher.publishStatusChange(testDriverId, previousStatus, newStatus, reason);

        // Then
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), messageCaptor.capture());

        DriverStatusEvent event = (DriverStatusEvent) messageCaptor.getValue();
        
        // Validate all required fields are present
        assertNotNull(event.getDriverId());
        assertNotNull(event.getPreviousStatus());
        assertNotNull(event.getNewStatus());
        assertNotNull(event.getReason());
        assertNotNull(event.getEventType());
        assertNotNull(event.getTimestamp());
        
        // Validate field values
        assertEquals(previousStatus, event.getPreviousStatus());
        assertEquals(newStatus, event.getNewStatus());
        assertEquals(reason, event.getReason());
        assertTrue(event.getTimestamp() > 0);
    }

    @Test
    @DisplayName("Should handle concurrent message publishing")
    void shouldHandleConcurrentMessagePublishing() {
        // Given
        when(geohashUtil.encode(anyDouble(), anyDouble())).thenReturn(testGeohash);

        UUID driver1 = UUID.randomUUID();
        UUID driver2 = UUID.randomUUID();

        // When - Simulate concurrent publishing
        rabbitMQPublisher.publishLocationUpdate(driver1, 10.7769, 106.7009, "AVAILABLE", testTimestamp);
        rabbitMQPublisher.publishLocationUpdate(driver2, 21.0285, 105.8542, "BUSY", testTimestamp);
        rabbitMQPublisher.publishStatusChange(driver1, "OFFLINE", "AVAILABLE", "Driver online");

        // Then
        verify(rabbitTemplate, times(3)).convertAndSend(anyString(), anyString(), any(Object.class));
        verify(geohashUtil, times(2)).encode(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should use correct exchange and routing keys")
    void shouldUseCorrectExchangeAndRoutingKeys() {
        // Given
        when(geohashUtil.encode(testLatitude, testLongitude)).thenReturn(testGeohash);

        // When
        rabbitMQPublisher.publishLocationUpdate(testDriverId, testLatitude, testLongitude, testStatus, testTimestamp);
        rabbitMQPublisher.publishStatusChange(testDriverId, "OFFLINE", "AVAILABLE", "Driver online");

        // Then
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.DRIVER_EXCHANGE),
                eq(RabbitMQConfig.DRIVER_LOCATION_ROUTING_KEY),
                any(DriverLocationEvent.class)
        );

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.DRIVER_EXCHANGE),
                eq(RabbitMQConfig.DRIVER_ONLINE_ROUTING_KEY), // Should be "driver.online" for AVAILABLE status
                any(DriverStatusEvent.class)
        );
    }
}