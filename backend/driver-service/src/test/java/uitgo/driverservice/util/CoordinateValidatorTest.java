package uitgo.driverservice.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uitgo.driverservice.exception.DriverServiceException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Coordinate Validator Tests")
class CoordinateValidatorTest {

    private CoordinateValidator coordinateValidator;

    @BeforeEach
    void setUp() {
        coordinateValidator = new CoordinateValidator();
    }

    @Test
    @DisplayName("Should validate valid coordinates successfully")
    void shouldValidateValidCoordinates() {
        assertDoesNotThrow(() -> coordinateValidator.validateCoordinates(10.7769, 106.7009)); // Ho Chi Minh City
        assertDoesNotThrow(() -> coordinateValidator.validateCoordinates(40.7128, -74.0060)); // New York
        assertDoesNotThrow(() -> coordinateValidator.validateCoordinates(-33.8688, 151.2093)); // Sydney
        assertDoesNotThrow(() -> coordinateValidator.validateCoordinates(0.0, 0.0)); // Equator, Prime Meridian
        assertDoesNotThrow(() -> coordinateValidator.validateCoordinates(90.0, 180.0)); // Max values
        assertDoesNotThrow(() -> coordinateValidator.validateCoordinates(-90.0, -180.0)); // Min values
    }

    @Test
    @DisplayName("Should validate valid Double coordinates successfully")
    void shouldValidateValidDoubleCoordinates() {
        assertDoesNotThrow(() -> coordinateValidator.validateCoordinates(Double.valueOf(10.7769), Double.valueOf(106.7009)));
        assertDoesNotThrow(() -> coordinateValidator.validateCoordinates(Double.valueOf(0.0), Double.valueOf(0.0)));
    }

    @Test
    @DisplayName("Should return true for valid coordinates when checking")
    void shouldReturnTrueForValidCoordinates() {
        assertTrue(coordinateValidator.isValidCoordinate(10.7769, 106.7009));
        assertTrue(coordinateValidator.isValidCoordinate(40.7128, -74.0060));
        assertTrue(coordinateValidator.isValidCoordinate(Double.valueOf(0.0), Double.valueOf(0.0)));
        assertTrue(coordinateValidator.isValidCoordinate(90.0, 180.0));
        assertTrue(coordinateValidator.isValidCoordinate(-90.0, -180.0));
    }

