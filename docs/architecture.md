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
│   │   │   ├── java/com/example/api_gateway/
│   │   │   └── resources/
│   │   ├── target/
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   ├── mvnw
│   │   ├── mvnw.cmd
│   │   ├── deps.txt
│   │   └── mvc.txt
│   ├── user-service/
│   │   ├── src/main/
│   │   │   ├── java/com/example/user_service/
│   │   │   └── resources/
│   │   ├── target/
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   ├── mvnw
│   │   └── mvnw.cmd
│   ├── trip-service/
│   │   ├── src/main/
│   │   │   ├── java/com/example/trip_service/
│   │   │   └── resources/
│   │   ├── target/
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   ├── mvnw
│   │   └── mvnw.cmd
│   ├── driver-service/
│   │   ├── src/main/
│   │   │   ├── java/uitgo/driverservice/
│   │   │   └── resources/
│   │   ├── target/
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   ├── mvnw
│   │   └── mvnw.cmd
│   └── dependencies.md
├── grpc-services/
│   ├── driver-service/
│   │   ├── main.go
│   │   └── proto/
│   ├── trip-service/
│   │   ├── main.go
│   │   └── proto/
│   ├── user-service/
│   │   ├── main.go
│   │   └── proto/
│   ├── proto/
│   ├── go.mod
│   ├── go.sum
│   ├── generate-proto.bat
│   ├── generate-proto.sh
│   ├── Dockerfile.driver
│   ├── Dockerfile.trip
│   ├── Dockerfile.user
│   └── README.md
├── db/
│   ├── user-service-db/
│   │   ├── schema.sql
│   │   └── test-data.sql
│   ├── trip-service-db/
│   │   └── schema.sql
│   ├── driver-service-db/
│   │   ├── schema.sql
│   │   └── test-data.sql
│   └── shared/
│       ├── docker-compose-db.yml
│       └── init-all-dbs.sql
├── infra/
│   └── docker-compose.yml
├── infrastructure/
│   └── docker/
│       └── postgres/
├── docs/
│   ├── code-interfaces-summary.md
│   ├── interfaces.md
│   ├── pattern2-implementation-status.md
│   ├── pattern2-poc.md
│   ├── pattern2-testing-guide.md
│   ├── service-standardization-guide.md
│   ├── uuid-migration-fixes.md
│   └── uuid-migration-status.md
├── src/
│   └── driver-service/
│       └── target/
├── demo-scripts/
│   ├── demo-service-integration.bat
│   ├── demo-ride-complete.bat
│   ├── demo-grpc.bat
│   ├── demo-rabbitmq.bat
│   ├── demo-rabbitmq-flow.bat
│   ├── demo-rabbitmq-status.bat
│   ├── debug-trip.bat
│   └── setup-demo-driver.bat
├── build-scripts/
│   ├── build-grpc-only.bat/.sh
│   ├── build-sequential.bat/.sh
│   ├── rebuild-all.bat/.sh
│   └── restart-docker.bat/.sh
├── temp-files/
│   ├── temp_connections.json
│   ├── temp_nodes.json
│   ├── temp_overview.json
│   ├── temp_queue_driver.offline.json
│   ├── temp_queue_driver.online.json
│   ├── temp_queue_trip.created.queue.json
│   ├── test_driver.json
│   └── test_trip.json
├── common-command.yaml
├── log.txt
├── plan.md
├── architecture.md
└── demo-batch-README.md
```

## Service Details

### API Gateway (Port 8080)
- **Purpose**: Entry point for all client requests
- **Technology**: Spring Cloud Gateway
- **Package**: `com.example.api_gateway`
- **Responsibilities**:
  - Route requests to appropriate microservices
  - Handle authentication and authorization
  - Load balancing and circuit breaking
  - API rate limiting
  - Path rewriting for different service context paths

### User Service (Port 8081)
- **Purpose**: Manage passenger and driver user accounts
- **Technology**: Spring Boot + PostgreSQL
- **Package**: `com.example.user_service`
- **Context Path**: `/` (root)
- **Responsibilities**:
  - User registration and authentication
  - Profile management
  - Session management
  - User role management (passenger/driver)

### Trip Service (Port 8082)
- **Purpose**: Core trip management functionality
- **Technology**: Spring Boot + PostgreSQL
- **Package**: `com.example.trip_service`
- **Context Path**: `/` (root)
- **Responsibilities**:
  - Trip creation and management
  - Trip status tracking (requested, matched, ongoing, completed, cancelled)
  - Fare calculation and estimation
  - Trip history and location tracking
  - Payment processing integration
  - Rating and review system

### Driver Service (Port 8083)
- **Purpose**: Driver-specific operations
- **Technology**: Spring Boot + PostgreSQL + Redis
- **Package**: `uitgo.driverservice`
- **Context Path**: `/api/driver-service`
- **Responsibilities**:
  - Driver registration and verification
  - Driver availability status management
  - Real-time location tracking with geospatial queries
  - Trip acceptance/rejection
  - Driver ratings and reviews
  - Vehicle information management

### gRPC Services
- **User gRPC Service (Port 50051)**: Direct gRPC communication for user operations
- **Trip gRPC Service (Port 50052)**: Direct gRPC communication for trip operations  
- **Driver gRPC Service (Port 50053)**: Direct gRPC communication for driver operations
- **Technology**: Go + gRPC
- **Purpose**: High-performance inter-service communication and external gRPC client support

## Communication Patterns

### API Interfaces
Detailed interface specifications are documented in [`docs/interfaces.md`](interfaces.md), including:
- REST API endpoints for each service
- gRPC service definitions and protobuf schemas
- Database entity models and repository interfaces
- Message queue event contracts
- Shared DTOs and common configurations

### Service Communication
- **REST APIs**: External client communication via API Gateway
- **gRPC**: Inter-service synchronous communication and direct client access
- **RabbitMQ**: Asynchronous event-driven messaging
- **Context Path Handling**: API Gateway handles different service context paths with path rewriting

### Port Configuration
| Service | HTTP Port | gRPC Port | Context Path |
|---------|-----------|-----------|--------------|
| API Gateway | 8080 | - | `/` |
| User Service | 8081 | 50051 | `/` |
| Trip Service | 8082 | 50052 | `/` |
| Driver Service | 8083 | 50053 | `/api/driver-service` |

### Message Queue Events
- `trip.created`: When a new trip is requested
- `trip.accepted`: When a driver accepts a trip
- `trip.completed`: When a trip is finished
- `driver.location.updated`: Real-time location updates
- `user.registered`: When a new user signs up
- `driver.status.changed`: When driver availability changes

## Database Design

### Database Design

### Database per Service Pattern
Each service has its own PostgreSQL database:
- **user_service_db**: User accounts and profiles
- **trip_service_db**: Trip data and history
- **driver_service_db**: Driver information and status

### Database Structure
```
PostgreSQL Instances:
├── user-service-db (Port 5432)
│   ├── users table (id, email, name, password, user_type, phone, created_at, deleted_at)
│   └── user_profiles table (profile management)
├── trip-service-db (Port 5432)  
│   ├── trips table (id, passenger_id, driver_id, status, pickup/destination details, fare, timestamps)
│   ├── payments table (payment processing records)
│   └── ratings table (trip ratings and reviews)
└── driver-service-db (Port 5432)
    ├── drivers table (driver_id, user_id, license_number, vehicle_model, vehicle_plate, rating, status)
    ├── driver_locations table (real-time location tracking with geospatial support)
    └── driver_sessions table (session management)

Redis Instance:
└── Driver location cache and session storage
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
3. gRPC services built with Go and containerized separately
4. Local development and testing with Docker Compose
5. Automated build scripts for development workflow

### Build Scripts
- `rebuild-all.bat/.sh`: Complete system rebuild and restart
- `build-grpc-only.bat/.sh`: Build only gRPC services
- `build-sequential.bat/.sh`: Sequential service builds
- `restart-docker.bat/.sh`: Docker environment restart

### Demo Scripts
- `demo-service-integration.bat`: Complete end-to-end ride scenario demonstration
- `demo-ride-complete.bat`: Ride completion workflow demonstration
- `demo-grpc.bat`: gRPC service communication testing
- `demo-rabbitmq.bat`: Message queue functionality testing
- `debug-trip.bat`: Trip service debugging and testing

### Development Strategy
- Local development with Docker Compose
- Continuous integration with automated builds
- Service-independent deployment capabilities
- Real-time testing with comprehensive demo scenarios
- Dynamic entity creation for testing (no pre-seeded data required)