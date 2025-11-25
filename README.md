# UIT-Go - Ride-Hailing Microservices System

A microservices-based ride-hailing platform built with Spring Boot, gRPC, PostgreSQL, Redis, and Docker.

## Table of Contents

- [System Overview](#system-overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Installation](#installation)
- [Running the System](#running-the-system)
- [Service Endpoints](#service-endpoints)
- [Database Access](#database-access)
- [API Testing](#api-testing)
- [Troubleshooting](#troubleshooting)
- [Development Workflow](#development-workflow)

## System Overview

UIT-Go is a comprehensive ride-hailing microservices system implementing modern cloud-native patterns with hybrid communication protocols (REST + gRPC).

### Microservices

- **User Service** (Port 8081) - User management, authentication, and authorization
- **Trip Service** (Port 8082) - Trip booking, fare calculation, and trip history
- **Driver Service** (Port 8083) - Driver management, availability tracking, and geospatial queries
- **Driver Simulator** (Port 8084) - Real-time driver location simulation
- **API Gateway** (Port 8080) - Unified entry point with intelligent routing

### Infrastructure Components

- **PostgreSQL** - Isolated databases for User Service (5435) and Trip Service (5433)
- **Redis** - Caching and geospatial data for driver locations (Port 6379)
- **Docker** - Complete containerization with Docker Compose orchestration
- **gRPC** - High-performance inter-service communication for real-time features

## Architecture

### Communication Patterns

- **REST APIs**: Client-facing endpoints via API Gateway
- **gRPC**: High-performance inter-service communication (Driver Service)
- **OpenFeign**: Declarative HTTP client for service-to-service calls
- **Redis GEO Commands**: Geospatial queries for driver location tracking

## Prerequisites

### Required for Docker-based local run

- **Docker Desktop** 20.10+ (with Docker Compose) and at least **4GB** memory allocated
  ```bash
  docker --version
  docker compose version  # or docker-compose --version
  ```
- **Git** - To clone the repository

### Optional (for local development outside Docker)

- **Java 17** or higher
  ```bash
  java -version
  ```
- **Maven 3.6+** (Maven Wrapper included in each service)
- **Postman** or **curl** - API testing
- **psql** or **DBeaver** - Database management
- **Redis CLI** - Redis inspection and debugging

## Project Structure

```
uit-go/
├── backend/
│   ├── api-gateway/        # Spring Cloud Gateway (Port 8080)
│   │   ├── src/
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── mvnw/mvnw.cmd
│   ├── user-service/       # User management (Port 8081)
│   │   ├── src/main/java/com/example/user_service/
│   │   │   ├── controller/  # REST controllers
│   │   │   ├── service/     # Business logic
│   │   │   ├── repository/  # Data access
│   │   │   ├── entity/      # JPA entities
│   │   │   ├── jwt/         # JWT authentication
│   │   │   └── config/      # Security & CORS config
│   │   ├── pom.xml
│   │   └── Dockerfile
│   ├── trip-service/       # Trip management (Port 8082)
│   │   ├── src/main/java/com/example/trip_service/
│   │   │   ├── controller/  # REST controllers
│   │   │   ├── service/     # Business logic
│   │   │   ├── repository/  # Data access
│   │   │   ├── entity/      # JPA entities
│   │   │   └── config/      # OpenFeign clients
│   │   ├── pom.xml
│   │   └── Dockerfile
│   ├── driver-service/     # Driver management (Port 8083)
│   │   ├── src/main/
│   │   │   ├── java/com/example/driverservice/
│   │   │   │   ├── controller/  # REST controllers
│   │   │   │   ├── service/     # Business logic
│   │   │   │   ├── grpc/        # gRPC service implementation
│   │   │   │   └── config/      # Redis & gRPC config
│   │   │   └── proto/       # Protocol Buffer definitions
│   │   ├── pom.xml
│   │   └── Dockerfile
│   └── driver-simulator/   # Location simulation (Port 8084)
│       ├── src/main/java/com/example/driversimulator/
│       │   ├── controller/  # Simulator REST API
│       │   ├── simulate/    # Path generation logic
│       │   └── config/      # gRPC client config
│       ├── pom.xml
│       └── Dockerfile
├── infra/
│   └── docker-compose.yml  # Complete Docker orchestration
├── schema/                 # Database initialization scripts
│   ├── user-schema.sql
│   └── trip-schema.sql
├── linux-run/              # macOS/Linux automation scripts
│   ├── start.sh           # Quick start all services
│   └── stop.sh            # Stop all containers
├── win-run/                # Windows automation scripts
│   ├── build-sequential.bat
│   ├── rebuild-all.bat
│   ├── restart-docker.bat
│   └── demo-service-integration.bat
└── docs/                   # Comprehensive documentation
    ├── architecture.md
    ├── interfaces.md
    ├── pattern2-testing-guide.md
    ├── pattern2-implementation-status.md
    └── redis-grpc-testing-commands.md
```

## Installation

Follow these steps to install and run everything locally with Docker (no host-side Java/Maven needed):

1. **Clone the repository**
   ```bash
   git clone https://github.com/octguy/uit-go.git
   cd uit-go
   ```
2. **Start Docker Desktop** and confirm it is running:
   ```bash
   docker ps
   ```
3. **Run the automated Docker build + start**
   - macOS/Linux:
     ```bash
     cd linux-run
     chmod +x start.sh stop.sh
     ./start.sh
     ```
   - Windows (PowerShell or Command Prompt):
     ```cmd
     cd win-run
     rebuild-all.bat
     ```
   These scripts will stop any old containers, build all service images (using the Maven wrapper inside the Docker build), and start the full stack.
4. **Verify the stack**
   ```bash
   cd infra
   docker-compose ps          # container status
   docker-compose logs --tail=50 api-gateway  # sample logs
   ```
5. **Stop when finished**
   ```bash
   cd infra
   docker-compose down        # keep data volumes
   # or to reset everything (including Postgres/Redis data):
   docker-compose down -v
   ```

> If you prefer to build outside Docker, the Maven wrapper lives under each service (e.g., `backend/user-service/mvnw`).

## Running the System

### Quick Start with Docker (Recommended)

```bash
# macOS/Linux
cd linux-run && ./start.sh

# Windows
cd win-run && rebuild-all.bat
```

What this does:
- Stops any existing UIT-Go containers
- Builds fresh images for every service
- Starts the full stack with Docker Compose
- Prints running containers and key endpoints

### Manual Start with Docker Compose

```bash
cd infra
docker-compose up -d --build   # build images and start
docker-compose ps              # check status
docker-compose logs -f         # tail all logs
docker-compose logs -f user-service  # tail one service
docker-compose down            # stop (keeps data)
docker-compose down -v         # stop and wipe data volumes
```

### Individual Service Development

Run a single service locally (without Docker) for development:

```bash
# Navigate to service directory
cd backend/user-service

# Run with Maven wrapper (macOS/Linux)
./mvnw spring-boot:run

# Run with Maven wrapper (Windows)
mvnw.cmd spring-boot:run
```

**Note**: When running services locally, ensure:

- PostgreSQL databases are accessible (via Docker or local installation)
- Redis is running (for Driver Service)
- Update `application.properties` with correct connection strings

## Service Endpoints

### Service Ports

| Service          | HTTP Port | gRPC Port | URL                   | Description                          |
| ---------------- | --------- | --------- | --------------------- | ------------------------------------ |
| API Gateway      | 8080      | -         | http://localhost:8080 | Unified entry point for all requests |
| User Service     | 8081      | -         | http://localhost:8081 | User management & authentication     |
| Trip Service     | 8082      | -         | http://localhost:8082 | Trip booking & management            |
| Driver Service   | 8083      | 9092      | http://localhost:8083 | Driver management & geospatial       |
| Driver Simulator | 8084      | -         | http://localhost:8084 | Real-time driver location simulation |

### Health Checks

Verify all services are running:

```bash
# API Gateway
curl http://localhost:8080/health

# User Service
curl http://localhost:8081/actuator/health

# Trip Service
curl http://localhost:8082/actuator/health

# Driver Service
curl http://localhost:8083/actuator/health

# Driver Simulator
curl http://localhost:8084/actuator/health
```

### Key API Endpoints (via API Gateway)

#### User Management

```bash
POST   http://localhost:8080/api/users/register       # Register new user
POST   http://localhost:8080/api/users/login          # User login
GET    http://localhost:8080/api/users/profile        # Get profile
PUT    http://localhost:8080/api/users/profile        # Update profile
```

#### Trip Management

```bash
POST   http://localhost:8080/api/trips/request        # Request new trip
GET    http://localhost:8080/api/trips/{id}           # Get trip details
PUT    http://localhost:8080/api/trips/{id}/cancel    # Cancel trip
GET    http://localhost:8080/api/trips/history        # Trip history
```

#### Driver Management

```bash
POST   http://localhost:8080/api/drivers/register     # Register driver
PUT    http://localhost:8080/api/drivers/status       # Update availability
GET    http://localhost:8080/api/drivers/nearby       # Find nearby drivers
PUT    http://localhost:8080/api/drivers/location     # Update location
```

#### Driver Simulator

```bash
POST   http://localhost:8084/api/simulate/start       # Start simulation
POST   http://localhost:8084/api/simulate/stop        # Stop simulation
GET    http://localhost:8084/api/simulate/status      # Get simulation status
```

## Database Access

Each service uses its own PostgreSQL database following the microservices database-per-service pattern.

### Database Configuration

| Service      | Database Name   | Username          | Password          | Port | Container Name  |
| ------------ | --------------- | ----------------- | ----------------- | ---- | --------------- |
| User Service | user_service_db | user_service_user | user_service_pass | 5435 | user-service-db |
| Trip Service | trip_service_db | trip_service_user | trip_service_pass | 5433 | trip-service-db |

### Connecting via psql

```bash
# User Service Database
psql -h localhost -p 5435 -U user_service_user -d user_service_db
# Password: user_service_pass

# Trip Service Database
psql -h localhost -p 5433 -U trip_service_user -d trip_service_db
# Password: trip_service_pass
```

### Connecting via Docker

```bash
# User Service Database
docker exec -it user-service-db psql -U user_service_user -d user_service_db

# Trip Service Database
docker exec -it trip-service-db psql -U trip_service_user -d trip_service_db
```

### Connecting via GUI Tools (DBeaver, pgAdmin, DataGrip)

Create a new PostgreSQL connection with:

- **Host:** localhost
- **Port:** 5435 (User Service) or 5433 (Trip Service)
- **Database:** user_service_db or trip_service_db
- **Username:** See table above
- **Password:** See table above

### Redis Access

Driver Service uses Redis for geospatial data and caching.

```bash
# Connect to Redis CLI
docker exec -it redis redis-cli

# Test connection
PING  # Should return PONG

# Check driver locations (example)
GEORADIUS drivers:locations 106.660172 10.762622 5 km

# View all keys
KEYS *
```

## API Testing

### Example: User Registration and Authentication

```bash
# 1. Register a new user
curl -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "SecurePass123",
    "name": "John Doe",
    "phone": "+1234567890",
    "userType": "PASSENGER"
  }'

# 2. Login to get JWT token
curl -X POST http://localhost:8081/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "SecurePass123"
  }'

# 3. Use token for authenticated requests
TOKEN="your-jwt-token-here"
curl -X GET http://localhost:8081/api/users/profile \
  -H "Authorization: Bearer $TOKEN"
```

### Example: Trip Creation

```bash
# Request a new trip
curl -X POST http://localhost:8082/api/trips/request \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "passengerId": "123e4567-e89b-12d3-a456-426614174000",
    "pickupLocation": "University Campus",
    "destination": "Downtown Mall",
    "pickupLatitude": 10.762622,
    "pickupLongitude": 106.660172,
    "destinationLatitude": 10.775818,
    "destinationLongitude": 106.695595
  }'

# Get trip details
curl http://localhost:8082/api/trips/{trip-id} \
  -H "Authorization: Bearer $TOKEN"
```

### Example: Driver Location Simulation

```bash
# Start driver simulation
curl -X POST http://localhost:8084/api/simulate/start \
  -H "Content-Type: application/json" \
  -d '{
    "driverId": "driver-001",
    "startLat": 10.762622,
    "startLng": 106.660172,
    "endLat": 10.775818,
    "endLng": 106.695595,
    "speedKmh": 40
  }'

# Find nearby drivers
curl -X GET "http://localhost:8083/api/driver-service/nearby?latitude=10.762622&longitude=106.660172&radius=5"
```

For comprehensive API testing examples, see:

- [docs/pattern2-testing-guide.md](docs/pattern2-testing-guide.md)
- [docs/redis-grpc-testing-commands.md](docs/redis-grpc-testing-commands.md)

## Troubleshooting

### Common Issues

#### 1. Port Already in Use

If you encounter port conflicts:

```bash
# Check what's using a port (macOS/Linux)
lsof -i :8081

# Check on Windows
netstat -ano | findstr :8081

# Kill the process (macOS/Linux)
kill -9 <PID>

# Kill the process (Windows)
taskkill /PID <PID> /F
```

Or modify ports in `docker-compose.yml`:

```yaml
ports:
  - "8085:8081" # Map external port 8085 to internal 8081
```

#### 2. Docker Build Fails

```bash
# Clean Docker system
docker system prune -a -f

# Remove volumes
docker volume prune -f

# Rebuild from scratch
cd infra
docker-compose down -v
docker-compose up --build --force-recreate
```

#### 3. Maven Build Fails

```bash
# Clean and rebuild specific service
cd backend/user-service
./mvnw clean install -DskipTests

# Force update dependencies
./mvnw clean install -U

# Clear Maven cache (if corrupted)
rm -rf ~/.m2/repository
```

#### 4. Services Won't Start

```bash
# Check Docker container logs
docker-compose logs user-service
docker-compose logs trip-service-db

# Check all container status
docker-compose ps

# Restart specific service
docker-compose restart user-service

# Rebuild specific service
docker-compose up -d --build user-service
```

#### 5. Database Connection Issues

**Symptoms**: Service starts but can't connect to database

**Solutions**:

```bash
# Check if database containers are running
docker-compose ps

# Check database logs
docker-compose logs user-service-db

# Verify database credentials in application.properties match docker-compose.yml

# Wait for database to be ready (health checks)
docker-compose up -d --wait

# Restart database containers
docker-compose restart user-service-db trip-service-db
```

#### 6. Redis Connection Issues

```bash
# Check Redis is running
docker-compose ps redis

# Test Redis connection
docker exec -it redis redis-cli ping
# Should return: PONG

# Check Redis logs
docker-compose logs redis

# Clear Redis data
docker exec -it redis redis-cli FLUSHALL
```

#### 7. gRPC Communication Failures

**For Driver Service gRPC**:

```bash
# Check if gRPC port 9092 is accessible
telnet localhost 9092

# Check Driver Service logs
docker-compose logs driver-service

# Verify gRPC stub configuration in client services
# Check GrpcClientConfig.java in driver-simulator
```

#### 8. Out of Memory Errors

```bash
# Increase Docker memory allocation
# Docker Desktop > Settings > Resources > Memory (recommend 4GB+)

# Set JVM heap size in Dockerfile
ENV JAVA_OPTS="-Xmx512m -Xms256m"
```

#### 9. Permission Denied (macOS/Linux)

```bash
# Make scripts executable
cd linux-run
chmod +x *.sh

# Or run with bash explicitly
bash start.sh
```

### Checking Service Health

```bash
# Quick health check all services
curl http://localhost:8080/health        # API Gateway
curl http://localhost:8081/actuator/health  # User Service
curl http://localhost:8082/actuator/health  # Trip Service
curl http://localhost:8083/actuator/health  # Driver Service
curl http://localhost:8084/actuator/health  # Driver Simulator

# View all container statuses
docker-compose ps

# Monitor logs in real-time
docker-compose logs -f
```

### Complete System Reset

If all else fails, perform a complete reset:

```bash
# Stop and remove all containers, networks, and volumes
cd infra
docker-compose down -v

# Remove Docker images
docker rmi $(docker images 'uit-go*' -q)

# Rebuild everything
cd ../linux-run  # or win-run on Windows
./start.sh
```

## Development Workflow

### Making Changes to a Service

1. **Modify Service Code**

   ```bash
   # Edit files in backend/<service-name>/src/
   # Example: backend/user-service/src/main/java/com/example/user_service/
   ```

2. **Rebuild the Service**

   ```bash
   cd backend/<service-name>
   ./mvnw clean package -DskipTests
   ```

3. **Restart the Container**

   ```bash
   cd ../../infra
   docker-compose restart <service-name>

   # Or rebuild the container image
   docker-compose up -d --build <service-name>
   ```

### Full System Rebuild

When making significant changes across multiple services:

#### macOS/Linux:

```bash
cd linux-run
./start.sh
```

This script will:

- Stop all running containers
- Build all services with Maven
- Rebuild and restart Docker containers
- Display service health status

#### Windows:

```bash
cd win-run
rebuild-all.bat
```

### Development Best Practices

1. **Hot Reload for Development**

   - Add Spring Boot DevTools dependency for automatic restart
   - Run services locally with `./mvnw spring-boot:run`

2. **Database Migrations**

   - Schema changes should be placed in `schema/` directory
   - Test migrations locally before deploying

3. **Testing**

   ```bash
   # Run tests for specific service
   cd backend/user-service
   ./mvnw test

   # Run tests with coverage
   ./mvnw test jacoco:report
   ```

4. **Logging**

   ```bash
   # View service logs
   docker-compose logs -f user-service

   # View last 100 lines
   docker-compose logs --tail=100 user-service
   ```

5. **Code Quality**
   - Follow Java coding conventions
   - Use meaningful commit messages
   - Test endpoints before committing

## Technology Stack

### Backend Services

- **Spring Boot 3.5.x** - Main application framework
- **Spring Cloud Gateway** - API Gateway and routing
- **Spring Data JPA** - Database ORM
- **Spring Security** - Authentication and authorization
- **Spring gRPC** - gRPC server/client support
- **OpenFeign** - Declarative HTTP client
- **JWT (jsonwebtoken)** - Token-based authentication

### Communication

- **gRPC 1.76.x** - High-performance RPC framework
- **Protocol Buffers** - Data serialization
- **REST** - HTTP-based APIs

### Data Storage

- **PostgreSQL 15** - Relational database
- **Redis 7** - In-memory data store with geospatial support

### Build & Deployment

- **Maven** - Dependency management and build tool
- **Docker** - Container platform
- **Docker Compose** - Multi-container orchestration

### Development Tools

- **Lombok** - Reduce boilerplate code
- **MapStruct** - Bean mapping
- **Spring Boot Actuator** - Production-ready monitoring

## Project Features

### Implemented Features

✅ **User Management**

- User registration and authentication
- JWT-based security
- Role-based access control (Passenger/Driver)
- Profile management

✅ **Trip Management**

- Trip request creation
- Trip status tracking (REQUESTED, MATCHED, ONGOING, COMPLETED, CANCELLED)
- Fare calculation
- Trip history

✅ **Driver Service**

- Driver registration and verification
- Real-time location tracking with Redis GEO
- Driver availability status
- Nearby driver search (geospatial queries)
- gRPC-based location updates

✅ **Driver Simulator**

- Automated driver movement simulation
- Path generation between waypoints
- Real-time location updates via gRPC
- Multi-driver simulation support

✅ **API Gateway**

- Centralized routing
- Path rewriting for service context paths
- Health monitoring

✅ **Infrastructure**

- Docker containerization
- Database-per-service pattern
- Health checks for all services
- Automated build scripts

### Planned Features

🔄 **In Progress**

- Payment processing integration
- Push notifications
- Real-time trip tracking
- Rating and review system

📋 **Backlog**

- Admin dashboard
- Analytics and reporting
- Message queue (RabbitMQ) integration
- Service mesh (Istio) implementation
- Kubernetes deployment

## Additional Resources

### Documentation

- **[Architecture Overview](docs/architecture.md)** - System design and component details
- **[API Interfaces](docs/interfaces.md)** - Complete API documentation
- **[Testing Guide](docs/pattern2-testing-guide.md)** - Comprehensive testing examples
- **[Implementation Status](docs/pattern2-implementation-status.md)** - Feature completion status
- **[Redis & gRPC Commands](docs/redis-grpc-testing-commands.md)** - Testing utilities

### Quick References

**Service Architecture Pattern**: Database-per-service microservices  
**Authentication**: JWT Bearer tokens  
**Inter-Service Communication**: REST (OpenFeign) + gRPC  
**Data Storage**: PostgreSQL (relational) + Redis (geospatial/caching)  
**Container Orchestration**: Docker Compose
