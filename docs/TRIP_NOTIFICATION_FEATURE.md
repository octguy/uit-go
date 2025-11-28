# Trip Notification System - User Story Implementation

## Overview

This implementation adds a **real-time trip notification system** using **RabbitMQ** that notifies nearby drivers when a passenger requests a trip. Drivers have **15 seconds** to accept or decline the trip request.

## User Story

**As a driver**, when a trip request comes up nearby, I want to be notified and have 15 seconds to decide whether to accept or decline.

## Architecture

### Components

1. **Trip Service** - Publishes trip notifications to RabbitMQ when a trip is created
2. **Driver Service** - Listens for trip notifications and manages driver responses
3. **RabbitMQ** - Message broker for asynchronous communication
4. **Redis** - Stores pending notifications with TTL (Time To Live)

### Flow Diagram

```
Passenger App          Trip Service          RabbitMQ          Driver Service          Driver App
     │                      │                    │                    │                    │
     │ Create Trip          │                    │                    │                    │
     ├─────────────────────>│                    │                    │                    │
     │                      │                    │                    │                    │
     │                      │ Find Nearby        │                    │                    │
     │                      │ Drivers            │                    │                    │
     │                      │                    │                    │                    │
     │                      │ Publish Notification│                   │                    │
     │                      ├───────────────────>│                    │                    │
     │                      │                    │                    │                    │
     │                      │                    │ Consume Message    │                    │
     │                      │                    ├───────────────────>│                    │
     │                      │                    │                    │                    │
     │                      │                    │                    │ Store in Redis     │
     │                      │                    │                    │ (15 sec TTL)       │
     │                      │                    │                    │                    │
     │                      │                    │                    │ Push Notification  │
     │                      │                    │                    ├───────────────────>│
     │                      │                    │                    │                    │
     │                      │                    │                    │                    │ Driver decides
     │                      │                    │                    │                    │ (15 seconds)
     │                      │                    │                    │                    │
     │                      │                    │                    │ Accept/Decline     │
     │                      │                    │                    │<───────────────────┤
     │                      │                    │                    │                    │
     │                      │ Update Trip Status │                    │                    │
     │<─────────────────────┼────────────────────┼────────────────────┤                    │
     │                      │                    │                    │                    │
```

## Implementation Details

### 1. RabbitMQ Configuration

**Docker Compose** (`infra/docker-compose.yml`):

```yaml
rabbitmq:
  image: rabbitmq:3.13-management-alpine
  container_name: rabbitmq
  ports:
    - "5672:5672" # AMQP port
    - "15672:15672" # Management UI
  environment:
    RABBITMQ_DEFAULT_USER: guest
    RABBITMQ_DEFAULT_PASS: guest
```

**Exchange & Queue Configuration**:

- Exchange: `trip.exchange` (Topic Exchange)
- Queue: `trip.notification.queue`
- Routing Key: `trip.notification`

### 2. Trip Service Components

**DTOs**:

- `TripNotificationRequest` - Contains trip details sent to RabbitMQ
- `TripNotificationResponse` - Response when driver accepts/declines

**Services**:

- `TripNotificationServiceImpl` - Publishes trip notifications to RabbitMQ

**Key Method** - `TripServiceImpl.createTrip()`:

```java
// 1. Create trip in database
// 2. Find nearby drivers (using Redis geospatial)
// 3. Build notification message
// 4. Publish to RabbitMQ
tripNotificationService.notifyNearbyDrivers(notification);
```

### 3. Driver Service Components

**Entity**:

- `PendingTripNotification` - Stores notification details in Redis with 15-second TTL

**Services**:

- `TripNotificationServiceImpl` - Manages pending notifications and driver responses

**Listener**:

- `TripNotificationListener` - Consumes messages from RabbitMQ queue

**Controller** - `TripNotificationController`:

- `POST /api/drivers/trips/{tripId}/accept?driverId={id}` - Accept trip
- `POST /api/drivers/trips/{tripId}/decline?driverId={id}` - Decline trip
- `GET /api/drivers/trips/pending?driverId={id}` - Get pending trips for driver
- `GET /api/drivers/trips/{tripId}` - Get specific trip notification

**Scheduled Task**:

- Runs every 5 seconds to clean up expired notifications

### 4. 15-Second Timeout Mechanism

The timeout is implemented using:

1. **Redis TTL**: Notifications are stored in Redis with 15-second TTL
2. **ExpiresAt field**: Each notification has an `expiresAt` timestamp
3. **Scheduled Task**: Cleanup job runs every 5 seconds to remove expired notifications
4. **Validation**: When driver accepts/declines, system checks if notification is still valid

```java
// Stored in Redis with 15-second TTL
redisTemplate.opsForValue().set(key, pending, 15, TimeUnit.SECONDS);

// Validation during accept
if (LocalDateTime.now().isAfter(pending.getExpiresAt())) {
    return "Trip notification has expired";
}
```

## API Endpoints

### Trip Service

#### Create Trip

```bash
POST /api/trips/create
Authorization: Bearer <JWT>

Request Body:
{
  "pickupLatitude": 10.762622,
  "pickupLongitude": 106.660172,
  "destinationLatitude": 10.777229,
  "destinationLongitude": 106.695534,
  "estimatedFare": 50000
}

Response:
{
  "id": "uuid",
  "passengerId": "uuid",
  "status": "SEARCHING_DRIVER",
  "pickupLatitude": 10.762622,
  "pickupLongitude": 106.660172,
  "destinationLatitude": 10.777229,
  "destinationLongitude": 106.695534,
  "fare": 50000,
  "requestedAt": "2024-01-01T10:00:00"
}
```

### Driver Service

#### Get Pending Trips (for Driver)

