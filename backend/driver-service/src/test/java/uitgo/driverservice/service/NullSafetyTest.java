package uitgo.driverservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import uitgo.driverservice.exception.DriverServiceException;
import uitgo.driverservice.repository.DriverLocationRepository;
import uitgo.driverservice.util.DistanceCalculator;
import uitgo.driverservice.util.GeohashUtil;
import uitgo.driverservice.util.CoordinateValidator;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("Null Safety Tests")
class NullSafetyTest {

    @Mock
    private DriverCacheService driverCacheService;
    
    @Mock
    private RabbitMQPublisher rabbitMQPublisher;
    
    @Mock
    private DistanceCalculator distanceCalculator;
    
    @Mock
    private DriverLocationRepository driverLocationRepository;
    
    @Mock
    private GeohashUtil geohashUtil;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private RabbitTemplate rabbitTemplate;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock 
    private GeohashUtil geohashUtilMock;

    private DriverLocationService driverLocationService;
    private DriverCacheService realDriverCacheService;
    private CoordinateValidator realCoordinateValidator;
    private RabbitMQPublisher realRabbitMQPublisher;

    @BeforeEach
    void setUp() {
        // Mock geohash util to return valid geohash for location updates (lenient to avoid unnecessary stubbing errors)
        lenient().when(geohashUtilMock.encode(anyDouble(), anyDouble())).thenReturn("w3gvk1hp");
        
        // Use real coordinate validator for null safety testing
        realCoordinateValidator = new CoordinateValidator();
        
        // Mock the new dependencies for testing
        RetryableOperationService mockRetryableOperationService = mock(RetryableOperationService.class);
        CompensatingTransactionService mockCompensatingTransactionService = mock(CompensatingTransactionService.class);
        
        // Create real instances for null safety testing
        driverLocationService = new DriverLocationService(
                driverLocationRepository, driverCacheService, distanceCalculator, eventPublisher, 
                geohashUtilMock, realCoordinateValidator, mockRetryableOperationService, mockCompensatingTransactionService
        );
        
        realDriverCacheService = new DriverCacheService(redisTemplate, geohashUtil);
        realRabbitMQPublisher = new RabbitMQPublisher(rabbitTemplate, geohashUtil);
    }

