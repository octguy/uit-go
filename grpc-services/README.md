# Go gRPC Services

This directory contains the Go-based gRPC services for inter-service communication in the UIT-Go system.

## Services

- **User Service** (Port: 50051) - User validation and information
- **Driver Service** (Port: 50052) - Driver location and status management  
- **Trip Service** (Port: 50053) - Trip information and status updates

## Structure

```
grpc-services/
├── go.mod                    # Go module definition
├── user-service/            # User gRPC service
│   └── main.go
├── driver-service/          # Driver gRPC service  
│   └── main.go
├── trip-service/            # Trip gRPC service
│   └── main.go
├── Dockerfile.user          # Docker build for user service
├── Dockerfile.driver        # Docker build for driver service
└── Dockerfile.trip          # Docker build for trip service
```

## Protocol Buffers

The protobuf definitions are located in `/shared/protobuf/`:
- `user.proto` - User service definitions
- `driver.proto` - Driver service definitions  
- `trip.proto` - Trip service definitions

## Environment Variables

Each service uses the following environment variables:

### User Service
- `DB_HOST` - Database host (default: localhost)
- `DB_PORT` - Database port (default: 5435)
- `DB_USER` - Database user (default: postgres)
- `DB_PASSWORD` - Database password (default: password)
- `DB_NAME` - Database name (default: user_service)

### Driver Service  
- `DB_HOST` - Database host (default: localhost)
- `DB_PORT` - Database port (default: 5434)
- `DB_USER` - Database user (default: postgres)
- `DB_PASSWORD` - Database password (default: password)
- `DB_NAME` - Database name (default: driver_service)

### Trip Service
- `DB_HOST` - Database host (default: localhost)
- `DB_PORT` - Database port (default: 5433)
- `DB_USER` - Database user (default: postgres)
- `DB_PASSWORD` - Database password (default: password)
- `DB_NAME` - Database name (default: trip_service)

## Running Locally

1. Generate protobuf code:
```bash
# Install protoc and protoc-gen-go if not already installed
go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest

# Generate Go code from proto files (need to implement)
```

2. Install dependencies:
```bash
go mod tidy
```

3. Run services:
```bash
# User service
go run user-service/main.go

# Driver service  
go run driver-service/main.go

# Trip service
go run trip-service/main.go
```

## Docker Build

Build individual services:
```bash
# User service
docker build -f Dockerfile.user -t uit-go-user-grpc .

# Driver service
docker build -f Dockerfile.driver -t uit-go-driver-grpc .

# Trip service
docker build -f Dockerfile.trip -t uit-go-trip-grpc .
```

## Integration

These gRPC services provide inter-service communication for:
- User validation between services
- Real-time driver location updates
- Trip status synchronization
- Cross-service data queries

The Spring Boot services can call these gRPC services for efficient, strongly-typed communication between microservices.