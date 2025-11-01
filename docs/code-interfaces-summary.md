# UIT-Go Code Interfaces Summary

## Overview
This document provides a comprehensive summary of all the code interfaces created for the UIT-Go ride-hailing microservices system.

## Services Structure

### 1. User Service (Port: 8081)

#### REST API Endpoints
- `POST /api/users/register` - Register new user
- `GET /api/users/{userId}` - Get user by ID
- `GET /api/users/email/{email}` - Get user by email
- `PUT /api/users/{userId}` - Update user information
- `GET /api/users/type/{userType}` - Get users by type (PASSENGER/DRIVER)

#### Key Classes
- **Controller**: `UserController.java`
- **Service**: `UserService.java`
- **Repository**: `UserRepository.java`
- **Entity**: `User.java`
- **DTOs**: 
  - `CreateUserRequest.java`
  - `UserResponse.java`
  - `UpdateUserRequest.java`

#### Entity Fields
```java
User {
    Long id
    String email (unique)
    String name
    String userType // "PASSENGER" or "DRIVER"
    String phone
    LocalDateTime createdAt
}
```

### 2. Trip Service (Port: 8082)

#### REST API Endpoints
- `POST /api/trips/request` - Request new trip
- `GET /api/trips/{tripId}` - Get trip by ID
- `PUT /api/trips/{tripId}/status` - Update trip status
- `PUT /api/trips/{tripId}/assign-driver` - Assign driver to trip
- `GET /api/trips/passenger/{passengerId}` - Get trips by passenger
- `GET /api/trips/driver/{driverId}` - Get trips by driver

#### Key Classes
- **Controller**: `TripController.java`
- **Service**: `TripService.java`
- **Repository**: `TripRepository.java`
- **Entity**: `Trip.java`
- **DTOs**:
  - `CreateTripRequest.java`
  - `TripResponse.java`
  - `UpdateTripStatusRequest.java`
  - `AssignDriverRequest.java`

#### Entity Fields
```java
Trip {
    Long id
    Long passengerId
    Long driverId
    String status // "REQUESTED", "ACCEPTED", "IN_PROGRESS", "COMPLETED", "CANCELLED"
    String pickupLocation
    String destination
    BigDecimal pickupLatitude
    BigDecimal pickupLongitude
    BigDecimal destinationLatitude
    BigDecimal destinationLongitude
    BigDecimal fare
    LocalDateTime createdAt
    LocalDateTime updatedAt
}
```

### 3. Driver Service (Port: 8083)

#### REST API Endpoints
- `POST /api/drivers/register` - Register new driver
- `GET /api/drivers/{driverId}` - Get driver by ID
- `PUT /api/drivers/{driverId}/status` - Update driver status
- `PUT /api/drivers/{driverId}/location` - Update driver location
- `GET /api/drivers/available` - Get all available drivers
- `GET /api/drivers/nearby?latitude={lat}&longitude={lng}&radiusKm={radius}` - Find nearby drivers

#### Key Classes
- **Controller**: `DriverController.java`
- **Service**: `DriverService.java`
- **Repository**: `DriverRepository.java`
- **Entity**: `Driver.java`
- **DTOs**:
  - `CreateDriverRequest.java`
  - `DriverResponse.java`
  - `UpdateDriverStatusRequest.java`
  - `UpdateLocationRequest.java`

#### Entity Fields
```java
Driver {
    Long id
    Long userId (references User.id)
    String vehiclePlate (unique)
    String vehicleModel
    String status // "AVAILABLE", "BUSY", "OFFLINE"
    BigDecimal currentLatitude
    BigDecimal currentLongitude
    LocalDateTime createdAt
    LocalDateTime lastUpdated
}
```

### 4. API Gateway (Port: 8080)

#### Configuration
- **Gateway Config**: `GatewayConfig.java` - Routes configuration
- **Health Controller**: `HealthController.java` - Health check endpoints

#### Routes
- `/api/users/**` → User Service (8081)
- `/api/trips/**` → Trip Service (8082)
- `/api/drivers/**` → Driver Service (8083)

## Shared Components

### Protocol Buffer Definitions
- `user.proto` - User service gRPC definitions
- `driver.proto` - Driver service gRPC definitions  
- `trip.proto` - Trip service gRPC definitions

### Shared Resources
- `shared/README.md` - Documentation for shared resources
- Contains protobuf definitions and common configurations

## gRPC Services (Go Implementation)

### User Service gRPC (Port: 50051)
- `user-service/main.go` - Go gRPC server for user operations
- Methods: GetUser, ValidateUser, GetUsersByType
- Database: Connects to user_service DB (port 5435)

### Driver Service gRPC (Port: 50052)
- `driver-service/main.go` - Go gRPC server for driver operations
- Methods: FindNearbyDrivers, GetDriverStatus, UpdateDriverLocation
- Database: Connects to driver_service DB (port 5434)
- Features: Haversine distance calculation for nearby drivers

### Trip Service gRPC (Port: 50053)  
- `trip-service/main.go` - Go gRPC server for trip operations
- Methods: GetTrip, GetTripsByUser, UpdateTripStatus
- Database: Connects to trip_service DB (port 5433)

## Key Features Implemented

1. **RESTful APIs**: All services expose REST endpoints for CRUD operations
2. **JPA Repositories**: Database access layer with custom query methods
3. **Service Layer**: Business logic implementation
4. **DTO Pattern**: Request/Response data transfer objects
5. **Entity Mapping**: JPA entities with proper relationships
6. **Location Services**: Geographic coordinate handling for drivers
7. **Status Management**: Comprehensive status tracking for trips and drivers
8. **Exception Handling**: Custom exceptions for different scenarios

## Database Design

### User Table
```sql
users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    user_type VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP
)
```

### Trip Table
```sql
trips (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    passenger_id BIGINT NOT NULL,
    driver_id BIGINT,
    status VARCHAR(50) NOT NULL,
    pickup_location TEXT,
    destination TEXT,
    pickup_latitude DECIMAL(10,8),
    pickup_longitude DECIMAL(11,8),
    destination_latitude DECIMAL(10,8),
    destination_longitude DECIMAL(11,8),
    fare DECIMAL(10,2),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
)
```

### Driver Table
```sql
drivers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNIQUE NOT NULL,
    vehicle_plate VARCHAR(20) UNIQUE NOT NULL,
    vehicle_model VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    current_latitude DECIMAL(10,8),
    current_longitude DECIMAL(11,8),
    created_at TIMESTAMP,
    last_updated TIMESTAMP
)
```

## Business Logic

1. **User Registration**: Create user accounts for passengers and drivers
2. **Trip Management**: Handle trip lifecycle from request to completion
3. **Driver Assignment**: Match drivers to trip requests based on location
4. **Location Tracking**: Real-time driver location updates
5. **Status Updates**: Track trip and driver status changes
6. **Fare Calculation**: Basic fare calculation logic

## Next Implementation Steps

1. Add Spring Boot Application main classes
2. Generate Go protobuf code from .proto files
3. Add RabbitMQ message handling
4. Implement proper error handling and validation
5. Add unit and integration tests
6. Configure application properties for each service
7. Add Docker configuration refinements
8. Implement authentication and authorization (JWT)
9. Set up gRPC client connections in Spring Boot services

This code structure provides a solid foundation for the UIT-Go ride-hailing microservices system with:
- **Spring Boot REST APIs** for external client communication
- **Go gRPC services** for efficient inter-service communication
- **Clear separation** between HTTP and gRPC layers
- **Proper microservices architecture** with database-per-service pattern