    @Test
    @DisplayName("DriverLocationService should throw exception for null driverId in updateDriverLocation")
    void shouldThrowExceptionForNullDriverIdInUpdateDriverLocation() {
        // When & Then
        DriverServiceException exception = assertThrows(DriverServiceException.class, () ->
                driverLocationService.updateDriverLocation(null, 10.7769, 106.7009)
        );
        
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertEquals("Driver ID cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("DriverLocationService should throw exception for null latitude in updateDriverLocation")
    void shouldThrowExceptionForNullLatitudeInUpdateDriverLocation() {
        UUID driverId = UUID.randomUUID();
        
        // When & Then
        DriverServiceException exception = assertThrows(DriverServiceException.class, () ->
                driverLocationService.updateDriverLocation(driverId, null, 106.7009)
        );
        
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertEquals("Latitude cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("DriverLocationService should throw exception for null longitude in updateDriverLocation")
    void shouldThrowExceptionForNullLongitudeInUpdateDriverLocation() {
        UUID driverId = UUID.randomUUID();
        
        // When & Then
        DriverServiceException exception = assertThrows(DriverServiceException.class, () ->
                driverLocationService.updateDriverLocation(driverId, 10.7769, null)
        );
        
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertEquals("Longitude cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("DriverLocationService should throw exception for null driverId in getDriverLocation")
    void shouldThrowExceptionForNullDriverIdInGetDriverLocation() {
        // When & Then
        DriverServiceException exception = assertThrows(DriverServiceException.class, () ->
                driverLocationService.getDriverLocation(null)
        );
        
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertEquals("Driver ID cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("DriverLocationService should throw exception for null parameters in calculateDistance")
    void shouldThrowExceptionForNullParametersInCalculateDistance() {
        UUID driverId = UUID.randomUUID();
        
        // Test null driverId
        DriverServiceException exception1 = assertThrows(DriverServiceException.class, () ->
                driverLocationService.calculateDistance(null, 10.7769, 106.7009)
        );
        assertEquals("INVALID_INPUT", exception1.getErrorCode());
        assertEquals("Driver ID cannot be null", exception1.getMessage());
        
        // Test null targetLat
        DriverServiceException exception2 = assertThrows(DriverServiceException.class, () ->
                driverLocationService.calculateDistance(driverId, null, 106.7009)
        );
        assertEquals("INVALID_INPUT", exception2.getErrorCode());
        assertEquals("Target latitude cannot be null", exception2.getMessage());
        
        // Test null targetLon
        DriverServiceException exception3 = assertThrows(DriverServiceException.class, () ->
                driverLocationService.calculateDistance(driverId, 10.7769, null)
        );
        assertEquals("INVALID_INPUT", exception3.getErrorCode());
        assertEquals("Target longitude cannot be null", exception3.getMessage());
    }

    @Test
    @DisplayName("DriverLocationService should throw exception for null parameters in isDriverWithinRadius")
    void shouldThrowExceptionForNullParametersInIsDriverWithinRadius() {
        UUID driverId = UUID.randomUUID();
        
        // Test null driverId
        DriverServiceException exception1 = assertThrows(DriverServiceException.class, () ->
                driverLocationService.isDriverWithinRadius(null, 10.7769, 106.7009, 5.0)
        );
        assertEquals("INVALID_INPUT", exception1.getErrorCode());
        assertEquals("Driver ID cannot be null", exception1.getMessage());
        
        // Test null centerLat
        DriverServiceException exception2 = assertThrows(DriverServiceException.class, () ->
                driverLocationService.isDriverWithinRadius(driverId, null, 106.7009, 5.0)
        );
        assertEquals("INVALID_INPUT", exception2.getErrorCode());
        assertEquals("Center latitude cannot be null", exception2.getMessage());
        
        // Test null centerLon
        DriverServiceException exception3 = assertThrows(DriverServiceException.class, () ->
                driverLocationService.isDriverWithinRadius(driverId, 10.7769, null, 5.0)
        );
        assertEquals("INVALID_INPUT", exception3.getErrorCode());
        assertEquals("Center longitude cannot be null", exception3.getMessage());
        
        // Test null radius
        DriverServiceException exception4 = assertThrows(DriverServiceException.class, () ->
                driverLocationService.isDriverWithinRadius(driverId, 10.7769, 106.7009, null)
        );
        assertEquals("INVALID_INPUT", exception4.getErrorCode());
        assertEquals("Radius cannot be null", exception4.getMessage());
    }

    @Test
    @DisplayName("DriverCacheService should throw exception for null parameters in cacheDriverLocation")
    void shouldThrowExceptionForNullParametersInCacheDriverLocation() {
        UUID driverId = UUID.randomUUID();
        
        // Test null driverId
        DriverServiceException exception1 = assertThrows(DriverServiceException.class, () ->
                realDriverCacheService.cacheDriverLocation(null, 10.7769, 106.7009, "AVAILABLE", System.currentTimeMillis())
        );
        assertEquals("INVALID_INPUT", exception1.getErrorCode());
        assertEquals("Driver ID cannot be null", exception1.getMessage());
        
        // Test null latitude
        DriverServiceException exception2 = assertThrows(DriverServiceException.class, () ->
                realDriverCacheService.cacheDriverLocation(driverId, null, 106.7009, "AVAILABLE", System.currentTimeMillis())
        );
        assertEquals("INVALID_INPUT", exception2.getErrorCode());
        assertEquals("Latitude cannot be null", exception2.getMessage());
        
        // Test null longitude
        DriverServiceException exception3 = assertThrows(DriverServiceException.class, () ->
                realDriverCacheService.cacheDriverLocation(driverId, 10.7769, null, "AVAILABLE", System.currentTimeMillis())
        );
        assertEquals("INVALID_INPUT", exception3.getErrorCode());
        assertEquals("Longitude cannot be null", exception3.getMessage());
        
        // Test null status
        DriverServiceException exception4 = assertThrows(DriverServiceException.class, () ->
                realDriverCacheService.cacheDriverLocation(driverId, 10.7769, 106.7009, null, System.currentTimeMillis())
        );
        assertEquals("INVALID_INPUT", exception4.getErrorCode());
        assertEquals("Status cannot be null", exception4.getMessage());
    }

    @Test
    @DisplayName("DriverCacheService should throw exception for null driverId in cache operations")
    void shouldThrowExceptionForNullDriverIdInCacheOperations() {
        // Test getDriverLocation
        DriverServiceException exception1 = assertThrows(DriverServiceException.class, () ->
                realDriverCacheService.getDriverLocation(null)
        );
        assertEquals("INVALID_INPUT", exception1.getErrorCode());
        assertEquals("Driver ID cannot be null", exception1.getMessage());
        
        // Test cacheDriverStatus
        DriverServiceException exception2 = assertThrows(DriverServiceException.class, () ->
                realDriverCacheService.cacheDriverStatus(null, "AVAILABLE")
        );
        assertEquals("INVALID_INPUT", exception2.getErrorCode());
        assertEquals("Driver ID cannot be null", exception2.getMessage());
        
        // Test getDriverStatus
        DriverServiceException exception3 = assertThrows(DriverServiceException.class, () ->
                realDriverCacheService.getDriverStatus(null)
        );
        assertEquals("INVALID_INPUT", exception3.getErrorCode());
        assertEquals("Driver ID cannot be null", exception3.getMessage());
        
        // Test invalidateDriverCache
        DriverServiceException exception4 = assertThrows(DriverServiceException.class, () ->
                realDriverCacheService.invalidateDriverCache(null)
        );
        assertEquals("INVALID_INPUT", exception4.getErrorCode());
        assertEquals("Driver ID cannot be null", exception4.getMessage());
        
        // Test isCached
        DriverServiceException exception5 = assertThrows(DriverServiceException.class, () ->
                realDriverCacheService.isCached(null)
        );
        assertEquals("INVALID_INPUT", exception5.getErrorCode());
        assertEquals("Driver ID cannot be null", exception5.getMessage());
    }

    @Test
    @DisplayName("DriverCacheService should throw exception for null status in cacheDriverStatus")
    void shouldThrowExceptionForNullStatusInCacheDriverStatus() {
        UUID driverId = UUID.randomUUID();
        
        DriverServiceException exception = assertThrows(DriverServiceException.class, () ->
                realDriverCacheService.cacheDriverStatus(driverId, null)
        );
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertEquals("Status cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("DriverCacheService should throw exception for null parameters in cacheDriverLocationWithGeohash")
    void shouldThrowExceptionForNullParametersInCacheDriverLocationWithGeohash() {
        UUID driverId = UUID.randomUUID();
        long timestamp = System.currentTimeMillis();
        String geohash = "s61m8";

        // Test null driverId
        DriverServiceException exception1 = assertThrows(DriverServiceException.class, () ->
                realDriverCacheService.cacheDriverLocationWithGeohash(null, 10.7769, 106.7009, "AVAILABLE", timestamp, geohash)
        );
        assertEquals("INVALID_INPUT", exception1.getErrorCode());
        assertEquals("Driver ID cannot be null", exception1.getMessage());

        // Test null latitude
        DriverServiceException exception2 = assertThrows(DriverServiceException.class, () ->
                realDriverCacheService.cacheDriverLocationWithGeohash(driverId, null, 106.7009, "AVAILABLE", timestamp, geohash)
        );
        assertEquals("INVALID_INPUT", exception2.getErrorCode());
        assertEquals("Latitude cannot be null", exception2.getMessage());

        // Test null longitude
        DriverServiceException exception3 = assertThrows(DriverServiceException.class, () ->
                realDriverCacheService.cacheDriverLocationWithGeohash(driverId, 10.7769, null, "AVAILABLE", timestamp, geohash)
        );
        assertEquals("INVALID_INPUT", exception3.getErrorCode());
        assertEquals("Longitude cannot be null", exception3.getMessage());

        // Test null status
        DriverServiceException exception4 = assertThrows(DriverServiceException.class, () ->
                realDriverCacheService.cacheDriverLocationWithGeohash(driverId, 10.7769, 106.7009, null, timestamp, geohash)
        );
        assertEquals("INVALID_INPUT", exception4.getErrorCode());
        assertEquals("Status cannot be null", exception4.getMessage());

        // Test null geohash
        DriverServiceException exception5 = assertThrows(DriverServiceException.class, () ->
                realDriverCacheService.cacheDriverLocationWithGeohash(driverId, 10.7769, 106.7009, "AVAILABLE", timestamp, null)
        );
        assertEquals("INVALID_INPUT", exception5.getErrorCode());
        assertEquals("Geohash cannot be null or empty", exception5.getMessage());

        // Test empty geohash
        DriverServiceException exception6 = assertThrows(DriverServiceException.class, () ->
                realDriverCacheService.cacheDriverLocationWithGeohash(driverId, 10.7769, 106.7009, "AVAILABLE", timestamp, "   ")
        );
        assertEquals("INVALID_INPUT", exception6.getErrorCode());
        assertEquals("Geohash cannot be null or empty", exception6.getMessage());
    }

    @Test
    @DisplayName("RabbitMQPublisher should throw exception for null parameters in publishLocationUpdate")
    void shouldThrowExceptionForNullParametersInPublishLocationUpdate() {
        UUID driverId = UUID.randomUUID();
        
        // Test null driverId
        DriverServiceException exception1 = assertThrows(DriverServiceException.class, () ->
                realRabbitMQPublisher.publishLocationUpdate(null, 10.7769, 106.7009, "AVAILABLE", System.currentTimeMillis())
        );
        assertEquals("INVALID_INPUT", exception1.getErrorCode());
        assertEquals("Driver ID cannot be null", exception1.getMessage());
        
        // Test null latitude
        DriverServiceException exception2 = assertThrows(DriverServiceException.class, () ->
                realRabbitMQPublisher.publishLocationUpdate(driverId, null, 106.7009, "AVAILABLE", System.currentTimeMillis())
        );
        assertEquals("INVALID_INPUT", exception2.getErrorCode());
        assertEquals("Latitude cannot be null", exception2.getMessage());
        
        // Test null longitude
        DriverServiceException exception3 = assertThrows(DriverServiceException.class, () ->
                realRabbitMQPublisher.publishLocationUpdate(driverId, 10.7769, null, "AVAILABLE", System.currentTimeMillis())
        );
        assertEquals("INVALID_INPUT", exception3.getErrorCode());
        assertEquals("Longitude cannot be null", exception3.getMessage());
        
        // Test null status
        DriverServiceException exception4 = assertThrows(DriverServiceException.class, () ->
                realRabbitMQPublisher.publishLocationUpdate(driverId, 10.7769, 106.7009, null, System.currentTimeMillis())
        );
        assertEquals("INVALID_INPUT", exception4.getErrorCode());
        assertEquals("Status cannot be null", exception4.getMessage());
    }

    @Test
    @DisplayName("RabbitMQPublisher should throw exception for null parameters in status change methods")
    void shouldThrowExceptionForNullParametersInStatusChangeMethods() {
        UUID driverId = UUID.randomUUID();
        
        // Test publishStatusChange with null driverId
        DriverServiceException exception1 = assertThrows(DriverServiceException.class, () ->
                realRabbitMQPublisher.publishStatusChange(null, "AVAILABLE", "BUSY", "Trip assigned")
        );
        assertEquals("INVALID_INPUT", exception1.getErrorCode());
        assertEquals("Driver ID cannot be null", exception1.getMessage());
        
        // Test publishStatusChange with null newStatus
        DriverServiceException exception2 = assertThrows(DriverServiceException.class, () ->
                realRabbitMQPublisher.publishStatusChange(driverId, "AVAILABLE", null, "Trip assigned")
        );
        assertEquals("INVALID_INPUT", exception2.getErrorCode());
        assertEquals("New status cannot be null", exception2.getMessage());
        
        // Test publishDriverOnline with null driverId
        DriverServiceException exception3 = assertThrows(DriverServiceException.class, () ->
                realRabbitMQPublisher.publishDriverOnline(null)
        );
        assertEquals("INVALID_INPUT", exception3.getErrorCode());
        assertEquals("Driver ID cannot be null", exception3.getMessage());
        
        // Test publishDriverOffline with null driverId
        DriverServiceException exception4 = assertThrows(DriverServiceException.class, () ->
                realRabbitMQPublisher.publishDriverOffline(null)
        );
        assertEquals("INVALID_INPUT", exception4.getErrorCode());
        assertEquals("Driver ID cannot be null", exception4.getMessage());
    }
}