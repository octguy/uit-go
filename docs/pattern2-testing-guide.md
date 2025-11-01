# 🧪 Pattern 2 POC Testing Guide

## 🚀 **Complete Testing Workflow**

### **Step 1: Start Docker Services**
```bash
cd "d:\Learning\Sem5\CloudComputingAndMicroservices\uit-go"
docker-compose up -d
```

### **Step 2: Check Service Health**
```bash
# Check all services are running
docker-compose ps

# Check Trip Service (Pattern 2 entry point)
curl http://localhost:8082/actuator/health

# Check User gRPC Service (Pattern 2 validation)
curl http://localhost:50051/validateUser -X POST -H "Content-Type: application/json" -d "{\"userId\":\"test\"}"
```

---

## 📊 **Pattern 2 Test Flow**

### **Test Scenario**: Create Trip with User Validation

#### **Flow Overview**:
```
Client → Trip Service (REST) → User Service (gRPC) → Database → Response Chain
```

---

## 🎯 **Test Commands**

### **Test 1: Create Trip (Success Case)**

#### **Step 1**: Insert Test User Data
```bash
# Connect to User Service database
docker exec -it uit-go-user-service-db-1 psql -U user_service_user -d user_service_db

# Insert test user
INSERT INTO users (id, email, password_hash, user_type, name, phone) 
VALUES ('123e4567-e89b-12d3-a456-426614174000', 'john.doe@example.com', 'hashedpassword', 'PASSENGER', 'John Doe', '+1234567890');

# Verify user exists
SELECT * FROM users;

# Exit database
\q
```

#### **Step 2**: Test Pattern 2 Flow
```bash
# Create Trip Request (triggers Pattern 2 workflow)
curl -X POST http://localhost:8082/api/trips/request \
  -H "Content-Type: application/json" \
  -d '{
    "passengerId": "123e4567-e89b-12d3-a456-426614174000",
    "pickupLocation": "University Campus",
    "destination": "Downtown Mall",
    "pickupLatitude": 10.762622,
    "pickupLongitude": 106.660172,
    "destinationLatitude": 10.775818,
    "destinationLongitude": 106.695595
  }'
```

#### **Expected Response**:
```json
{
  "id": "generated-uuid",
  "passengerId": "123e4567-e89b-12d3-a456-426614174000",
  "driverId": null,
  "status": "REQUESTED",
  "pickupLocation": "University Campus",
  "destination": "Downtown Mall",
  "pickupLatitude": 10.762622,
  "pickupLongitude": 106.660172,
  "destinationLatitude": 10.775818,
  "destinationLongitude": 106.695595,
  "fare": null,
  "createdAt": "2025-11-01T10:30:00",
  "updatedAt": "2025-11-01T10:30:00"
}
```

---

### **Test 2: Create Trip (Validation Failure)**

```bash
# Test with non-existent user
curl -X POST http://localhost:8082/api/trips/request \
  -H "Content-Type: application/json" \
  -d '{
    "passengerId": "999e9999-e99b-99d9-a999-999999999999",
    "pickupLocation": "Invalid Location",
    "destination": "Invalid Destination"
  }'
```

#### **Expected Response**:
```json
{
  "error": "User validation failed: User not found"
}
```

---

## 📋 **Log Monitoring**

### **Monitor Pattern 2 Flow Logs**:
```bash
# Watch Trip Service logs for Pattern 2 execution
docker logs -f uit-go-trip-service-1

# Watch User Service logs for gRPC calls
docker logs -f uit-go-user-grpc-service-1
```

### **Expected Log Output**:

#### **Trip Service Logs**:
```
🚀 Pattern 2 POC: Create Trip Request - University Campus → Downtown Mall
📞 Making gRPC call to User Service for userId: 123e4567-e89b-12d3-a456-426614174000
✅ gRPC response received from User Service
✅ User validated via gRPC: PASSENGER
✅ Trip saved to database: [generated-trip-uuid]
```

#### **User Service Logs**:
```
📞 gRPC call: validateUser(userId=123e4567-e89b-12d3-a456-426614174000)
✅ User 123e4567-e89b-12d3-a456-426614174000 validated: PASSENGER
```

---

## 🧪 **Advanced Testing**

### **PowerShell Testing Script**:
```powershell
# Create test script
$headers = @{ "Content-Type" = "application/json" }

# Test 1: Valid user
$body1 = @{
    passengerId = "123e4567-e89b-12d3-a456-426614174000"
    pickupLocation = "Start Point"
    destination = "End Point"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/api/trips/request" -Method Post -Headers $headers -Body $body1

# Test 2: Invalid user
$body2 = @{
    passengerId = "invalid-uuid"
    pickupLocation = "Start Point"  
    destination = "End Point"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/api/trips/request" -Method Post -Headers $headers -Body $body2
```

---

## 🔍 **Database Verification**

### **Check Trip Creation**:
```bash
# Connect to Trip Service database
docker exec -it uit-go-trip-service-db-1 psql -U trip_service_user -d trip_service_db

# Check created trips
SELECT id, passenger_id, pickup_location, destination, status, created_at FROM trips;

# Exit
\q
```

---

## 🎯 **Pattern 2 Success Indicators**

### **✅ Pattern 2 Working Correctly When**:
1. **REST Request**: Successfully received by Trip Service
2. **gRPC Call**: Trip Service calls User Service for validation  
3. **Database Query**: User Service queries PostgreSQL for user
4. **Validation Response**: User Service returns validation result
5. **Trip Creation**: Trip Service creates trip in database
6. **Response Chain**: Final response returned to client

### **📊 Architecture Verification**:
- **HTTP REST**: Client ↔ Trip Service
- **HTTP gRPC-style**: Trip Service ↔ User Service  
- **PostgreSQL**: User Service ↔ User Database
- **PostgreSQL**: Trip Service ↔ Trip Database

---

## 🐞 **Troubleshooting**

### **Common Issues**:
1. **Service Not Ready**: Wait 30 seconds after `docker-compose up`
2. **Database Connection**: Check database containers are healthy
3. **UUID Format**: Ensure valid UUID format in requests
4. **Network Issues**: Verify Docker network connectivity

### **Debug Commands**:
```bash
# Check service connectivity
docker exec uit-go-trip-service-1 curl http://user-grpc-service:50051/validateUser

# Check database connections
docker logs uit-go-user-service-db-1
docker logs uit-go-trip-service-db-1
```

The Pattern 2 POC is now **fully ready for testing**! 🚀