```bash
GET /api/drivers/trips/pending?driverId={driverId}

Response:
[
  {
    "tripId": "uuid",
    "passengerId": "uuid",
    "passengerName": "Passenger",
    "pickupLatitude": 10.762622,
    "pickupLongitude": 106.660172,
    "destinationLatitude": 10.777229,
    "destinationLongitude": 106.695534,
    "estimatedFare": 50000,
    "distanceKm": 2.5,
    "notifiedAt": "2024-01-01T10:00:00",
    "expiresAt": "2024-01-01T10:00:15",
    "expired": false,
    "accepted": false
  }
]
```

#### Accept Trip

```bash
POST /api/drivers/trips/{tripId}/accept?driverId={driverId}

Response (Success):
{
  "tripId": "uuid",
  "driverId": "uuid",
  "accepted": true,
  "message": "Trip accepted successfully"
}

Response (Expired):
{
  "tripId": "uuid",
  "driverId": "uuid",
  "accepted": false,
  "message": "Trip notification has expired"
}

Response (Already Accepted):
{
  "tripId": "uuid",
  "driverId": "uuid",
  "accepted": false,
  "message": "Trip already accepted by another driver"
}
```

#### Decline Trip

```bash
POST /api/drivers/trips/{tripId}/decline?driverId={driverId}

Response:
{
  "tripId": "uuid",
  "driverId": "uuid",
  "accepted": false,
  "message": "Trip declined by driver"
}
```

## Configuration

### Trip Service (`application.yml`)

```yaml
spring:
  rabbitmq:
    host: rabbitmq
    port: 5672
    username: guest
    password: guest

rabbitmq:
  exchange:
    trip: trip.exchange
  queue:
    trip-notification: trip.notification.queue
  routing-key:
    trip-notification: trip.notification
```

### Driver Service (`application.yml`)

```yaml
spring:
  rabbitmq:
    host: rabbitmq
    port: 5672
    username: guest
    password: guest

rabbitmq:
  exchange:
    trip: trip.exchange
  queue:
    trip-notification: trip.notification.queue
  routing-key:
    trip-notification: trip.notification
```

## Dependencies Added

### Both Services (`pom.xml`)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

## Testing

### 1. Start Services

```bash
cd infra
docker-compose up -d
```

### 2. Create a Trip

```bash
# Login as passenger and get JWT token
curl -X POST http://localhost:8082/api/trips/create \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "pickupLatitude": 10.762622,
    "pickupLongitude": 106.660172,
    "destinationLatitude": 10.777229,
    "destinationLongitude": 106.695534,
    "estimatedFare": 50000
  }'
```

### 3. Check Driver's Pending Trips

```bash
curl -X GET "http://localhost:8083/api/drivers/trips/pending?driverId=<driver-uuid>"
```

### 4. Accept Trip (within 15 seconds)

```bash
curl -X POST "http://localhost:8083/api/drivers/trips/<trip-uuid>/accept?driverId=<driver-uuid>"
```

### 5. Monitor RabbitMQ

Open browser: `http://localhost:15672`

- Username: `guest`
- Password: `guest`

Navigate to **Queues** tab to see:

- Queue: `trip.notification.queue`
- Messages published/consumed
- Message rates

## Key Features

✅ **Asynchronous Communication** - RabbitMQ decouples trip creation from driver notification  
✅ **15-Second Timeout** - Automatic expiration using Redis TTL  
✅ **Concurrent Access Control** - Prevents multiple drivers from accepting same trip  
✅ **Scalable** - Multiple driver service instances can consume from same queue  
✅ **Fault Tolerant** - Messages persist in RabbitMQ if driver service is down  
✅ **Real-time Updates** - Drivers get notified immediately via message queue  
✅ **Auto Cleanup** - Scheduled task removes expired notifications

## Future Enhancements

1. **WebSocket Integration** - Push real-time notifications to driver mobile app
2. **Priority Queue** - VIP passengers get higher priority
3. **Driver Radius Filtering** - Only notify drivers within specific radius
4. **Retry Mechanism** - Notify next closest driver if first declines
5. **Analytics** - Track acceptance rates, response times
6. **Push Notifications** - Mobile push notifications via FCM/APNs
7. **Dead Letter Queue** - Handle failed notifications

## Monitoring

### RabbitMQ Management UI

- **URL**: `http://localhost:15672`
- **Credentials**: guest/guest
- **Metrics**: Messages published, consumed, queue depth, consumer count

### Redis Monitoring

```bash
# Connect to Redis
docker exec -it redis redis-cli

# Check pending trips
KEYS pending_trips:*

# Get specific trip
GET pending_trips:<trip-uuid>

# Check TTL
TTL pending_trips:<trip-uuid>
```

### Logs

```bash
# Trip Service logs
docker logs -f trip-service

# Driver Service logs
docker logs -f driver-service

# RabbitMQ logs
docker logs -f rabbitmq
```

## Troubleshooting

### Issue: Messages not being consumed

**Solution**: Check if RabbitMQ listener is enabled in driver-service

```bash
# Verify @EnableRabbitListener in application
# Check RabbitMQ connection in logs
```

### Issue: Notifications expiring too quickly

**Solution**: Adjust timeout in `TripNotificationServiceImpl`

```java
private static final int NOTIFICATION_TIMEOUT_SECONDS = 15;
```

### Issue: Redis not storing objects

**Solution**: Ensure `redisObjectTemplate` bean is configured with JSON serialization

## Summary

This implementation successfully delivers the user story with:

- ✅ Real-time notifications via RabbitMQ
- ✅ 15-second decision window with Redis TTL
- ✅ Accept/Decline functionality
- ✅ Automatic expiration and cleanup
- ✅ Scalable architecture
- ✅ Production-ready error handling

The system is now ready for drivers to receive and respond to trip requests with the required 15-second timeout!
