#!/bin/bash

# Trip Notification Feature Test Script
# This script tests the RabbitMQ-based trip notification system

set -e

# Configuration
API_GATEWAY_PORT=8080
TRIP_SERVICE_PORT=8082
DRIVER_SERVICE_PORT=8083

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "=================================================="
echo "Trip Notification System - Test Script"
echo "=================================================="
echo ""

# Test if services are running
echo -e "${YELLOW}Checking if services are running...${NC}"
if ! curl -s -f http://localhost:8082/actuator/health > /dev/null; then
    echo -e "${RED}❌ Trip Service is not running on port 8082${NC}"
    exit 1
fi

if ! curl -s -f http://localhost:8083/actuator/health > /dev/null; then
    echo -e "${RED}❌ Driver Service is not running on port 8083${NC}"
    exit 1
fi

echo -e "${GREEN}✅ All services are running${NC}"
echo ""

# Test RabbitMQ
echo -e "${YELLOW}Checking RabbitMQ...${NC}"
if curl -s -u guest:guest http://localhost:15672/api/overview > /dev/null 2>&1; then
    echo -e "${GREEN}✅ RabbitMQ is accessible at http://localhost:15672${NC}"
    
    # Check if queue exists
    QUEUE_INFO=$(curl -s -u guest:guest http://localhost:15672/api/queues/%2F/trip.notification.queue)
    if echo "$QUEUE_INFO" | grep -q "trip.notification.queue"; then
        echo -e "${GREEN}✅ Queue 'trip.notification.queue' exists${NC}"
    else
        echo -e "${YELLOW}⚠️  Queue 'trip.notification.queue' not found (will be created on first use)${NC}"
    fi
else
    echo -e "${RED}❌ RabbitMQ Management UI is not accessible${NC}"
    echo "   Make sure RabbitMQ is running and management plugin is enabled"
fi
echo ""

# Test Redis
echo -e "${YELLOW}Checking Redis...${NC}"
if docker exec -it redis redis-cli PING 2>/dev/null | grep -q "PONG"; then
    echo -e "${GREEN}✅ Redis is running and accessible${NC}"
else
    echo -e "${RED}❌ Redis is not accessible${NC}"
fi
echo ""

echo "=================================================="
echo "Manual Test Instructions:"
echo "=================================================="
echo ""
echo "1. Login as a passenger and get JWT token:"
echo "   curl -X POST http://localhost:8081/api/auth/login \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -d '{\"email\":\"passenger@example.com\",\"password\":\"password\"}'"
echo ""
echo "2. Create a trip (replace <JWT> with your token):"
echo "   curl -X POST http://localhost:8082/api/trips/create \\"
echo "     -H 'Authorization: Bearer <JWT>' \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -d '{"
echo "       \"pickupLatitude\": 10.762622,"
echo "       \"pickupLongitude\": 106.660172,"
echo "       \"destinationLatitude\": 10.777229,"
echo "       \"destinationLongitude\": 106.695534,"
echo "       \"estimatedFare\": 50000"
echo "     }'"
echo ""
echo "3. Check pending trips for a driver (replace <DRIVER_ID> with actual driver UUID):"
echo "   curl -X GET 'http://localhost:8083/api/drivers/trips/pending?driverId=<DRIVER_ID>'"
echo ""
echo "4. Accept trip within 15 seconds (replace <TRIP_ID> and <DRIVER_ID>):"
echo "   curl -X POST 'http://localhost:8083/api/drivers/trips/<TRIP_ID>/accept?driverId=<DRIVER_ID>'"
echo ""
echo "5. Or decline trip:"
echo "   curl -X POST 'http://localhost:8083/api/drivers/trips/<TRIP_ID>/decline?driverId=<DRIVER_ID>'"
echo ""
echo "6. Monitor RabbitMQ:"
echo "   Open browser: http://localhost:15672"
echo "   Username: guest"
echo "   Password: guest"
echo ""
echo "7. Monitor Redis pending trips:"
echo "   docker exec -it redis redis-cli KEYS 'pending_trips:*'"
echo "   docker exec -it redis redis-cli GET 'pending_trips:<TRIP_ID>'"
echo ""
echo "=================================================="
echo "Expected Behavior:"
echo "=================================================="
echo "- Trip created in trip-service database"
echo "- Notification published to RabbitMQ exchange 'trip.exchange'"
echo "- Driver-service consumes message from 'trip.notification.queue'"
echo "- Notification stored in Redis with 15-second TTL"
echo "- Driver has 15 seconds to accept/decline"
echo "- After 15 seconds, notification automatically expires"
echo "- Only one driver can accept each trip"
echo ""
echo -e "${GREEN}Test script completed!${NC}"
