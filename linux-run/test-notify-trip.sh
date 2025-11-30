#!/bin/bash

# Auto Test Script: Login, Create Trip, Get Driver Notifications
# This script automates the complete flow of creating a trip and finding which drivers are notified

set -e

# Configuration
USER_SERVICE_PORT=8080
TRIP_SERVICE_PORT=8080
DRIVER_SERVICE_PORT=8080

# Default test credentials
PASSENGER_EMAIL="${PASSENGER_EMAIL:-user1@gmail.com}"
PASSENGER_PASSWORD="${PASSENGER_PASSWORD:-123456}"

# Default trip coordinates (District 1, Ho Chi Minh City)
PICKUP_LAT="${PICKUP_LAT:-10.762622}"
PICKUP_LNG="${PICKUP_LNG:-106.660172}"
DEST_LAT="${DEST_LAT:-10.777229}"
DEST_LNG="${DEST_LNG:-106.695534}"
FARE="${FARE:-50000}"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo "=========================================================="
echo "  Auto Trip Creation & Driver Notification Test"
echo "=========================================================="
echo ""

# Step 0: Setup - Bring all drivers online and start simulation
echo -e "${BLUE}Step 0: Setting up drivers...${NC}"
echo ""

echo -e "${YELLOW}Bringing all drivers online...${NC}"
ONLINE_RESPONSE=$(curl -s -X POST http://localhost:${DRIVER_SERVICE_PORT}/api/drivers/online-all)
echo "Response: $ONLINE_RESPONSE"
echo ""

echo -e "${YELLOW}Starting driver location simulation...${NC}"
echo "Simulating movement from (10.762622, 106.660172) to (10.776889, 106.700806)"
SIMULATE_RESPONSE=$(curl -s -X POST "http://localhost:8084/api/simulate/start-all?startLat=10.762622&startLng=106.660172&endLat=10.776889&endLng=106.700806&steps=200&delayMillis=1000")
echo "Response: $SIMULATE_RESPONSE"
echo ""

echo -e "${YELLOW}Waiting 10 seconds for drivers to start simulating...${NC}"
for i in {10..1}; do
    echo -ne "\r  ⏳ ${i} seconds remaining... "
    sleep 1
done
echo -e "\r  ${GREEN}✅ Ready!${NC}                    "
echo ""

echo -e "${GREEN}✅ Drivers setup complete!${NC}"
echo ""

echo "=========================================================="

# Step 1: Login as passenger
echo -e "${BLUE}Step 1: Logging in as passenger...${NC}"
echo "Email: $PASSENGER_EMAIL"
echo ""

LOGIN_RESPONSE=$(curl -s -X POST http://localhost:${USER_SERVICE_PORT}/api/users/login \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${PASSENGER_EMAIL}\",\"password\":\"${PASSENGER_PASSWORD}\"}")

# Check if login was successful
if echo "$LOGIN_RESPONSE" | jq -e '.accessToken' > /dev/null 2>&1; then
    TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken')
    echo -e "${GREEN}✅ Login successful!${NC}"
    echo "Access Token: ${TOKEN:0:50}..."
    echo "Token: ${TOKEN:0:50}..."
else
    echo -e "${RED}❌ Login failed!${NC}"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi

echo ""
echo "=========================================================="

# Step 2: Find nearby drivers before creating trip
echo -e "${BLUE}Step 2: Finding nearby drivers at pickup location...${NC}"
echo "Pickup Location: ($PICKUP_LAT, $PICKUP_LNG)"
echo ""

NEARBY_DRIVERS=$(curl -s "http://localhost:8083/api/internal/drivers/nearby?lat=${PICKUP_LAT}&lng=${PICKUP_LNG}&radiusKm=3.0&limit=10")

if echo "$NEARBY_DRIVERS" | jq -e '. | length' > /dev/null 2>&1; then
    DRIVER_COUNT=$(echo "$NEARBY_DRIVERS" | jq '. | length')
    echo -e "${GREEN}✅ Found $DRIVER_COUNT nearby driver(s)${NC}"
    echo ""
    echo "$NEARBY_DRIVERS" | jq -r '.[] | "  • Driver ID: \(.driverId)\n    Distance: \(.distanceInMeters)m | Location: (\(.latitude), \(.longitude))"'
    
    # Extract first driver ID for testing
    FIRST_DRIVER_ID=$(echo "$NEARBY_DRIVERS" | jq -r '.[0].driverId')
    echo ""
    echo -e "${YELLOW}Test Driver ID: $FIRST_DRIVER_ID${NC}"
else
    echo -e "${YELLOW}⚠️  No nearby drivers found or API error${NC}"
    echo "Response: $NEARBY_DRIVERS"
fi

echo ""
echo "=========================================================="

# Step 3: Create trip
echo -e "${BLUE}Step 3: Creating trip...${NC}"
echo "Pickup: ($PICKUP_LAT, $PICKUP_LNG)"
echo "Destination: ($DEST_LAT, $DEST_LNG)"
echo "Estimated Fare: $FARE VND"
echo ""

TRIP_RESPONSE=$(curl -s -X POST http://localhost:${TRIP_SERVICE_PORT}/api/trips/create \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{
    \"pickupLatitude\": ${PICKUP_LAT},
    \"pickupLongitude\": ${PICKUP_LNG},
    \"destinationLatitude\": ${DEST_LAT},
    \"destinationLongitude\": ${DEST_LNG},
    \"estimatedFare\": ${FARE}
  }")

# Check if trip was created successfully
if echo "$TRIP_RESPONSE" | jq -e '.id' > /dev/null 2>&1; then
    TRIP_ID=$(echo "$TRIP_RESPONSE" | jq -r '.id')
    TRIP_STATUS=$(echo "$TRIP_RESPONSE" | jq -r '.status')
    echo -e "${GREEN}✅ Trip created successfully!${NC}"
    echo "Trip ID: $TRIP_ID"
    echo "Status: $TRIP_STATUS"
else
    echo -e "${RED}❌ Trip creation failed!${NC}"
    echo "Response: $TRIP_RESPONSE"
    exit 1
fi

echo ""
echo "=========================================================="

# Step 4: Wait a moment for message to propagate through RabbitMQ
echo -e "${BLUE}Step 4: Waiting for RabbitMQ to process notification...${NC}"
sleep 1
echo -e "${GREEN}✅ Ready${NC}"

echo ""
echo "=========================================================="

# Step 5: Check trip-service logs to see which driver (nearest) was notified
echo -e "${BLUE}Step 5: Checking trip-service logs for notified driver...${NC}"
echo ""

TRIP_LOGS=$(docker logs trip-service 2>&1 | grep "Trip $TRIP_ID" | tail -5)

NOTIFIED_DRIVER_ID=""
if [ -n "$TRIP_LOGS" ]; then
    echo "$TRIP_LOGS"
    echo ""
    
    # Extract the single nearest driver ID from logs
    NOTIFIED_DRIVER_ID=$(echo "$TRIP_LOGS" | grep -oE 'nearest driver: [a-f0-9-]+' | grep -oE '[a-f0-9-]+$' | head -1)
    if [ -n "$NOTIFIED_DRIVER_ID" ]; then
        echo -e "${GREEN}✅ Nearest driver notified: $NOTIFIED_DRIVER_ID${NC}"
    else
        # Fallback to old format if needed
        NOTIFIED_DRIVER_ID=$(echo "$TRIP_LOGS" | grep -oE '\[[a-f0-9-]+\]' | grep -oE '[a-f0-9-]+' | head -1)
        if [ -n "$NOTIFIED_DRIVER_ID" ]; then
            echo -e "${GREEN}✅ Driver notified: $NOTIFIED_DRIVER_ID${NC}"
        fi
    fi
else
    echo -e "${YELLOW}⚠️  Could not find specific log entries (logs may have rotated)${NC}"
fi

echo ""
echo "=========================================================="

# Step 6: Verify that ONLY the nearest driver received the notification
echo -e "${BLUE}Step 6: Verifying nearest driver received notification...${NC}"
echo ""

if [ -n "$FIRST_DRIVER_ID" ]; then
    echo -e "${YELLOW}Expected nearest driver: $FIRST_DRIVER_ID${NC}"
    if [ -n "$NOTIFIED_DRIVER_ID" ]; then
        echo -e "${YELLOW}Actually notified driver: $NOTIFIED_DRIVER_ID${NC}"
        echo ""
        
        if [ "$FIRST_DRIVER_ID" = "$NOTIFIED_DRIVER_ID" ]; then
            echo -e "${GREEN}✅ CORRECT: Nearest driver was notified!${NC}"
        else
            echo -e "${RED}❌ MISMATCH: Expected $FIRST_DRIVER_ID but $NOTIFIED_DRIVER_ID was notified${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  Could not determine which driver was notified from logs${NC}"
    fi
    echo ""
    
    # Check pending notification for the nearest driver
    echo -e "${YELLOW}Checking pending trips for nearest driver: $FIRST_DRIVER_ID${NC}"
    PENDING_TRIPS=$(curl -s "http://localhost:${DRIVER_SERVICE_PORT}/api/drivers/trips/pending?driverId=${FIRST_DRIVER_ID}")
    
    if echo "$PENDING_TRIPS" | jq -e '. | length' > /dev/null 2>&1; then
        PENDING_COUNT=$(echo "$PENDING_TRIPS" | jq '. | length')
        if [ "$PENDING_COUNT" -gt 0 ]; then
            echo -e "${GREEN}  ✅ Nearest driver has $PENDING_COUNT pending trip(s)${NC}"
            echo "$PENDING_TRIPS" | jq -r '.[] | "    Trip ID: \(.tripId)\n    Fare: \(.estimatedFare) VND\n    Distance: \(.distanceKm) km\n    Expires at: \(.expiresAt)"'
        else
            echo -e "${YELLOW}  ⚠️  No pending trips (may have expired after 15 seconds)${NC}"
        fi
    else
        echo -e "${RED}  ❌ Error checking pending trips${NC}"
    fi
    echo ""
    
    # Check if other drivers received notifications (should be 0)
    if [ "$DRIVER_COUNT" -gt 1 ]; then
        echo -e "${YELLOW}Verifying other drivers did NOT receive notification...${NC}"
        OTHER_NOTIFIED=0
        echo "$NEARBY_DRIVERS" | jq -r '.[1:] | .[].driverId' | while read -r OTHER_DRIVER_ID; do
            PENDING=$(curl -s "http://localhost:${DRIVER_SERVICE_PORT}/api/drivers/trips/pending?driverId=${OTHER_DRIVER_ID}")
            if echo "$PENDING" | jq -e '. | length' > /dev/null 2>&1; then
                COUNT=$(echo "$PENDING" | jq '. | length')
                if [ "$COUNT" -gt 0 ]; then
                    echo -e "${RED}  ❌ Driver $OTHER_DRIVER_ID unexpectedly has $COUNT pending trip(s)${NC}"
                    OTHER_NOTIFIED=$((OTHER_NOTIFIED + 1))
                fi
            fi
        done
        if [ "$OTHER_NOTIFIED" -eq 0 ]; then
            echo -e "${GREEN}  ✅ Other drivers correctly have no pending trips${NC}"
        fi
    fi
else
    echo -e "${YELLOW}⚠️  No nearby drivers to check${NC}"
fi

echo "=========================================================="

# Step 7: Check Redis for pending notifications
echo -e "${BLUE}Step 7: Checking Redis for pending notifications...${NC}"
echo ""

REDIS_KEYS=$(docker exec redis redis-cli KEYS "pending_trips:*:${TRIP_ID}" 2>/dev/null || echo "")

if [ -n "$REDIS_KEYS" ]; then
    echo -e "${GREEN}✅ Found pending notifications in Redis:${NC}"
    echo "$REDIS_KEYS" | while read -r KEY; do
        if [ -n "$KEY" ]; then
            # Extract driver ID from key (format: pending_trips:<driver-id>:<trip-id>)
            DRIVER_ID_FROM_KEY=$(echo "$KEY" | cut -d':' -f2)
            TTL=$(docker exec redis redis-cli TTL "$KEY" 2>/dev/null || echo "0")
            echo "  • Driver: $DRIVER_ID_FROM_KEY | TTL: ${TTL}s | Key: $KEY"
        fi
    done
else
    echo -e "${YELLOW}⚠️  No pending notifications found in Redis (may have expired after 15 seconds)${NC}"
fi

echo ""
echo "=========================================================="

# Summary
echo -e "${GREEN}SUMMARY${NC}"
echo "=========================================================="
echo "Trip ID: $TRIP_ID"
echo "Trip Status: $TRIP_STATUS"
echo "Nearby Drivers Found: $DRIVER_COUNT"
if [ -n "$FIRST_DRIVER_ID" ]; then
    echo "Nearest Driver ID: $FIRST_DRIVER_ID"
fi
if [ -n "$NOTIFIED_DRIVER_ID" ]; then
    echo "Notified Driver ID: $NOTIFIED_DRIVER_ID"
    if [ "$FIRST_DRIVER_ID" = "$NOTIFIED_DRIVER_ID" ]; then
        echo -e "${GREEN}✅ Verification: Nearest driver was correctly notified${NC}"
    else
        echo -e "${RED}❌ Verification: Driver mismatch detected${NC}"
    fi
fi
echo ""

if [ -n "$NOTIFIED_DRIVER_ID" ]; then
    echo -e "${YELLOW}To test driver acceptance (within 15 seconds of trip creation):${NC}"
    echo ""
    echo "curl -X POST 'http://localhost:${DRIVER_SERVICE_PORT}/api/drivers/trips/${TRIP_ID}/accept?driverId=${NOTIFIED_DRIVER_ID}' | jq"
    echo ""
    echo -e "${YELLOW}To check pending trips for notified driver:${NC}"
    echo ""
    echo "curl -X GET 'http://localhost:${DRIVER_SERVICE_PORT}/api/drivers/trips/pending?driverId=${NOTIFIED_DRIVER_ID}' | jq"
fi

echo ""
echo "=========================================================="
echo -e "${GREEN}✅ Test completed!${NC}"
echo "=========================================================="
echo ""

# Export variables for use in shell
echo "# You can use these variables in your shell:"
echo "export TRIP_ID=\"$TRIP_ID\""
[ -n "$NOTIFIED_DRIVER_ID" ] && echo "export DRIVER_ID=\"$NOTIFIED_DRIVER_ID\""
echo "export PASSENGER_TOKEN=\"$TOKEN\""
