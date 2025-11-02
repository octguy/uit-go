package uitgo.driverservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import uitgo.driverservice.entity.Driver;
import uitgo.driverservice.repository.DriverRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class DriverUpdateService {

    private final DriverRepository driverRepository;
    private final RestTemplate restTemplate;

    @Value("${user.grpc.service.url:http://user-service-grpc:50051}")
    private String userGrpcServiceUrl;

    @Autowired
    public DriverUpdateService(DriverRepository driverRepository, RestTemplate restTemplate) {
        this.driverRepository = driverRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public Map<String, Object> updateDriver(String driverId, Map<String, Object> request) {
        System.out.println("🚀 Update Driver Request - Validating driver: " + driverId);

        try {
            UUID driverUuid = UUID.fromString(driverId);
            Optional<Driver> driverOpt = driverRepository.findByDriverId(driverUuid);

            if (driverOpt.isEmpty()) {
                throw new RuntimeException("Driver not found with id: " + driverId);
            }

            Driver driver = driverOpt.get();

            // Step 1: Validate user via gRPC call to User Service
            try {
                Map<String, Object> grpcRequest = new HashMap<>();
                grpcRequest.put("user_id", driver.getUserId().toString());

                Map<String, Object> grpcResponse = validateUserViaGrpc(grpcRequest);

                System.out.println("📋 gRPC Response Details:");
                System.out.println("   Valid: " + grpcResponse.get("valid"));
                System.out.println("   User ID: " + grpcResponse.get("user_id"));
                System.out.println("   Success: " + grpcResponse.get("success"));
                System.out.println("   Status: " + grpcResponse.get("status"));
                System.out.println("   Message: " + grpcResponse.get("message"));

                // Check both valid and success flags for comprehensive validation
                boolean valid = Boolean.TRUE.equals(grpcResponse.get("valid"));
                boolean success = Boolean.TRUE.equals(grpcResponse.get("success"));

                if (!valid || !success) {
                    String errorMessage = grpcResponse.get("message") != null ?
                        grpcResponse.get("message").toString() :
                        "User validation failed - user not found or inactive";
                    throw new RuntimeException("❌ User validation failed: " + errorMessage);
                }

                System.out.println("✅ User validated via gRPC: " + driver.getUserId());
                System.out.println("   Status: " + grpcResponse.get("status"));

            } catch (Exception e) {
                log.error("❌ gRPC call failed: {}", e.getMessage());
                throw new RuntimeException("❌ User validation failed: " + e.getMessage());
            }

            // Step 2: Update driver fields if validation passed
            if (request.get("licenseNumber") != null) {
                String newLicense = request.get("licenseNumber").toString();
                validateLicenseNumber(newLicense, driverUuid);
                driver.setLicenseNumber(newLicense);
            }

            if (request.get("vehicleModel") != null) {
                driver.setVehicleModel(request.get("vehicleModel").toString());
            }

            if (request.get("vehiclePlate") != null) {
                String newPlate = request.get("vehiclePlate").toString();
                validateVehiclePlate(newPlate, driverUuid);
                driver.setVehiclePlate(newPlate);
            }

            if (request.get("rating") != null) {
                try {
                    Double rating = Double.parseDouble(request.get("rating").toString());
                    if (rating >= 0.0 && rating <= 5.0) {
                        driver.setRating(rating);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid rating format: {}", request.get("rating"));
                }
            }

            if (request.get("status") != null) {
                try {
                    Driver.DriverStatus newStatus = Driver.DriverStatus.valueOf(request.get("status").toString());
                    driver.setStatus(newStatus);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid status value: " + request.get("status"));
                }
            }

            // Step 3: Save updated driver
            Driver updatedDriver = driverRepository.save(driver);

            System.out.println("✅ Driver updated successfully: " + updatedDriver.getDriverId());

            // Step 4: Create response
            return createSuccessResponse(updatedDriver);

        } catch (IllegalArgumentException e) {
            log.error("Invalid driver ID format: {}", driverId);
            throw new RuntimeException("Invalid driver ID format");
        } catch (Exception e) {
            log.error("Error updating driver: ", e);
            throw e;
        }
    }

    public Map<String, Object> validateDriver(String driverId) {
        System.out.println("🚀 Validate Driver Request - Validating driver: " + driverId);

        try {
            UUID driverUuid = UUID.fromString(driverId);
            Optional<Driver> driverOpt = driverRepository.findByDriverId(driverUuid);

            if (driverOpt.isEmpty()) {
                return createValidationResponse(false, driverId, null, "NOT_FOUND", "Driver not found");
            }

            Driver driver = driverOpt.get();

            // Step 1: Validate associated user via gRPC call to User Service
            try {
                Map<String, Object> grpcRequest = new HashMap<>();
                grpcRequest.put("user_id", driver.getUserId().toString());

                Map<String, Object> grpcResponse = validateUserViaGrpc(grpcRequest);

                System.out.println("📋 gRPC Response Details:");
                System.out.println("   Valid: " + grpcResponse.get("valid"));
                System.out.println("   User ID: " + grpcResponse.get("user_id"));
                System.out.println("   Success: " + grpcResponse.get("success"));
                System.out.println("   Status: " + grpcResponse.get("status"));
                System.out.println("   Message: " + grpcResponse.get("message"));

                boolean userValid = Boolean.TRUE.equals(grpcResponse.get("valid"));
                boolean userSuccess = Boolean.TRUE.equals(grpcResponse.get("success"));
                boolean driverValid = driver.getStatus() != Driver.DriverStatus.OFFLINE;
                boolean overallValid = userValid && userSuccess && driverValid;

                System.out.println("✅ Driver validation completed");
                System.out.println("   User Valid: " + userValid);
                System.out.println("   Driver Active: " + driverValid);
                System.out.println("   Overall Valid: " + overallValid);

                return createDetailedValidationResponse(overallValid, driverId, driver, userValid, driverValid, grpcResponse);

            } catch (Exception e) {
                log.error("❌ gRPC call failed: {}", e.getMessage());
                return createValidationResponse(false, driverId, driver.getUserId().toString(), 
                    "VALIDATION_ERROR", "❌ User validation failed: " + e.getMessage());
            }

        } catch (IllegalArgumentException e) {
            log.error("Invalid driver ID format: {}", driverId);
            return createValidationResponse(false, driverId, null, "INVALID_FORMAT", "Invalid driver ID format");
        } catch (Exception e) {
            log.error("Error validating driver: ", e);
            return createValidationResponse(false, driverId, null, "ERROR", "Error validating driver: " + e.getMessage());
        }
    }

    private Map<String, Object> validateUserViaGrpc(Map<String, Object> grpcRequest) {
        System.out.println("📞 Making gRPC call to User Service for user validation");
        System.out.println("🔗 Target: " + userGrpcServiceUrl);
        System.out.println("📝 Request: " + grpcRequest);

        try {
            String grpcUrl = userGrpcServiceUrl + "/validateUser";
            ResponseEntity<Map> response = restTemplate.postForEntity(grpcUrl, grpcRequest, Map.class);

            Map<String, Object> responseBody = response.getBody();
            System.out.println("✅ gRPC response received from User Service");
            System.out.println("   Response details: " + responseBody);

            return responseBody != null ? responseBody : new HashMap<>();

        } catch (Exception e) {
            System.err.println("❌ gRPC call failed: " + e.getMessage());
            log.error("gRPC call failed", e);
            throw new RuntimeException("gRPC call failed: " + e.getMessage());
        }
    }

    private void validateLicenseNumber(String licenseNumber, UUID currentDriverId) {
        Optional<Driver> existingDriver = driverRepository.findByLicenseNumber(licenseNumber);
        if (existingDriver.isPresent() && !existingDriver.get().getDriverId().equals(currentDriverId)) {
            throw new RuntimeException("License number already exists");
        }
    }

    private void validateVehiclePlate(String vehiclePlate, UUID currentDriverId) {
        Optional<Driver> existingDriver = driverRepository.findByVehiclePlate(vehiclePlate);
        if (existingDriver.isPresent() && !existingDriver.get().getDriverId().equals(currentDriverId)) {
            throw new RuntimeException("Vehicle plate already exists");
        }
    }

    private Map<String, Object> createSuccessResponse(Driver driver) {
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
        response.put("message", "Driver updated successfully with gRPC validation");
        response.put("userValidated", true);
        response.put("updatedAt", System.currentTimeMillis());
        return response;
    }

    private Map<String, Object> createValidationResponse(boolean valid, String driverId, String userId, 
                                                        String status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("valid", valid);
        response.put("driver_id", driverId);
        response.put("user_id", userId);
        response.put("success", !status.equals("ERROR"));
        response.put("status", status);
        response.put("message", message);
        return response;
    }

    private Map<String, Object> createDetailedValidationResponse(boolean overallValid, String driverId, 
                                                               Driver driver, boolean userValid, 
                                                               boolean driverValid, Map<String, Object> grpcResponse) {
        Map<String, Object> response = new HashMap<>();
        response.put("valid", overallValid);
        response.put("driver_id", driverId);
        response.put("user_id", driver.getUserId().toString());
        response.put("success", true);
        response.put("status", overallValid ? "ACTIVE" : "INACTIVE");
        response.put("driver_status", driver.getStatus().toString());
        response.put("user_valid", userValid);
        response.put("driver_valid", driverValid);
        response.put("message", overallValid ? 
            "Driver and user are valid and active" : 
            "Driver or user validation failed");
        response.put("user_validation", grpcResponse);
        return response;
    }
}
