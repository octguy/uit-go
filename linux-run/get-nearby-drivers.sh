#!/bin/bash

# Helper Script: Get Nearby Drivers for Testing
# This script helps you find driver IDs that will receive trip notifications

set -e

# Configuration
DRIVER_SERVICE_PORT=8083

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "=================================================="
echo "Get Nearby Drivers - Helper Script"
echo "=================================================="
echo ""

# Check coordinates
if [ -z "$1" ] || [ -z "$2" ]; then
    echo -e "${YELLOW}Usage: $0 <latitude> <longitude> [radius_km] [limit]${NC}"
    echo ""
    echo "Example coordinates (District 1, Ho Chi Minh City):"
    echo "  Pickup: 10.762622, 106.660172"
    echo "  Destination: 10.777229, 106.695534"
    echo ""
    echo "Example usage:"
    echo "  $0 10.762622 106.660172"
    echo "  $0 10.762622 106.660172 5.0 10"
    exit 1
fi

LATITUDE=$1
LONGITUDE=$2
RADIUS=${3:-3.0}
LIMIT=${4:-10}

echo -e "${BLUE}Searching for drivers near:${NC}"
echo "  Latitude: $LATITUDE"
echo "  Longitude: $LONGITUDE"
echo "  Radius: ${RADIUS} km"
echo "  Limit: $LIMIT drivers"
echo ""

# Call the API
RESPONSE=$(curl -s "http://localhost:${DRIVER_SERVICE_PORT}/api/internal/drivers/nearby?lat=${LATITUDE}&lng=${LONGITUDE}&radiusKm=${RADIUS}&limit=${LIMIT}")

echo -e "${GREEN}Nearby Drivers:${NC}"
echo "$RESPONSE" | jq -r '.[] | "Driver ID: \(.driverId) | Distance: \(.distanceInMeters)m | Location: (\(.latitude), \(.longitude))"' || echo "$RESPONSE"

echo ""
echo -e "${YELLOW}To test trip notification with these drivers:${NC}"
echo ""
echo "1. Copy any Driver ID from above"
echo "2. Create a trip at this location"
echo "3. Check pending trips for that driver:"
echo "   curl -X GET 'http://localhost:8083/api/drivers/trips/pending?driverId=<DRIVER_ID>'"
echo ""
echo "4. Accept the trip within 15 seconds:"
echo "   curl -X POST 'http://localhost:8083/api/drivers/trips/<TRIP_ID>/accept?driverId=<DRIVER_ID>'"
echo ""
