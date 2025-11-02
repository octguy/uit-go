package uitgo.driverservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uitgo.driverservice.dto.DriverLocationDTO;
import uitgo.driverservice.service.DriverLocationService;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/drivers")
public class DriverController {

    private final DriverLocationService driverLocationService;

    @Autowired
    public DriverController(DriverLocationService driverLocationService) {
        this.driverLocationService = driverLocationService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "driver-service");
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Driver Service is running with REST API");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/nearby")
    public ResponseEntity<Map<String, Object>> getNearbyDrivers(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5.0") double radiusKm,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Finding nearby drivers for location: {}, {} within {} km", 
                latitude, longitude, radiusKm);
        
        try {
            List<DriverLocationDTO> nearbyDrivers = driverLocationService.findNearbyDrivers(
                latitude, longitude, radiusKm, limit);

            List<Map<String, Object>> driverData = nearbyDrivers.stream()
                .map(this::convertToDriverResponse)
                .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("drivers", driverData);
            response.put("count", driverData.size());
            response.put("searchRadius", radiusKm);
            response.put("searchLocation", Map.of("latitude", latitude, "longitude", longitude));
            response.put("success", true);
            response.put("message", "Nearby drivers found");
            response.put("optimized", true); // Indicates use of optimized spatial queries
            
            log.info("Found {} nearby drivers within {} km", driverData.size(), radiusKm);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error finding nearby drivers: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("drivers", new ArrayList<>());
            errorResponse.put("count", 0);
            errorResponse.put("success", false);
            errorResponse.put("message", "Error finding nearby drivers: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    private Map<String, Object> convertToDriverResponse(DriverLocationDTO driver) {
        Map<String, Object> driverMap = new HashMap<>();
        driverMap.put("driverId", driver.getDriverId().toString());
        driverMap.put("latitude", driver.getLatitude());
        driverMap.put("longitude", driver.getLongitude());
        driverMap.put("distance", Math.round(driver.getDistance() * 100.0) / 100.0); // Round to 2 decimals
        driverMap.put("timestamp", driver.getTimestamp());
        driverMap.put("geohash", driver.getGeohash());
        
        // Mock additional driver details that would come from driver profile service
        driverMap.put("rating", 4.0 + Math.random());
        driverMap.put("vehicleModel", "Toyota Vios");
        driverMap.put("vehiclePlate", "51A-" + String.format("%04d", driver.getDriverId().hashCode() % 10000));
        driverMap.put("status", "AVAILABLE");
        
        return driverMap;
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<Map<String, Object>> getDriver(@PathVariable String driverId) {
        log.info("Getting driver: {}", driverId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("driverId", driverId);
        response.put("userId", "user-" + UUID.randomUUID().toString());
        response.put("licenseNumber", "LICENSE-123456");
        response.put("vehicleModel", "Toyota Vios");
        response.put("vehiclePlate", "51A-12345");
        response.put("rating", 4.5);
        response.put("totalTrips", 150);
        response.put("status", "AVAILABLE");
        response.put("success", true);
        response.put("message", "Driver retrieved successfully (Mock)");
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{driverId}/location")
    public ResponseEntity<Map<String, Object>> updateDriverLocation(
            @PathVariable String driverId,
            @RequestBody Map<String, Object> request) {
        
        log.info("Updating location for driver: {}", driverId);
        
        try {
            UUID driverUuid = UUID.fromString(driverId);
            Double latitude = ((Number) request.get("latitude")).doubleValue();
            Double longitude = ((Number) request.get("longitude")).doubleValue();
            
            DriverLocationDTO updatedLocation = driverLocationService.updateDriverLocation(
                driverUuid, latitude, longitude);
            
            Map<String, Object> response = new HashMap<>();
            response.put("driverId", driverId);
            response.put("latitude", updatedLocation.getLatitude());
            response.put("longitude", updatedLocation.getLongitude());
            response.put("timestamp", updatedLocation.getTimestamp());
            response.put("geohash", updatedLocation.getGeohash());
            response.put("success", true);
            response.put("message", "Driver location updated successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error updating driver location: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("driverId", driverId);
            errorResponse.put("success", false);
            errorResponse.put("message", "Error updating location: " + e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        }
    }

    @PutMapping("/{driverId}/status")
    public ResponseEntity<Map<String, Object>> updateDriverStatus(
            @PathVariable String driverId,
            @RequestBody Map<String, Object> request) {
        
        log.info("Updating status for driver: {} to {}", driverId, request.get("status"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("driverId", driverId);
        response.put("status", request.get("status"));
        response.put("timestamp", System.currentTimeMillis());
        response.put("success", true);
        response.put("message", "Driver status updated successfully (Mock)");
        
        return ResponseEntity.ok(response);
    }
}