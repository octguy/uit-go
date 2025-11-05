# UIT-Go System Test Commands

This document provides standalone curl and grpcurl commands to test the entire UIT-Go ride-sharing platform, including RabbitMQ message flow.

## Prerequisites

1. Start the system: `cd infra && docker-compose up -d`
2. Wait for all services to start (30-60 seconds)
3. Ensure you have `curl` and `grpcurl` installed

## 1. Health Checks

### Check All Services
```bash
# API Gateway
curl -s "http://localhost:8080/actuator/health" || echo "API Gateway: Using fallback health check"

# Driver Service
curl -s "http://localhost:8083/api/driver-service/api/drivers/health"

# User Service (via API Gateway)
curl -s "http://localhost:8080/api/users/health" || echo "User Service: No health endpoint"

# Trip Service (via API Gateway) 
curl -s "http://localhost:8080/api/trips/health" || echo "Trip Service: No health endpoint"

# RabbitMQ Management
curl -s -u guest:guest "http://localhost:15672/api/overview" | grep -o '"rabbitmq_version":"[^"]*"'

# Redis
redis-cli -h localhost ping || echo "Redis: Use docker exec to test"
```

## 2. RabbitMQ Queue Status

### Check All Queues
```bash
# List all queues
curl -s -u guest:guest "http://localhost:15672/api/queues" | grep -o '"name":"[^"]*"'

# Check specific queues
curl -s -u guest:guest "http://localhost:15672/api/queues/%2F/driver.location.updates" | grep -o '"messages":[0-9]*'
curl -s -u guest:guest "http://localhost:15672/api/queues/%2F/driver.status.changes" | grep -o '"messages":[0-9]*'
curl -s -u guest:guest "http://localhost:15672/api/queues/%2F/trip.created.queue" | grep -o '"messages":[0-9]*'
```

### Check Exchanges
```bash
curl -s -u guest:guest "http://localhost:15672/api/exchanges" | grep -o '"name":"[^"]*","type":"[^"]*"'
```

## 3. User Management (REST API)

### Create a Test User
```bash
curl -X POST "http://localhost:8080/api/users/register" -H "Content-Type: application/json" -d '{
    "name": "John Doe",
    "email": "john.doe@example.com",
    "phone": "+84901234567",
    "password": "securepassword123",
    "userType": "PASSENGER"
  }'
```

### Get User by Email
```bash
curl -s "http://localhost:8080/api/users/email/john.doe@example.com"
```

## 4. Driver Management (REST API)

### Register a Test Driver
```bash
curl -X POST "http://localhost:8083/api/driver-service/api/drivers/register" -H "Content-Type: application/json" -d '{
    "userId": "550e8400-e29b-41d4-a716-44665544999",
    "email": "driver2@example.com",
    "phone": "+84901234567",
    "name": "Test Driver 2",
    "license_number": "DL987654325",
    "vehicle_type": "Toyota Vios",
    "vehicle_plate": "51A-66666"
  }'
```

### Get Driver List
```bash
curl -s "http://localhost:8083/api/driver-service/api/drivers/list"
```

### Update Driver Location (Triggers RabbitMQ Message)
```bash
# First, get a valid driver ID from the list above, then:
DRIVER_ID="550e8400-e29b-41d4-a716-446655440002"  # Replace with actual driver ID

curl -X PUT "http://localhost:8083/api/driver-service/api/drivers/${DRIVER_ID}/location" -H "Content-Type: application/json" -d '{
    "latitude": 10.762622,
    "longitude": 106.660172
  }'
```

### Find Nearby Drivers
```bash
curl -s "http://localhost:8083/api/driver-service/api/drivers/nearby?latitude=10.762622&longitude=106.660172&radius=5000"
```

## 5. Trip Management (REST API)

### Create a Trip Request
```bash
curl -X POST "http://localhost:8080/api/trips/request" -H "Content-Type: application/json" -d '{
    "passengerId": "550e8400-e29b-41d4-a716-446655440001",
    "pickupLatitude": 10.762622,
    "pickupLongitude": 106.660172,
    "destinationLatitude": 10.772622,
    "destinationLongitude": 106.670172,
    "pickupLocation": "District 1, Ho Chi Minh City",
    "destination": "District 3, Ho Chi Minh City"
  }'
```

### Get Fare Estimate
```bash
curl -s "http://localhost:8080/api/trips/estimate" \
  -H "Content-Type: application/json" \
  -X POST \
  -d '{
    "pickupLatitude": 10.762622,
    "pickupLongitude": 106.660172,
    "destinationLatitude": 10.772622,
    "destinationLongitude": 106.670172
  }'
```

