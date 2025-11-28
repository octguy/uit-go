# How to Find Driver IDs for Testing

## TL;DR - Quick Answer

When you create a trip, the **trip-service logs will show you which drivers are notified**:

```bash
# Watch the logs when creating a trip
docker logs -f trip-service

# You'll see:
# "Found 3 nearby drivers for trip <trip-id>: [driver-id-1, driver-id-2, driver-id-3]"
# "Trip <trip-id> created and notification sent to RabbitMQ for drivers: [driver-id-1, driver-id-2, driver-id-3]"
```

**Copy any of those driver IDs** and use them to test the endpoint:

```bash
curl -X GET "http://localhost:8083/api/drivers/trips/pending?driverId=<DRIVER_ID>"
```

---

## 4 Ways to Find Driver IDs

### 1. Use the Helper Script (Easiest)

```bash
./linux-run/get-nearby-drivers.sh 10.762622 106.660172
```

This shows all nearby drivers and their IDs.

### 2. Check Trip Service Logs After Creating Trip

```bash
docker logs trip-service | grep "nearby drivers"
```

### 3. Direct API Call

```bash
curl "http://localhost:8083/api/internal/drivers/nearby?lat=10.762622&lng=106.660172&radiusKm=3.0&limit=10" | jq
```

### 4. Check Redis Keys

```bash
docker exec -it redis redis-cli KEYS "pending_trips:*"
# Keys are formatted as: pending_trips:<DRIVER_ID>:<TRIP_ID>
# The first UUID is the driver ID
```

---

## Complete Test Example

```bash
# Step 1: Find nearby drivers
./linux-run/get-nearby-drivers.sh 10.762622 106.660172

# Output example:
# Driver ID: 550e8400-e29b-41d4-a716-446655440000 | Distance: 234m
# Driver ID: 6ba7b810-9dad-11d1-80b4-00c04fd430c8 | Distance: 456m

# Step 2: Set driver ID
DRIVER_ID="550e8400-e29b-41d4-a716-446655440000"

# Step 3: Create a trip (you'll need a passenger JWT token)
# ... create trip and get TRIP_ID ...

# Step 4: Check pending trips for that driver
curl -X GET "http://localhost:8083/api/drivers/trips/pending?driverId=${DRIVER_ID}" | jq

# Step 5: Accept the trip (within 15 seconds!)
curl -X POST "http://localhost:8083/api/drivers/trips/${TRIP_ID}/accept?driverId=${DRIVER_ID}" | jq
```

---

## Key Changes Made

The system now:

1. **Logs driver IDs** when trips are created
2. **Stores notifications per driver** in Redis with keys: `pending_trips:<driver-id>:<trip-id>`
3. **Filters pending trips** by driver ID when queried
4. **Removes notifications for other drivers** when one accepts

See [TESTING_TRIP_NOTIFICATIONS.md](./TESTING_TRIP_NOTIFICATIONS.md) for complete testing guide.