    @Test
    @DisplayName("Should throw exception for null latitude")
    void shouldThrowExceptionForNullLatitude() {
        DriverServiceException exception = assertThrows(DriverServiceException.class,
                () -> coordinateValidator.validateCoordinates((Double) null, Double.valueOf(106.7009)));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertEquals("Latitude cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for null longitude")
    void shouldThrowExceptionForNullLongitude() {
        DriverServiceException exception = assertThrows(DriverServiceException.class,
                () -> coordinateValidator.validateCoordinates(Double.valueOf(10.7769), (Double) null));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertEquals("Longitude cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for NaN coordinates")
    void shouldThrowExceptionForNaNCoordinates() {
        DriverServiceException exception = assertThrows(DriverServiceException.class,
                () -> coordinateValidator.validateCoordinates(Double.NaN, 106.7009));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertEquals("Coordinates cannot be NaN", exception.getMessage());

        exception = assertThrows(DriverServiceException.class,
                () -> coordinateValidator.validateCoordinates(10.7769, Double.NaN));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertEquals("Coordinates cannot be NaN", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for infinite coordinates")
    void shouldThrowExceptionForInfiniteCoordinates() {
        DriverServiceException exception = assertThrows(DriverServiceException.class,
                () -> coordinateValidator.validateCoordinates(Double.POSITIVE_INFINITY, 106.7009));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertEquals("Coordinates cannot be infinite", exception.getMessage());

        exception = assertThrows(DriverServiceException.class,
                () -> coordinateValidator.validateCoordinates(10.7769, Double.NEGATIVE_INFINITY));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertEquals("Coordinates cannot be infinite", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for latitude out of range")
    void shouldThrowExceptionForLatitudeOutOfRange() {
        DriverServiceException exception = assertThrows(DriverServiceException.class,
                () -> coordinateValidator.validateCoordinates(91.0, 106.7009));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Latitude must be between -90 and 90"));

        exception = assertThrows(DriverServiceException.class,
                () -> coordinateValidator.validateCoordinates(-91.0, 106.7009));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Latitude must be between -90 and 90"));
    }

    @Test
    @DisplayName("Should throw exception for longitude out of range")
    void shouldThrowExceptionForLongitudeOutOfRange() {
        DriverServiceException exception = assertThrows(DriverServiceException.class,
                () -> coordinateValidator.validateCoordinates(10.7769, 181.0));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Longitude must be between -180 and 180"));

        exception = assertThrows(DriverServiceException.class,
                () -> coordinateValidator.validateCoordinates(10.7769, -181.0));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Longitude must be between -180 and 180"));
    }

    @Test
    @DisplayName("Should return false for invalid coordinates when checking")
    void shouldReturnFalseForInvalidCoordinates() {
        assertFalse(coordinateValidator.isValidCoordinate((Double) null, Double.valueOf(106.7009)));
        assertFalse(coordinateValidator.isValidCoordinate(Double.valueOf(10.7769), (Double) null));
        assertFalse(coordinateValidator.isValidCoordinate(Double.NaN, 106.7009));
        assertFalse(coordinateValidator.isValidCoordinate(10.7769, Double.NaN));
        assertFalse(coordinateValidator.isValidCoordinate(Double.POSITIVE_INFINITY, 106.7009));
        assertFalse(coordinateValidator.isValidCoordinate(10.7769, Double.NEGATIVE_INFINITY));
        assertFalse(coordinateValidator.isValidCoordinate(91.0, 106.7009));
        assertFalse(coordinateValidator.isValidCoordinate(10.7769, 181.0));
        assertFalse(coordinateValidator.isValidCoordinate(-91.0, 106.7009));
        assertFalse(coordinateValidator.isValidCoordinate(10.7769, -181.0));
    }

    @Test
    @DisplayName("Should validate individual latitude values")
    void shouldValidateIndividualLatitude() {
        assertDoesNotThrow(() -> coordinateValidator.validateLatitude(45.0));
        assertDoesNotThrow(() -> coordinateValidator.validateLatitude(Double.valueOf(0.0)));
        assertDoesNotThrow(() -> coordinateValidator.validateLatitude(90.0));
        assertDoesNotThrow(() -> coordinateValidator.validateLatitude(-90.0));

        // Test null
        DriverServiceException exception = assertThrows(DriverServiceException.class,
                () -> coordinateValidator.validateLatitude((Double) null));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertEquals("Latitude cannot be null", exception.getMessage());

        // Test out of range
        exception = assertThrows(DriverServiceException.class,
                () -> coordinateValidator.validateLatitude(91.0));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Latitude must be between -90 and 90"));
    }

    @Test
    @DisplayName("Should validate individual longitude values")
    void shouldValidateIndividualLongitude() {
        assertDoesNotThrow(() -> coordinateValidator.validateLongitude(45.0));
        assertDoesNotThrow(() -> coordinateValidator.validateLongitude(Double.valueOf(0.0)));
        assertDoesNotThrow(() -> coordinateValidator.validateLongitude(180.0));
        assertDoesNotThrow(() -> coordinateValidator.validateLongitude(-180.0));

        // Test null
        DriverServiceException exception = assertThrows(DriverServiceException.class,
                () -> coordinateValidator.validateLongitude((Double) null));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertEquals("Longitude cannot be null", exception.getMessage());

        // Test out of range
        exception = assertThrows(DriverServiceException.class,
                () -> coordinateValidator.validateLongitude(181.0));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Longitude must be between -180 and 180"));
    }

    @Test
    @DisplayName("Should handle edge cases properly")
    void shouldHandleEdgeCases() {
        // Test boundary values
        assertDoesNotThrow(() -> coordinateValidator.validateCoordinates(89.999999, 179.999999));
        assertDoesNotThrow(() -> coordinateValidator.validateCoordinates(-89.999999, -179.999999));
        
        // Test very small values
        assertDoesNotThrow(() -> coordinateValidator.validateCoordinates(0.000001, 0.000001));
        assertDoesNotThrow(() -> coordinateValidator.validateCoordinates(-0.000001, -0.000001));
    }
}