### Assign Driver to Trip
```bash
# Get trip ID from the trip creation response, then:
TRIP_ID="your-trip-id-here"  # Replace with actual trip ID
DRIVER_ID="550e8400-e29b-41d4-a716-446655440002"  # Replace with actual driver ID

curl -X PUT "http://localhost:8080/api/trips/${TRIP_ID}/assign-driver" \
  -H "Content-Type: application/json" \
  -d '{
    "driverId": "'${DRIVER_ID}'"
  }'
```

## 6. gRPC Service Tests

### User gRPC Service
```bash
# List available services
grpcurl -plaintext localhost:50051 list

# Validate user by ID
grpcurl -plaintext -d '{"user_id": "550e8400-e29b-41d4-a716-446655440001"}' localhost:50051 user.UserService/ValidateUser

# Create user via gRPC
grpcurl -plaintext -d '{
  "name": "Jane Smith",
  "email": "jane.smith@example.com",
  "phone": "+84901234569"
}' localhost:50051 user.UserService/CreateUser
```

### Trip gRPC Service
```bash
# List available services
grpcurl -plaintext localhost:50052 list

# Get trip by ID
grpcurl -plaintext -d '{"trip_id": "your-trip-id"}' \
  localhost:50052 trip.TripService/GetTrip

# Create trip via gRPC
grpcurl -plaintext -d '{
  "user_id": "550e8400-e29b-41d4-a716-446655440001",
  "pickup_latitude": 10.762622,
  "pickup_longitude": 106.660172,
  "destination_latitude": 10.772622,
  "destination_longitude": 106.670172,
  "pickup_address": "District 1, HCMC",
  "destination_address": "District 3, HCMC"
}' localhost:50052 trip.TripService/CreateTrip
```

### Driver gRPC Service
```bash
# List available services
grpcurl -plaintext localhost:50053 list

# Get driver by ID
grpcurl -plaintext -d '{"driver_id": "550e8400-e29b-41d4-a716-446655440002"}' localhost:50053 driver.DriverService/GetDriver

# Update driver location via gRPC
grpcurl -plaintext -d '{
  "driver_id": "550e8400-e29b-41d4-a716-446655440002",
  "latitude": 10.762622,
  "longitude": 106.660172
}' localhost:50053 driver.DriverService/UpdateDriverLocation
```

## 7. Complete End-to-End Flow Test

### Step 1: Create User
```bash
USER_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "testuser@example.com",
    "phone": "+84901111111",
    "password": "password123"
  }')

echo "User created: $USER_RESPONSE"
USER_ID=$(echo $USER_RESPONSE | grep -o '"userId":"[^"]*"' | cut -d'"' -f4)
echo "User ID: $USER_ID"
```

### Step 2: Register Driver
```bash
DRIVER_RESPONSE=$(curl -s -X POST "http://localhost:8083/api/driver-service/api/drivers/register" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440001",
    "email": "testdriver@example.com",
    "phone": "+84902222222",
    "name": "Test Driver",
    "license_number": "DL111111111",
    "vehicle_type": "Honda City",
    "vehicle_plate": "51A-11111"
  }')

echo "Driver registered: $DRIVER_RESPONSE"
```

### Step 3: Update Driver Location (Triggers RabbitMQ)
```bash
LOCATION_RESPONSE=$(curl -s -X PUT "http://localhost:8083/api/driver-service/api/drivers/550e8400-e29b-41d4-a716-446655440002/location" \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 10.762622,
    "longitude": 106.660172
  }')

echo "Location updated: $LOCATION_RESPONSE"
```

### Step 4: Check RabbitMQ Messages
```bash
echo "Checking RabbitMQ queues for new messages..."
curl -s -u guest:guest "http://localhost:15672/api/queues/%2F/driver.location.updates" | grep -o '"messages":[0-9]*'
```

### Step 5: Create Trip
```bash
TRIP_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/trips/request" -H "Content-Type: application/json" -d '{
    "passengerId": "be43cbdf-3718-49fa-8c8c-75447251184c",
    "pickupLatitude": 10.762622,
    "pickupLongitude": 106.660172,
    "destinationLatitude": 10.772622,
    "destinationLongitude": 106.670172,
    "pickupLocation": "District 1, Ho Chi Minh City",
    "destination": "District 3, Ho Chi Minh City"
  }')

echo "Trip created: $TRIP_RESPONSE"
TRIP_ID=$(echo $TRIP_RESPONSE | grep -o '"tripId":"[^"]*"' | cut -d'"' -f4)
echo "Trip ID: $TRIP_ID"
```

### Step 6: Find Nearby Drivers
```bash
NEARBY_DRIVERS=$(curl -s "http://localhost:8083/api/driver-service/api/drivers/nearby?latitude=10.762622&longitude=106.660172&radius=5000")
echo "Nearby drivers: $NEARBY_DRIVERS"
```

## 8. RabbitMQ Message Flow Verification

