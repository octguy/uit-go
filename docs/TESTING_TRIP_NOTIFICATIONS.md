# Testing Guide: Trip Notification System

## Quick Start - Finding Driver IDs for Testing

### Method 1: Using the Helper Script (Recommended)

```bash
# Find nearby drivers at a specific location
./linux-run/get-nearby-drivers.sh 10.762622 106.660172

# With custom radius and limit
./linux-run/get-nearby-drivers.sh 10.762622 106.660172 5.0 20
```

This will show you:

```
Nearby Drivers:
Driver ID: 550e8400-e29b-41d4-a716-446655440000 | Distance: 234m | Location: (10.763, 106.661)
Driver ID: 6ba7b810-9dad-11d1-80b4-00c04fd430c8 | Distance: 456m | Location: (10.764, 106.662)
```

### Method 2: Direct API Call

```bash
curl -X GET "http://localhost:8083/api/internal/drivers/nearby?lat=10.762622&lng=106.660172&radiusKm=3.0&limit=10" | jq
```

### Method 3: Check Redis After Creating a Trip

```bash
# After creating a trip, check which drivers were notified
docker exec -it redis redis-cli

# In Redis CLI:
KEYS pending_trips:*

# You'll see keys like: pending_trips:550e8400-e29b-41d4-a716-446655440000:uuid-of-trip
# The first UUID is the driver ID
```

### Method 4: Monitor Trip Service Logs

```bash
# Watch the trip-service logs when creating a trip
docker logs -f trip-service

# Look for log entries like:
# "Found 3 nearby drivers for trip <trip-id>: [driver-id-1, driver-id-2, driver-id-3]"
# "Trip <trip-id> created and notification sent to RabbitMQ for drivers: [driver-id-1, driver-id-2, driver-id-3]"
```

---

## Complete Testing Flow

### Step 1: Setup - Make Sure You Have Drivers in the System

First, ensure you have drivers registered and with locations set:

```bash
# Check if there are drivers with locations in Redis
docker exec -it redis redis-cli ZRANGE drivers:locations 0 -1 WITHSCORES
```

If empty, you need to:

1. Register a driver via user-service
2. Set the driver's location via driver-service gRPC

### Step 2: Find Nearby Drivers

```bash
# Use the helper script to find drivers near your pickup location
./linux-run/get-nearby-drivers.sh 10.762622 106.660172
```

**Save one or more Driver IDs from the output!** For example:

```
DRIVER_ID=550e8400-e29b-41d4-a716-446655440000
```

### Step 3: Create a Trip

```bash
# Login as passenger and get JWT token
PASSENGER_TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"passenger@example.com","password":"password"}' | jq -r '.token')

# Create a trip
TRIP_RESPONSE=$(curl -s -X POST http://localhost:8082/api/trips/create \
  -H "Authorization: Bearer $PASSENGER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "pickupLatitude": 10.762622,
    "pickupLongitude": 106.660172,
    "destinationLatitude": 10.777229,
    "destinationLongitude": 106.695534,
    "estimatedFare": 50000
  }')

# Extract trip ID
TRIP_ID=$(echo $TRIP_RESPONSE | jq -r '.id')
echo "Trip ID: $TRIP_ID"
```

### Step 4: Check Trip Service Logs

```bash
# View logs to see which drivers were notified
docker logs trip-service | tail -20

# Look for:
# "Found 3 nearby drivers for trip <trip-id>: [driver-id-1, driver-id-2, driver-id-3]"
```

### Step 5: Check Pending Trips for a Driver

```bash
# Replace with actual driver ID from Step 2
DRIVER_ID="550e8400-e29b-41d4-a716-446655440000"

curl -X GET "http://localhost:8083/api/drivers/trips/pending?driverId=${DRIVER_ID}" | jq
```

**Expected Response:**

```json
[
  {
    "tripId": "uuid-of-trip",
    "passengerId": "uuid-of-passenger",
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

### Step 6: Accept the Trip (Within 15 Seconds!)

```bash
# Replace with your actual driver ID and trip ID
curl -X POST "http://localhost:8083/api/drivers/trips/${TRIP_ID}/accept?driverId=${DRIVER_ID}" | jq
```

**Expected Response (Success):**

```json
{
  "tripId": "uuid-of-trip",
  "driverId": "uuid-of-driver",
  "accepted": true,
  "message": "Trip accepted successfully"
}
```

**If you wait more than 15 seconds:**

```json
{
  "tripId": "uuid-of-trip",
  "driverId": "uuid-of-driver",
  "accepted": false,
  "message": "Trip notification has expired"
}
```

### Step 7: Verify Other Drivers Can't Accept

```bash
# Try with a different driver ID (should fail)
DRIVER_ID_2="6ba7b810-9dad-11d1-80b4-00c04fd430c8"

