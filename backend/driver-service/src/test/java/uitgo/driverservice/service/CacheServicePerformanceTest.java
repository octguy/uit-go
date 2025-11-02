package uitgo.driverservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uitgo.driverservice.util.GeohashUtil;

import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Performance tests to verify optimization benefits for DriverCacheService
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
class CacheServicePerformanceTest {

    private DriverCacheService driverCacheService;
    private GeohashUtil geohashUtil;
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        geohashUtil = mock(GeohashUtil.class);
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> mockRedisTemplate = mock(RedisTemplate.class);
        redisTemplate = mockRedisTemplate;
        driverCacheService = new DriverCacheService(redisTemplate, geohashUtil);

        // Mock Redis operations
        when(redisTemplate.opsForValue()).thenReturn(mock());
        when(geohashUtil.encode(anyDouble(), anyDouble())).thenReturn("s61m8");
    }

    @Test
    @DisplayName("Optimized method should avoid duplicate geohash calculations")
    void optimizedMethodShouldAvoidDuplicateGeohashCalculations() {
        UUID driverId = UUID.randomUUID();
        double latitude = 10.7769;
        double longitude = 106.7009;
        String status = "AVAILABLE";
        long timestamp = System.currentTimeMillis();
        String preCalculatedGeohash = "s61m8";

        // Test original method (should calculate geohash once)
        driverCacheService.cacheDriverLocation(driverId, latitude, longitude, status, timestamp);
        
        // Verify geohash was calculated once in original method
        verify(geohashUtil, times(1)).encode(latitude, longitude);

        // Reset mock
        reset(geohashUtil);

        // Test optimized method with pre-calculated geohash (should not calculate geohash)
        driverCacheService.cacheDriverLocationWithGeohash(driverId, latitude, longitude, status, timestamp, preCalculatedGeohash);
        
        // Verify geohash was NOT calculated in optimized method
        verify(geohashUtil, never()).encode(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Multiple calls to optimized method should not calculate geohash")
    void multipleCallsToOptimizedMethodShouldNotCalculateGeohash() {
        UUID driverId1 = UUID.randomUUID();
        UUID driverId2 = UUID.randomUUID();
        double latitude = 10.7769;
        double longitude = 106.7009;
        String status = "AVAILABLE";
        long timestamp = System.currentTimeMillis();
        String preCalculatedGeohash = "s61m8";

        // Make multiple calls to optimized method
        driverCacheService.cacheDriverLocationWithGeohash(driverId1, latitude, longitude, status, timestamp, preCalculatedGeohash);
        driverCacheService.cacheDriverLocationWithGeohash(driverId2, latitude, longitude, status, timestamp, preCalculatedGeohash);
        driverCacheService.cacheDriverLocationWithGeohash(driverId1, latitude + 0.001, longitude + 0.001, status, timestamp, preCalculatedGeohash);
        
        // Verify geohash was never calculated in any of the optimized method calls
        verify(geohashUtil, never()).encode(anyDouble(), anyDouble());
    }
}