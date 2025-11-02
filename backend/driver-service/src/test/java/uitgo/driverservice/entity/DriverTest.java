package uitgo.driverservice.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import uitgo.driverservice.entity.Driver.DriverStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Driver Entity Tests")
class DriverTest {

    private Driver driver;
    private UUID testDriverId;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testDriverId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        
        driver = Driver.builder()
                .driverId(testDriverId)
                .userId(testUserId)
                .licenseNumber("LIC123456789")
                .vehicleModel("Toyota Camry 2023")
                .vehiclePlate("29A-12345")
                .rating(4.8)
                .totalCompletedTrips(150)
                .status(DriverStatus.AVAILABLE)
                .vehicleCapacity(4)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();
    }

    @Test
    @DisplayName("Should create driver with valid data")
    void shouldCreateDriverWithValidData() {
        assertNotNull(driver);
        assertEquals(testDriverId, driver.getDriverId());
        assertEquals(testUserId, driver.getUserId());
        assertEquals("LIC123456789", driver.getLicenseNumber());
        assertEquals("Toyota Camry 2023", driver.getVehicleModel());
        assertEquals("29A-12345", driver.getVehiclePlate());
        assertEquals(4.8, driver.getRating());
        assertEquals(150, driver.getTotalCompletedTrips());
        assertEquals(DriverStatus.AVAILABLE, driver.getStatus());
        assertEquals(4, driver.getVehicleCapacity());
        assertNotNull(driver.getCreatedAt());
        assertNotNull(driver.getUpdatedAt());
    }

    @Test
    @DisplayName("Should handle builder pattern correctly")
    void shouldHandleBuilderPatternCorrectly() {
        Driver newDriver = Driver.builder()
                .userId(UUID.randomUUID())
                .licenseNumber("NEW123")
                .vehicleModel("Honda Civic")
                .vehiclePlate("30B-67890")
                .rating(5.0)
                .totalCompletedTrips(0)
                .status(DriverStatus.AVAILABLE)
                .vehicleCapacity(4)
                .build();

        assertNotNull(newDriver);
        assertEquals("NEW123", newDriver.getLicenseNumber());
        assertEquals("Honda Civic", newDriver.getVehicleModel());
        assertEquals(5.0, newDriver.getRating());
        assertEquals(0, newDriver.getTotalCompletedTrips());
    }

    @Test
    @DisplayName("Should handle rating bounds correctly")
    void shouldHandleRatingBoundsCorrectly() {
        // Test minimum rating
        driver.setRating(1.0);
        assertEquals(1.0, driver.getRating());

        // Test maximum rating
        driver.setRating(5.0);
        assertEquals(5.0, driver.getRating());

        // Test decimal rating
        driver.setRating(4.75);
        assertEquals(4.75, driver.getRating());
    }

    @Test
    @DisplayName("Should handle status changes correctly")
    void shouldHandleStatusChangesCorrectly() {
        driver.setStatus(DriverStatus.BUSY);
        assertEquals(DriverStatus.BUSY, driver.getStatus());

        driver.setStatus(DriverStatus.OFFLINE);
        assertEquals(DriverStatus.OFFLINE, driver.getStatus());

        driver.setStatus(DriverStatus.AVAILABLE);
        assertEquals(DriverStatus.AVAILABLE, driver.getStatus());
    }

    @Test
    @DisplayName("Should update trip count correctly")
    void shouldUpdateTripCountCorrectly() {
        int initialCount = driver.getTotalCompletedTrips();
        driver.setTotalCompletedTrips(initialCount + 1);
        
        assertEquals(initialCount + 1, driver.getTotalCompletedTrips());
    }

    @Test
    @DisplayName("Should handle vehicle information updates")
    void shouldHandleVehicleInformationUpdates() {
        driver.setVehicleModel("BMW X5 2024");
        driver.setVehiclePlate("HCM-999");
        driver.setVehicleCapacity(7);

        assertEquals("BMW X5 2024", driver.getVehicleModel());
        assertEquals("HCM-999", driver.getVehiclePlate());
        assertEquals(7, driver.getVehicleCapacity());
    }

    @Test
    @DisplayName("Should handle timestamp updates")
    void shouldHandleTimestampUpdates() {
        long newTimestamp = System.currentTimeMillis() + 1000;
        driver.setUpdatedAt(newTimestamp);
        
        assertEquals(newTimestamp, driver.getUpdatedAt());
        assertNotEquals(driver.getCreatedAt(), driver.getUpdatedAt());
    }

    @Test
    @DisplayName("Should maintain data integrity with lombok annotations")
    void shouldMaintainDataIntegrityWithLombokAnnotations() {
        // Test equals and hashCode (from @Data)
        Driver sameDriver = Driver.builder()
                .driverId(testDriverId)
                .userId(testUserId)
                .licenseNumber("LIC123456789")
                .vehicleModel("Toyota Camry 2023")
                .vehiclePlate("29A-12345")
                .rating(4.8)
                .totalCompletedTrips(150)
                .status(DriverStatus.AVAILABLE)
                .vehicleCapacity(4)
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build();

        assertEquals(driver, sameDriver);
        assertEquals(driver.hashCode(), sameDriver.hashCode());

        // Test toString (from @Data)
        String driverString = driver.toString();
        assertNotNull(driverString);
        assertTrue(driverString.contains("Driver"));
        assertTrue(driverString.contains(testDriverId.toString()));
    }
}