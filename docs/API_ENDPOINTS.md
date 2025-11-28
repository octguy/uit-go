# API Endpoints Documentation

This document contains all REST API endpoints for the UIT-GO ride-hailing application.

## Table of Contents

- [User Service (Port 8081)](#user-service-port-8081)
- [Trip Service (Port 8082)](#trip-service-port-8082)
- [Driver Service (Port 8083)](#driver-service-port-8083)
- [Driver Simulator (Port 8084)](#driver-simulator-port-8084)

---

## User Service (Port 8081)

### User Management

#### Register User

```
POST http://localhost:8081/api/users/register
```

#### Login

```
POST http://localhost:8081/api/users/login
```

#### Get Current User

```
GET http://localhost:8081/api/users/me
Authorization: Bearer <token>
```

#### Get User by ID

```
GET http://localhost:8081/api/users/{userId}
```

#### Get User by Email

```
GET http://localhost:8081/api/users/email/{email}
```

#### Get All Users

```
GET http://localhost:8081/api/users
```

### Driver Management

#### Register Driver

```
POST http://localhost:8081/api/drivers/register
```

### Internal Endpoints (Service-to-Service)

#### Validate Token

```
GET http://localhost:8081/api/internal/auth/validate
Authorization: Bearer <token>
```

#### Get All Drivers (Internal)

```
GET http://localhost:8081/api/internal/drivers
```

---

## Trip Service (Port 8082)

### Trip Management

#### Get All Trips

```
GET http://localhost:8082/api/trips
```

#### Get Estimated Fare

```
GET http://localhost:8082/api/trips/get-estimated-fare
```

#### Create Trip

```
POST http://localhost:8082/api/trips/create
Authorization: Bearer <passenger-token>
```

#### Get Trip by ID

```
GET http://localhost:8082/api/trips/{id}
```

#### Cancel Trip

```
POST http://localhost:8082/api/trips/{id}/cancel
Authorization: Bearer <passenger-token>
```

#### Accept Trip

```
POST http://localhost:8082/api/trips/{id}/accept
Authorization: Bearer <driver-token>

Note: Trip must be accepted within 15 seconds of creation
```

#### Start Trip

```
POST http://localhost:8082/api/trips/{id}/start
Authorization: Bearer <driver-token>
```

#### Complete Trip

```
POST http://localhost:8082/api/trips/{id}/complete
Authorization: Bearer <driver-token>
```

#### Rate Trip

```
POST http://localhost:8082/api/trips/{id}/rate?rating={1-5}&comment={text}
Authorization: Bearer <token>
```

#### Get User Trips

```
GET http://localhost:8082/api/trips/user
Authorization: Bearer <passenger-token>
```

#### Get Driver Trips

```
GET http://localhost:8082/api/trips/driver
Authorization: Bearer <driver-token>
```

#### Get Nearby Drivers

```
GET http://localhost:8082/api/trips/driver/get-nearby-drivers?lat={lat}&lng={lng}&radiusKm={3.0}&limit={5}
```

#### Debug Endpoints

```
GET http://localhost:8082/api/trips/get-user-request
GET http://localhost:8082/api/trips/user
GET http://localhost:8082/api/trips/driver
```

### Rating Management

#### Get All Ratings

```
GET http://localhost:8082/api/ratings
```

#### Health Check

```
GET http://localhost:8082/api/ratings/hello
```

---

## Driver Service (Port 8083)

### Driver Status Management

#### Set All Drivers Online

```
POST http://localhost:8083/api/drivers/online-all
```

#### Set Driver Online

```
POST http://localhost:8083/api/drivers/online
Authorization: Bearer <driver-token>
```

#### Set Driver Offline

```
POST http://localhost:8083/api/drivers/offline
Authorization: Bearer <driver-token>
```

### Trip Notification Management

#### Accept Trip (with Driver ID)

```
POST http://localhost:8083/api/drivers/trips/{tripId}/accept?driverId={uuid}
```

#### Decline Trip

```
POST http://localhost:8083/api/drivers/trips/{tripId}/decline?driverId={uuid}
```

#### Get Pending Trips for Driver

```
GET http://localhost:8083/api/drivers/trips/pending?driverId={uuid}

Note: Notifications expire after 15 seconds (Redis TTL)
```

#### Get Trip Notification

```
GET http://localhost:8083/api/drivers/trips/{tripId}
```

### Internal Endpoints (Service-to-Service)

#### Get Nearby Drivers

```
GET http://localhost:8083/api/internal/drivers/nearby?lat={lat}&lng={lng}&radiusKm={3.0}&limit={5}
```

---

## Driver Simulator (Port 8084)

### Simulation Control

#### Start All Drivers Simulation

```
POST http://localhost:8084/api/simulate/start-all?startLat={lat}&startLng={lng}&endLat={lat}&endLng={lng}&steps={100}&delayMillis={1000}

Example:
POST http://localhost:8084/api/simulate/start-all?startLat=10.762622&startLng=106.660172&endLat=10.776889&endLng=106.700806&steps=200&delayMillis=1000
```

---

## Authentication & Authorization

### Token-based Authentication

Most endpoints require a JWT token in the Authorization header:
```
Authorization: Bearer <token>
```

### Roles

- **PASSENGER**: Can create trips, cancel trips, rate trips
- **DRIVER**: Can accept trips, start trips, complete trips

### Special Notes

1. **Trip Acceptance Time Limit**: Drivers must accept trips within 15 seconds of creation
2. **Driver Location Updates**: Driver simulator sends location updates via gRPC, stored in Redis with geospatial indexing
3. **Trip Notifications**: Sent via RabbitMQ when trip is created, stored in Redis with 15-second TTL
4. **Internal Endpoints**: `/api/internal/*` endpoints are for service-to-service communication only

---

## Port Summary

| Service             | Port  | Purpose                                     |
| ------------------- | ----- | ------------------------------------------- |
| User Service        | 8081  | User & driver registration, authentication  |
| Trip Service        | 8082  | Trip management, ratings                    |
| Driver Service      | 8083  | Driver status, location, trip notifications |
| Driver Simulator    | 8084  | Simulate driver movement for testing        |
| RabbitMQ            | 5672  | Message queue for async notifications       |
| RabbitMQ Management | 15672 | Web UI for RabbitMQ                         |
| Redis               | 6379  | Cache for locations & notifications         |

---

Generated: November 28, 2025

Generated: November 28, 2025