curl -X POST "http://localhost:8083/api/drivers/trips/${TRIP_ID}/accept?driverId=${DRIVER_ID_2}" | jq
```

**Expected Response:**

```json
{
  "tripId": "uuid-of-trip",
  "driverId": "uuid-of-driver-2",
  "accepted": false,
  "message": "Trip already accepted by another driver"
}
```

---

## Monitoring & Debugging

### Check RabbitMQ

```bash
# Open browser
open http://localhost:15672
# Login: guest / guest

# Or via API
curl -s -u guest:guest http://localhost:15672/api/queues/%2F/trip.notification.queue | jq
```

### Check Redis

```bash
# See all pending trips
docker exec -it redis redis-cli KEYS "pending_trips:*"

# Get specific notification (replace with actual key)
docker exec -it redis redis-cli GET "pending_trips:550e8400-e29b-41d4-a716-446655440000:uuid-of-trip"

# Check TTL (should be 15 seconds or less)
docker exec -it redis redis-cli TTL "pending_trips:550e8400-e29b-41d4-a716-446655440000:uuid-of-trip"
```

### View Driver Service Logs

```bash
# Real-time logs
docker logs -f driver-service

# Look for:
# "Received trip notification from RabbitMQ: tripId=..., passengerId=..."
# "Stored pending trip notification for driver xxx: tripId=..., expiresAt=..."
# "Found 2 pending trips for driver xxx"
# "Driver xxx accepted trip yyy"
```

### View Trip Service Logs

```bash
# Real-time logs
docker logs -f trip-service

# Look for:
# "Found 3 nearby drivers for trip xxx: [id1, id2, id3]"
# "Trip xxx created and notification sent to RabbitMQ for drivers: [id1, id2, id3]"
```

---

## Common Issues & Solutions

### Issue: "No pending trips found for driver"

**Possible causes:**

1. No drivers nearby at that location
2. Trip notification expired (>15 seconds)
3. Wrong driver ID

**Solution:**

```bash
# Check if drivers exist at location
./linux-run/get-nearby-drivers.sh 10.762622 106.660172

# Check Redis directly
docker exec -it redis redis-cli KEYS "pending_trips:*"

# Check driver-service logs
docker logs driver-service | grep "Stored pending trip"
```

### Issue: "Trip notification not found or has expired"

**Cause:** More than 15 seconds passed since trip creation

**Solution:**

- Create a new trip
- Accept within 15 seconds

### Issue: Can't find any nearby drivers

**Cause:** No drivers have set their location

**Solution:**

```bash
# Set a driver's location via gRPC
grpcurl -plaintext -d '{"driver_id":"your-driver-uuid","latitude":10.762622,"longitude":106.660172}' \
  localhost:9092 driver.DriverService/UpdateDriverLocation

# Verify in Redis
docker exec -it redis redis-cli ZRANGE drivers:locations 0 -1 WITHSCORES
```

---

## Test Scenarios

### Scenario 1: Normal Accept Flow

1. Create trip → 2 drivers nearby
2. Check pending trips for Driver A → 1 trip found
3. Driver A accepts within 15 seconds → Success
4. Check pending trips for Driver B → Trip removed
5. Driver B tries to accept → "Already accepted"

### Scenario 2: Timeout Flow

1. Create trip → 2 drivers nearby
2. Wait 16 seconds
3. Check pending trips for Driver A → Empty list
4. Driver A tries to accept → "Expired"

### Scenario 3: Decline Flow

1. Create trip → 2 drivers nearby
2. Driver A declines
3. Driver A's notification still removed (for now)
4. Driver B can still accept (within 15 seconds)

---

## Quick Reference Commands

```bash
# 1. Find nearby drivers
./linux-run/get-nearby-drivers.sh 10.762622 106.660172

# 2. Create trip and capture ID
TRIP_ID=$(curl -s -X POST http://localhost:8082/api/trips/create \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{...}' | jq -r '.id')

# 3. Check pending for driver
curl "http://localhost:8083/api/drivers/trips/pending?driverId=<DRIVER_ID>" | jq

# 4. Accept trip
curl -X POST "http://localhost:8083/api/drivers/trips/<TRIP_ID>/accept?driverId=<DRIVER_ID>" | jq

# 5. Check Redis keys
docker exec -it redis redis-cli KEYS "pending_trips:*"

# 6. Monitor logs
docker logs -f trip-service
docker logs -f driver-service
```

---

## Summary

**To answer your question:** The driver IDs that will receive the notification are:

1. **Listed in trip-service logs** when the trip is created
2. **Found via the nearby drivers API** at the pickup location
3. **Stored in Redis** with keys like `pending_trips:<driver-id>:<trip-id>`

Use the `get-nearby-drivers.sh` script to quickly find which drivers will be notified before creating a trip!
