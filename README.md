# UIT-Go - Ride-Hailing Microservices System

A microservices-based ride-hailing platform built with Spring Boot, gRPC, PostgreSQL, Redis, and Docker.

## Table of Contents

- [System Overview](#system-overview)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Installation](#installation)
- [Running the System](#running-the-system)
- [Service Endpoints](#service-endpoints)
- [Database Access](#database-access)
- [Troubleshooting](#troubleshooting)

## System Overview

UIT-Go consists of the following microservices:

- **User Service** (Port 8081) - User management and authentication
- **Trip Service** (Port 8082) - Trip booking and management
- **Driver Service** (Port 8083) - Driver management and availability
- **API Gateway** (Port 8080) - Route management and service orchestration

### Infrastructure Components

- **PostgreSQL** - Separate databases for each service (Ports 5433-5435)
- **Redis** - Caching and session management (Port 6379)
- **Docker** - Containerization platform

## Prerequisites

Ensure the following are installed on your system:

### Required

- **Java 17** or higher
  ```bash
  java -version
  ```
- **Maven 3.6+** (included via Maven Wrapper)
- **Docker Desktop** (version 20.10+)
  ```bash
  docker --version
  docker-compose --version
  ```

### Optional (for development)

- **Git** - For version control
- **Postman** or **curl** - For API testing

## Project Structure

```
uit-go/
├── backend/
│   ├── api-gateway/        # API Gateway service
│   ├── user-service/       # User management service
│   ├── trip-service/       # Trip management service
│   └── driver-service/     # Driver management service
├── infra/
│   └── docker-compose.yml  # Docker orchestration
├── schema/                 # Database schemas
│   ├── user-schema.sql
│   ├── trip-schema.sql
│   └── driver-schema.sql
├── linux-run/              # Linux/macOS scripts
│   ├── rebuild-all.sh
│   ├── restart-docker.sh
│   └── start.sh
├── win-run/                # Windows scripts
│   ├── rebuild-all.bat
│   ├── restart-docker.bat
│   └── build-sequential.bat
└── docs/                   # Documentation
```

## Installation

### 1. Clone the Repository

```bash
git clone <repository-url>
cd uit-go
```

### 2. Verify Prerequisites

```bash
# Check Java version
java -version

# Check Docker
docker --version
docker-compose --version

# Ensure Docker Desktop is running
docker ps
```

### 3. Build All Services

#### On macOS/Linux:

```bash
cd linux-run
chmod +x *.sh
./rebuild-all.sh
```

#### On Windows:

```cmd
cd win-run
rebuild-all.bat
```

This script will:

1. Build all Spring Boot services using Maven
2. Stop any running Docker containers
3. Build and start all services in Docker

## Running the System

### Quick Start (All Services)

#### On macOS/Linux:

```bash
cd linux-run
./start.sh
```

#### On Windows:

```cmd
cd win-run
start.bat
```

### Manual Start with Docker Compose

Navigate to the infrastructure directory:

```bash
cd infra
docker-compose up -d
```

To view logs:

```bash
docker-compose logs -f
```

To stop all services:

```bash
docker-compose down
```

### Individual Service Development

To run a single service locally without Docker:

```bash
# Navigate to service directory
cd backend/user-service

# Run with Maven wrapper (Linux/macOS)
./mvnw spring-boot:run

# Run with Maven wrapper (Windows)
mvnw.cmd spring-boot:run
```

## Service Endpoints

Once running, services are available at:

| Service        | Port | URL                   | Description            |
| -------------- | ---- | --------------------- | ---------------------- |
| User Service   | 8081 | http://localhost:8081 | User management APIs   |
| Trip Service   | 8082 | http://localhost:8082 | Trip booking APIs      |
| Driver Service | 8083 | http://localhost:8083 | Driver management APIs |
| API Gateway    | 8080 | http://localhost:8080 | Unified API gateway    |

### Health Checks

Verify services are running:

```bash
# User Service
curl http://localhost:8081/actuator/health

# Trip Service
curl http://localhost:8082/actuator/health

# Driver Service
curl http://localhost:8083/actuator/health
```

## Database Access

Each service has its own PostgreSQL database:

| Database          | Port | Username            | Password            | Database Name     |
| ----------------- | ---- | ------------------- | ------------------- | ----------------- |
| User Service DB   | 5435 | user_service_user   | user_service_pass   | user_service_db   |
| Trip Service DB   | 5433 | trip_service_user   | trip_service_pass   | trip_service_db   |
| Driver Service DB | 5434 | driver_service_user | driver_service_pass | driver_service_db |

### Connect via psql:

```bash
# User Service Database
psql -h localhost -p 5435 -U user_service_user -d user_service_db

# Trip Service Database
psql -h localhost -p 5433 -U trip_service_user -d trip_service_db

# Driver Service Database
psql -h localhost -p 5434 -U driver_service_user -d driver_service_db
```

### Connect via GUI (DBeaver, pgAdmin):

- **Host:** localhost
- **Port:** See table above
- **Database, Username, Password:** See table above

## Troubleshooting

### Port Already in Use

If you get port conflicts:

```bash
# Check what's using a port (macOS/Linux)
lsof -i :8081

# Kill the process
kill -9 <PID>

# Or use different ports by modifying docker-compose.yml
```

### Docker Build Fails

```bash
# Clean Docker system
docker system prune -a

# Remove volumes
docker volume prune

# Rebuild
cd infra
docker-compose up --build --force-recreate
```

### Maven Build Fails

```bash
# Clean and rebuild specific service
cd backend/user-service
./mvnw clean install -DskipTests

# Clear Maven cache if needed
rm -rf ~/.m2/repository
```

### Services Won't Start

```bash
# Check Docker logs
docker-compose logs <service-name>

# Examples:
docker-compose logs user-service
docker-compose logs trip-service-db

# Check container status
docker-compose ps
```

### Database Connection Issues

- Ensure PostgreSQL containers are running: `docker-compose ps`
- Check database logs: `docker-compose logs user-service-db`
- Verify credentials in `docker-compose.yml` match application configs

### Redis Connection Issues

```bash
# Check Redis is running
docker-compose ps redis

# Test Redis connection
docker exec -it redis redis-cli ping
# Should return: PONG
```

## Development Workflow

### Making Changes

1. **Modify Service Code**

   ```bash
   # Edit files in backend/<service-name>/src/
   ```

2. **Rebuild Service**

   ```bash
   cd backend/<service-name>
   ./mvnw clean package -DskipTests
   ```

3. **Restart Container**
   ```bash
   cd infra
   docker-compose restart <service-name>
   ```

### Full Rebuild

When making significant changes:

#### macOS/Linux:

```bash
cd linux-run
./rebuild-all.sh
```

#### Windows:

```cmd
cd win-run
rebuild-all.bat
```

## Additional Resources

- **Architecture Documentation:** [docs/architecture.md](docs/architecture.md)
- **Service Interfaces:** [docs/interfaces.md](docs/interfaces.md)
- **Testing Guide:** [docs/pattern2-testing-guide.md](docs/pattern2-testing-guide.md)
