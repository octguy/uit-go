# UIT-Go System Architecture

## Project Overview
UIT-Go is a ride-hailing microservices system built with Spring Boot, gRPC, RabbitMQ, PostgreSQL, and containerized with Docker.

## System Architecture

### High-Level Architecture
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Mobile App    │────│   API Gateway    │────│  Load Balancer  │
│  (Frontend)     │    │   (Port 8080)    │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                ┌───────────────┼───────────────┐
                │               │               │
        ┌───────▼──────┐ ┌──────▼──────┐ ┌─────▼──────┐
        │ User Service │ │ Trip Service│ │Driver Service│
        │ (Port 8081)  │ │ (Port 8082) │ │ (Port 8083) │
        └───────┬──────┘ └──────┬──────┘ └─────┬──────┘
                │               │               │
        ┌───────▼──────┐ ┌──────▼──────┐ ┌─────▼──────┐
        │ PostgreSQL   │ │ PostgreSQL  │ │ PostgreSQL │
        │ (Users DB)   │ │ (Trips DB)  │ │ (Drivers DB)│
        └──────────────┘ └─────────────┘ └────────────┘
                                │
                        ┌───────▼──────┐
                        │   RabbitMQ   │
                        │  (Messaging) │
                        └──────────────┘
```

## Project Structure
```
uit-go/
├── backend/
│   ├── api-gateway/
│   │   ├── src/main/
│   │   │   ├── java/com/example/gateway/
│   │   │   ├── protobuf/
│   │   │   └── resources/
│   │   ├── target/
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── mvnw.cmd
│   ├── user-service/
│   │   ├── src/main/
│   │   │   ├── java/com/example/user/
│   │   │   ├── protobuf/
│   │   │   └── resources/
│   │   ├── target/
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── mvnw.cmd
│   ├── trip-service/
│   │   ├── src/main/
│   │   │   ├── java/com/example/trip/
│   │   │   ├── protobuf/
│   │   │   └── resources/
│   │   ├── target/
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── mvnw.cmd
│   ├── driver-service/
│   │   ├── src/main/
│   │   │   ├── java/com/example/driver/
│   │   │   ├── protobuf/
│   │   │   └── resources/
│   │   ├── target/
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── mvnw.cmd
│   └── dependencies.md
├── db/
│   ├── user-service-db/
│   │   ├── migrations/
│   │   ├── init-scripts/
│   │   └── schema.sql
│   ├── trip-service-db/
│   │   ├── migrations/
│   │   ├── init-scripts/
│   │   └── schema.sql
│   ├── driver-service-db/
│   │   ├── migrations/
│   │   ├── init-scripts/
│   │   └── schema.sql
│   └── shared/
│       ├── docker-compose-db.yml
│       └── init-all-dbs.sql
├── infra/
│   ├── terraform/
│   │   ├── modules/
│   │   ├── environments/
│   │   └── main.tf
│   └── docker-compose.yml
├── docs/
│   ├── api/
│   ├── deployment/
│   └── user-guides/
├── build-all.bat
├── plan.md
├── architecture.md
└── desc.md
```

## Service Details

### API Gateway (Port 8080)
- **Purpose**: Entry point for all client requests
- **Technology**: Spring Cloud Gateway
- **Responsibilities**:
  - Route requests to appropriate microservices
  - Handle authentication and authorization
  - Load balancing and circuit breaking
  - API rate limiting

### User Service (Port 8081)
- **Purpose**: Manage passenger and driver user accounts
- **Technology**: Spring Boot + PostgreSQL
- **Responsibilities**:
  - User registration and authentication
  - Profile management
  - Session management
  - User role management (passenger/driver)

### Trip Service (Port 8082)
- **Purpose**: Core trip management functionality
- **Technology**: Spring Boot + PostgreSQL
- **Responsibilities**:
  - Trip creation and management
  - Trip status tracking (requested, matched, ongoing, completed, cancelled)
  - Fare calculation
  - Trip history

### Driver Service (Port 8083)
- **Purpose**: Driver-specific operations
- **Technology**: Spring Boot + PostgreSQL
- **Responsibilities**:
  - Driver registration and verification
  - Driver availability status
  - Location tracking
  - Trip acceptance/rejection
  - Driver ratings and reviews

## Communication Patterns

### API Interfaces
Detailed interface specifications are documented in [`docs/interfaces.md`](docs/interfaces.md), including:
- REST API endpoints for each service
- gRPC service definitions and protobuf schemas
- Database entity models and repository interfaces
- Message queue event contracts
- Shared DTOs and common configurations

### Service Communication
- **REST APIs**: External client communication via API Gateway
- **gRPC**: Inter-service synchronous communication
- **RabbitMQ**: Asynchronous event-driven messaging

### Message Queue Events
- `trip.created`: When a new trip is requested
- `trip.accepted`: When a driver accepts a trip
- `trip.completed`: When a trip is finished
- `driver.location.updated`: Real-time location updates
- `user.registered`: When a new user signs up

## Database Design

### Database Design

### Database per Service Pattern
Each service has its own PostgreSQL database:
- **user_service_db**: User accounts and profiles
- **trip_service_db**: Trip data and history
- **driver_service_db**: Driver information and status

### Database Structure
```
PostgreSQL Instance:
├── user_service_db
│   ├── users table
│   ├── user_profiles table
│   └── user_sessions table
├── trip_service_db
│   ├── trips table
│   ├── trip_status table
│   └── trip_history table
└── driver_service_db
    ├── drivers table
    ├── driver_locations table
    └── driver_ratings table
```

### Database Migration Strategy
- Flyway or Liquibase for schema versioning
- Independent migration scripts per service
- Environment-specific migration configurations

## Infrastructure

### Containerization
- **Docker**: Each service containerized with individual Dockerfiles
- **Docker Compose**: Local development environment orchestration

## Security

### Authentication & Authorization
- JWT tokens for session management
- Role-based access control (RBAC)
- API Gateway-level authentication

### Network Security
- Docker network isolation
- Service-to-service communication via gRPC
- Database access restricted to service containers

## Monitoring & Observability

### Application Monitoring
- Spring Boot Actuator endpoints for health checks
- Service-level metrics and logging
- Container health monitoring via Docker

## Development Workflow

### Build Process
1. Maven builds each service independently with Maven Wrapper
2. Docker images created for each service using multi-stage builds
3. Local development and testing with Docker Compose
4. Automated build scripts for development workflow

### Development Strategy
- Local development with Docker Compose
- Continuous integration with automated builds
- Service-independent deployment capabilities