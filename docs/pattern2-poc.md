# Pattern 2 Proof of Concept - REST + gRPC Hybrid

## Overview
Demonstrate inter-service communication using REST (client-facing) and gRPC (service-to-service).

## Selected Functions

### Function 1: Create Trip (Trip Service)
- **REST Endpoint**: `POST /api/trips`
- **Port**: 8082
- **Database**: Insert trip record
- **Logic**: Create trip + validate passenger exists

### Function 2: Validate User (User Service) 
- **gRPC Method**: `validateUser(userId)`
- **Port**: 50051
- **Database**: Query user by ID
- **Logic**: Check user exists and return user type

## Communication Flow

```
Client (REST) → Trip Service → User Service (gRPC) → Database
     ↓              ↓              ↓                    ↓
POST /trips    validateUser    Query users table    Return validation
     ↓              ↓              ↓                    ↓
              Insert trip     Return response      Return trip data
```

## Implementation Steps

1. **Trip Service (Spring Boot)**
   - Implement `createTrip()` REST endpoint
   - Add gRPC client to call User Service
   - Insert trip to database after validation

2. **User Service (Go gRPC)**
   - Implement gRPC server on port 50051
   - Add `validateUser()` method with database query
   - Return user validation response

3. **Logging Points**
   - REST request received
   - gRPC call initiated  
   - Database operations
   - Response sent

## Success Criteria
- Client calls REST, Trip Service calls User Service via gRPC
- Both services query databases
- Clear logging shows communication flow
- Proper response returned to client

## Tech Stack
- **Trip Service**: Spring Boot + REST + gRPC Client
- **User Service**: Go + gRPC Server + PostgreSQL
- **Communication**: HTTP REST + gRPC
- **Databases**: PostgreSQL (separate per service)