### Monitor Queue Activity
```bash
# Watch queue message counts in real-time
watch -n 1 'curl -s -u guest:guest "http://localhost:15672/api/queues" | grep -o "\"name\":\"[^\"]*\",\"messages\":[0-9]*"'
```

### Manual Message Publishing (Direct to RabbitMQ)
```bash
# Publish a test message to driver.location.updates queue
curl -s -u guest:guest -X POST "http://localhost:15672/api/exchanges/%2F/driver.events/publish" \
  -H "Content-Type: application/json" \
  -d '{
    "properties": {},
    "routing_key": "driver.location.updated",
    "payload": "{\"driverId\":\"test-123\",\"latitude\":10.762622,\"longitude\":106.660172,\"timestamp\":\"2025-11-02T13:40:00Z\"}",
    "payload_encoding": "string"
  }'
```

## 9. Service Integration Test

### Complete Ride Flow
```bash
#!/bin/bash
echo "=== UIT-Go Complete Ride Flow Test ==="

# 1. Check system health
echo "1. Checking system health..."
curl -s "http://localhost:8083/api/driver-service/api/drivers/health" > /dev/null && echo "✅ Driver Service OK" || echo "❌ Driver Service Failed"

# 2. Create user
echo "2. Creating test user..."
USER_ID="550e8400-e29b-41d4-a716-446655440001"

# 3. Update driver location
echo "3. Updating driver location..."
DRIVER_ID="550e8400-e29b-41d4-a716-446655440002"
curl -s -X PUT "http://localhost:8083/api/driver-service/api/drivers/${DRIVER_ID}/location" \
  -H "Content-Type: application/json" \
  -d '{"latitude": 10.762622, "longitude": 106.660172}' > /dev/null && echo "✅ Location Updated" || echo "❌ Location Update Failed"

# 4. Check RabbitMQ message
echo "4. Checking RabbitMQ messages..."
MESSAGES=$(curl -s -u guest:guest "http://localhost:15672/api/queues/%2F/driver.location.updates" | grep -o '"messages":[0-9]*' | cut -d':' -f2)
echo "📨 Messages in queue: $MESSAGES"

# 5. Create trip
echo "5. Creating trip..."
curl -s -X POST "http://localhost:8080/api/trips/request" \
  -H "Content-Type: application/json" \
  -d "{\"userId\": \"${USER_ID}\", \"pickupLatitude\": 10.762622, \"pickupLongitude\": 106.660172, \"destinationLatitude\": 10.772622, \"destinationLongitude\": 106.670172, \"pickupAddress\": \"District 1\", \"destinationAddress\": \"District 3\"}" > trip_result.json

if grep -q "tripId" trip_result.json; then
    echo "✅ Trip Created Successfully"
    TRIP_ID=$(grep -o '"tripId":"[^"]*"' trip_result.json | cut -d'"' -f4)
    echo "🚕 Trip ID: $TRIP_ID"
else
    echo "❌ Trip Creation Failed"
    cat trip_result.json
fi

echo "=== Test Complete ==="
```

## 10. Performance & Load Testing

### Concurrent Location Updates
```bash
# Test multiple simultaneous location updates
for i in {1..10}; do
  curl -s -X PUT "http://localhost:8083/api/driver-service/api/drivers/550e8400-e29b-41d4-a716-446655440002/location" \
    -H "Content-Type: application/json" \
    -d "{\"latitude\": $((10762622 + i)), \"longitude\": $((106660172 + i))}" &
done
wait
echo "All location updates completed"
```

### RabbitMQ Performance
```bash
# Check RabbitMQ message rates
curl -s -u guest:guest "http://localhost:15672/api/queues" | grep -o '"message_stats":{"publish_details":{"rate":[0-9.]*}}'
```

## Expected Results

When running these commands successfully, you should see:

1. **Health checks**: All services respond with 200 OK
2. **RabbitMQ**: Queues exist and show message activity
3. **User/Driver registration**: Returns success with generated IDs
4. **Location updates**: Trigger RabbitMQ messages in driver.location.updates queue
5. **Trip creation**: Returns trip ID and triggers driver matching
6. **gRPC services**: Respond to reflection and method calls
7. **End-to-end flow**: Complete ride request from user creation to driver assignment

## Troubleshooting

- **503 Service Unavailable**: Wait longer for services to start
- **404 Not Found**: Check service context paths and endpoints
- **RabbitMQ connection errors**: Verify RabbitMQ container is running
- **gRPC unavailable**: Ensure gRPC services have started and ports are accessible

### Update Driver
```
curl -X PUT http://localhost:8083/api/drivers/550e8400-e29b-41d4-a716-446655440002 -H "Content-Type: application/json" -d '{
    "licenseNumber": "DL987654321",
    "vehicleModel": "Honda Civic",
    "vehiclePlate": "XYZ-5678",
    "rating": 4.5,
    "status": "AVAILABLE"
  }'
  ```