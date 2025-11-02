package uitgo.driverservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uitgo.driverservice.dto.DriverLocationDTO;
import uitgo.driverservice.entity.Driver;
import uitgo.driverservice.repository.DriverRepository;
import uitgo.driverservice.service.DriverLocationService;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverLocationService driverLocationService;
    private final DriverRepository driverRepository;

    @Autowired
    public DriverController(DriverLocationService driverLocationService, DriverRepository driverRepository) {
        this.driverLocationService = driverLocationService;
        this.driverRepository = driverRepository;
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

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerDriver(@RequestBody Map<String, Object> request) {
        log.info("Registering new driver: {}", request.get("email"));
        
        try {
            // Check if driver already exists by license number
            String licenseNumber = request.get("license_number") != null ? 
                request.get("license_number").toString() : null;
            
            if (licenseNumber != null && driverRepository.findByLicenseNumber(licenseNumber).isPresent()) {
                log.warn("Driver with license number {} already exists", licenseNumber);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Driver with this license number already exists");
                errorResponse.put("code", "DUPLICATE_LICENSE");
                return ResponseEntity.status(409).body(errorResponse);
            }
            
            // Check if vehicle plate already exists
            String vehiclePlate = request.get("vehicle_plate") != null ? 
                request.get("vehicle_plate").toString() : null;
            
            if (vehiclePlate != null && driverRepository.findByVehiclePlate(vehiclePlate).isPresent()) {
                log.warn("Vehicle with plate {} already exists", vehiclePlate);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Vehicle with this plate number already exists");
                errorResponse.put("code", "DUPLICATE_VEHICLE_PLATE");
                return ResponseEntity.status(409).body(errorResponse);
            }
            
            // Create a new driver entity
            Driver driver = new Driver();
            
            // Set required fields from request
            if (request.get("userId") != null) {
                driver.setUserId(UUID.fromString(request.get("userId").toString()));
            } else {
                // Generate a new user ID if not provided
                driver.setUserId(UUID.randomUUID());
            }
            
            driver.setLicenseNumber(licenseNumber);
            driver.setVehicleModel(request.get("vehicle_type") != null ? 
                request.get("vehicle_type").toString() : null);
            driver.setVehiclePlate(request.get("vehicle_plate") != null ? 
                request.get("vehicle_plate").toString() : null);
            
            // Set default values
            driver.setRating(0.0);
            driver.setTotalCompletedTrips(0);
            driver.setStatus(Driver.DriverStatus.OFFLINE);
            
            // Save to database
            Driver savedDriver = driverRepository.save(driver);
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("driverId", savedDriver.getDriverId().toString());
            response.put("userId", savedDriver.getUserId().toString());
            response.put("email", request.get("email"));
            response.put("phone", request.get("phone"));
            response.put("name", request.get("name"));
            response.put("licenseNumber", savedDriver.getLicenseNumber());
            response.put("vehicleType", savedDriver.getVehicleModel());
            response.put("vehiclePlate", savedDriver.getVehiclePlate());
            response.put("status", savedDriver.getStatus().toString());
            response.put("rating", savedDriver.getRating());
            response.put("registrationDate", savedDriver.getCreatedAt());
            response.put("success", true);
            response.put("message", "Driver registered successfully in database");
            
            log.info("Driver registered successfully with ID: {}", savedDriver.getDriverId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error registering driver: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error registering driver: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
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
        
        // Handle distance - ensure it's always present, even if 0.0
        Double distance = driver.getDistance();
        if (distance != null) {
            double roundedDistance = Math.round(distance * 100.0) / 100.0; // Round to 2 decimals
            driverMap.put("distance", roundedDistance);
        } else {
            driverMap.put("distance", 0.0); // Default to 0.0 if null
        }
        
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
        
        try {
            UUID driverUuid = UUID.fromString(driverId);
            Optional<Driver> driverOpt = driverRepository.findByDriverId(driverUuid);
            
            if (driverOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Driver not found with id: " + driverId);
                return ResponseEntity.status(404).body(errorResponse);
            }
            
            Driver driver = driverOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("driverId", driver.getDriverId().toString());
            response.put("userId", driver.getUserId().toString());
            response.put("licenseNumber", driver.getLicenseNumber());
            response.put("vehicleModel", driver.getVehicleModel());
            response.put("vehiclePlate", driver.getVehiclePlate());
            response.put("rating", driver.getRating());
            response.put("totalTrips", driver.getTotalCompletedTrips());
            response.put("status", driver.getStatus().toString());
            response.put("success", true);
            response.put("message", "Driver retrieved successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid driver ID format: {}", driverId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid driver ID format");
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            log.error("Error retrieving driver: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error retrieving driver: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
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
            
            // Check if driver exists in database first
            Optional<Driver> driverOpt = driverRepository.findByDriverId(driverUuid);
            if (driverOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("driverId", driverId);
                errorResponse.put("success", false);
                errorResponse.put("message", "Driver not found with id: " + driverId);
                return ResponseEntity.status(404).body(errorResponse);
            }
            
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
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for driver location update: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("driverId", driverId);
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid input: " + e.getMessage());
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            log.error("Error updating driver location: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("driverId", driverId);
            errorResponse.put("success", false);
            errorResponse.put("message", "Error updating location: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PutMapping("/{driverId}/status")
    public ResponseEntity<Map<String, Object>> updateDriverStatus(
            @PathVariable String driverId,
            @RequestBody Map<String, Object> request) {
        
        log.info("Updating status for driver: {} to {}", driverId, request.get("status"));
        
        try {
            UUID driverUuid = UUID.fromString(driverId);
            Optional<Driver> driverOpt = driverRepository.findByDriverId(driverUuid);
            
            if (driverOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("driverId", driverId);
                errorResponse.put("success", false);
                errorResponse.put("message", "Driver not found with id: " + driverId);
                return ResponseEntity.status(404).body(errorResponse);
            }
            
            Driver driver = driverOpt.get();
            String statusStr = request.get("status").toString();
            
            try {
                Driver.DriverStatus newStatus = Driver.DriverStatus.valueOf(statusStr);
                driver.setStatus(newStatus);
                driverRepository.save(driver);
                
                Map<String, Object> response = new HashMap<>();
                response.put("driverId", driverId);
                response.put("status", newStatus.toString());
                response.put("timestamp", System.currentTimeMillis());
                response.put("success", true);
                response.put("message", "Driver status updated successfully");
                
                return ResponseEntity.ok(response);
                
            } catch (IllegalArgumentException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("driverId", driverId);
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid status value: " + statusStr + ". Valid values: AVAILABLE, BUSY, OFFLINE, ON_BREAK");
                return ResponseEntity.status(400).body(errorResponse);
            }
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid driver ID format: {}", driverId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("driverId", driverId);
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid driver ID format");
            return ResponseEntity.status(400).body(errorResponse);
        } catch (Exception e) {
            log.error("Error updating driver status: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("driverId", driverId);
            errorResponse.put("success", false);
            errorResponse.put("message", "Error updating driver status: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}