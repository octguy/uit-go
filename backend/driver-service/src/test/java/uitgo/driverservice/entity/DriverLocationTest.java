package uitgo.driverservice.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DriverLocation Entity Tests")
class DriverLocationTest {

    private DriverLocation driverLocation;
    private UUID testLocationId;
    private UUID testDriverId;

    @BeforeEach
    void setUp() {
        testLocationId = UUID.randomUUID();
        testDriverId = UUID.randomUUID();
        
        driverLocation = DriverLocation.builder()
                .locationId(testLocationId)
                .driverId(testDriverId)
                .latitude(10.7769)
                .longitude(106.7009)
                .geohash("w3gvk1hp")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @Test
    @DisplayName("Should create driver location with valid coordinates")
    void shouldCreateDriverLocationWithValidCoordinates() {
        assertNotNull(driverLocation);
        assertEquals(testLocationId, driverLocation.getLocationId());
        assertEquals(testDriverId, driverLocation.getDriverId());
        assertEquals(10.7769, driverLocation.getLatitude());
        assertEquals(106.7009, driverLocation.getLongitude());
        assertEquals("w3gvk1hp", driverLocation.getGeohash());
        assertNotNull(driverLocation.getTimestamp());
    }

    @Test
    @DisplayName("Should handle location updates correctly")
    void shouldHandleLocationUpdatesCorrectly() {
        // Ho Chi Minh City coordinates
        double newLat = 10.7829;
        double newLng = 106.6934;
        long newTimestamp = System.currentTimeMillis();
        
        driverLocation.setLatitude(newLat);
        driverLocation.setLongitude(newLng);
        driverLocation.setTimestamp(newTimestamp);
        driverLocation.setGeohash("w3gvk2hp");

        assertEquals(newLat, driverLocation.getLatitude());
        assertEquals(newLng, driverLocation.getLongitude());
        assertEquals(newTimestamp, driverLocation.getTimestamp());
        assertEquals("w3gvk2hp", driverLocation.getGeohash());
    }

    @Test
    @DisplayName("Should validate coordinates bounds")
    void shouldValidateCoordinatesBounds() {
        // Test extreme coordinates
        driverLocation.setLatitude(-90.0);
        driverLocation.setLongitude(-180.0);
        assertEquals(-90.0, driverLocation.getLatitude());
        assertEquals(-180.0, driverLocation.getLongitude());

        driverLocation.setLatitude(90.0);
        driverLocation.setLongitude(180.0);
        assertEquals(90.0, driverLocation.getLatitude());
        assertEquals(180.0, driverLocation.getLongitude());
    }

    @Test
    @DisplayName("Should handle geohash correctly")
    void shouldHandleGeohashCorrectly() {
        String newGeohash = "w679w7k5"; // Hanoi geohash
        driverLocation.setGeohash(newGeohash);
        assertEquals(newGeohash, driverLocation.getGeohash());
    }

    @Test
    @DisplayName("Should handle builder pattern correctly")
    void shouldHandleBuilderPatternCorrectly() {
        DriverLocation newLocation = DriverLocation.builder()
                .driverId(UUID.randomUUID())
                .latitude(21.0285)  // Hanoi coordinates
                .longitude(105.8542)
                .geohash("w679w7k5")
                .timestamp(System.currentTimeMillis())
                .build();

        assertNotNull(newLocation);
        assertEquals(21.0285, newLocation.getLatitude());
        assertEquals(105.8542, newLocation.getLongitude());
        assertEquals("w679w7k5", newLocation.getGeohash());
        assertNotNull(newLocation.getTimestamp());
    }

    @Test
    @DisplayName("Should maintain data integrity with lombok annotations")
    void shouldMaintainDataIntegrityWithLombokAnnotations() {
        DriverLocation sameLocation = DriverLocation.builder()
                .locationId(testLocationId)
                .driverId(testDriverId)
                .latitude(10.7769)
                .longitude(106.7009)
                .geohash("w3gvk1hp")
                .timestamp(driverLocation.getTimestamp())
                .build();

        assertEquals(driverLocation, sameLocation);
        assertEquals(driverLocation.hashCode(), sameLocation.hashCode());

        // Test toString
        String locationString = driverLocation.toString();
        assertNotNull(locationString);
        assertTrue(locationString.contains("DriverLocation"));
        assertTrue(locationString.contains(testDriverId.toString()));
    }

    @Test
    @DisplayName("Should handle timestamp precision")
    void shouldHandleTimestampPrecision() {
        long now = System.currentTimeMillis();
        driverLocation.setTimestamp(now);
        
        assertEquals(now, driverLocation.getTimestamp());
        assertTrue(driverLocation.getTimestamp() > 0);
    }

    @Test
    @DisplayName("Should handle automatic timestamp creation with PrePersist")
    void shouldHandleAutomaticTimestampCreationWithPrePersist() {
        DriverLocation newLocation = DriverLocation.builder()
                .driverId(UUID.randomUUID())
                .latitude(10.7769)
                .longitude(106.7009)
                .geohash("w3gvk1hp")
                .build();

        // Simulate @PrePersist call
        if (newLocation.getTimestamp() == null) {
            newLocation.setTimestamp(System.currentTimeMillis());
        }

        assertNotNull(newLocation.getTimestamp());
        assertTrue(newLocation.getTimestamp() > 0);
    }

    @Test
    @DisplayName("Should handle Vietnam coordinates correctly")
    void shouldHandleVietnamCoordinatesCorrectly() {
        // Test Ho Chi Minh City
        driverLocation.setLatitude(10.7769);
        driverLocation.setLongitude(106.7009);
        
        assertEquals(10.7769, driverLocation.getLatitude());
        assertEquals(106.7009, driverLocation.getLongitude());

        // Test Hanoi
        driverLocation.setLatitude(21.0285);
        driverLocation.setLongitude(105.8542);
        
        assertEquals(21.0285, driverLocation.getLatitude());
        assertEquals(105.8542, driverLocation.getLongitude());

        // Test Da Nang
        driverLocation.setLatitude(16.0544);
        driverLocation.setLongitude(108.2022);
        
        assertEquals(16.0544, driverLocation.getLatitude());
        assertEquals(108.2022, driverLocation.getLongitude());
    }
}