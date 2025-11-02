# Service Standardization Guide

This guide establishes the standard patterns and conventions for all microservices in the ride-sharing platform. The architecture uses a **hybrid approach**:

- **Java Spring Boot Services** (`backend/`) - Handle business logic, database operations, and REST APIs
- **Go gRPC Services** (`grpc-services/`) - Provide gRPC facades that call the Spring Boot services

This guide covers both layers, with the driver-service as the reference implementation.

## 1. Architecture Overview

### Hybrid Service Architecture
```
┌─────────────────┐    gRPC     ┌─────────────────┐    HTTP/REST   ┌─────────────────┐
│   Client Apps   │ ◄---------> │  Go gRPC Layer  │ ◄-----------> │ Java Spring Boot │
│                 │             │ (grpc-services/) │               │   (backend/)     │
└─────────────────┘             └─────────────────┘               └─────────────────┘
                                                                            │
                                                                            ▼
                                                                   ┌─────────────────┐
                                                                   │   PostgreSQL    │
                                                                   │    Database     │
                                                                   └─────────────────┘
```

### Service Layers
1. **Go gRPC Layer**: Handles gRPC protocol, validation, and forwards requests to Spring Boot
2. **Java Spring Boot Layer**: Business logic, database operations, caching, message queues
3. **Database Layer**: PostgreSQL with proper schemas and relationships

## 2. Go gRPC Services Structure (`grpc-services/`)

### Project Structure
```
grpc-services/
├── go.mod                    # Go module definition
├── proto/                    # Shared proto files
│   ├── driver.proto
│   ├── trip.proto
│   └── user.proto
├── driver-service/
│   └── main.go
├── trip-service/
│   └── main.go
└── user-service/
    └── main.go
```

### Go Module Standards
```go
module github.com/uit-go/grpc-services

go 1.21

require (
    google.golang.org/grpc v1.60.0
    google.golang.org/protobuf v1.31.0
    github.com/google/uuid v1.3.0
)
```

### Standard Go gRPC Service Template
```go
package main

import (
    "context"
    "log"
    "net"
    "net/http"
    "encoding/json"
    "bytes"
    
    "google.golang.org/grpc"
    pb "github.com/uit-go/grpc-services/proto"
)

type {ServiceName}Server struct {
    pb.Unimplemented{ServiceName}ServiceServer
    springBootURL string
    httpClient    *http.Client
}

func (s *{ServiceName}Server) {MethodName}(ctx context.Context, req *pb.{RequestType}) (*pb.{ResponseType}, error) {
    // Call Spring Boot service via HTTP
    url := fmt.Sprintf("%s/api/{endpoint}", s.springBootURL)
    
    // Convert gRPC request to HTTP request
    httpReq := convertToHTTPRequest(req)
    
    // Make HTTP call
    resp, err := s.makeHTTPCall(url, httpReq)
    if err != nil {
        return &pb.{ResponseType}{
            Success: false,
            Message: fmt.Sprintf("Service call failed: %v", err),
        }, nil
    }
    
    // Convert HTTP response to gRPC response
    return convertToGRPCResponse(resp), nil
}

func main() {
    port := getEnv("GRPC_PORT", "9090")
    springBootURL := getEnv("SPRING_BOOT_URL", "http://{service-name}-service:8080")
    
    server := &{ServiceName}Server{
        springBootURL: springBootURL,
        httpClient:    &http.Client{Timeout: 30 * time.Second},
    }
    
    lis, err := net.Listen("tcp", ":"+port)
    if err != nil {
        log.Fatalf("Failed to listen: %v", err)
    }
    
    grpcServer := grpc.NewServer()
    pb.Register{ServiceName}ServiceServer(grpcServer, server)
    
    log.Printf("{ServiceName} gRPC server listening on port %s", port)
    if err := grpcServer.Serve(lis); err != nil {
        log.Fatalf("Failed to serve: %v", err)
    }
}
```

## 4. Shared Protocol Buffers (`grpc-services/proto/`)

### Proto File Standards
- Location: `grpc-services/proto/{service_name}.proto`
- Consistent naming and response patterns across services
- Generate code for both Go and Java

### Standard Proto Template
```protobuf
syntax = "proto3";

package uitgo.{servicename};

option go_package = "github.com/uit-go/grpc-services/proto/{servicename}";
option java_multiple_files = true;
option java_package = "uitgo.{servicename}.grpc";
option java_outer_classname = "{ServiceName}ServiceProto";

service {ServiceName}Service {
  rpc {MethodName} ({RequestType}) returns ({ResponseType});
}

message {RequestType} {
  string {entity}_id = 1;
  // Request-specific fields
}

message {ResponseType} {
  string {entity}_id = 1;
  // Response-specific fields
  bool success = 98;
  string message = 99;
}
```

## 5. Java Spring Boot Layer Standards

### Package Naming Convention
```
uitgo.{servicename}
├── config/           # Configuration classes
├── controller/       # REST controllers (if needed)
├── dto/             # Data Transfer Objects
├── entity/          # JPA entities
├── event/           # Event handling classes
├── exception/       # Custom exceptions
├── grpc/            # gRPC service implementations
├── repository/      # JPA repositories
├── service/         # Business logic services
└── util/           # Utility classes
```

### File Naming Conventions
- Service name: `{service-name}-service` (kebab-case)
- Main class: `{ServiceName}ServiceApplication.java` (PascalCase)
- Entity: `{EntityName}.java` (PascalCase)
- gRPC Implementation: `{ServiceName}ServiceGrpcImpl.java`
- Repository: `{EntityName}Repository.java`
- Service: `{EntityName}{Function}Service.java`

## 2. Maven Configuration (pom.xml)

### Standard Properties
```xml
<properties>
    <java.version>17</java.version>
    <grpc.version>1.60.0</grpc.version>
    <protobuf.version>3.24.0</protobuf.version>
</properties>
```

### Required Dependencies
```xml
<!-- Core Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- gRPC -->
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-stub</artifactId>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-protobuf</artifactId>
</dependency>
<dependency>
    <groupId>net.devh</groupId>
    <artifactId>grpc-server-spring-boot-starter</artifactId>
    <version>3.1.0.RELEASE</version>
</dependency>

<!-- Database -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Message Queue -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<!-- Utilities -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### Required Build Plugins
```xml
<!-- Protobuf Maven Plugin -->
<plugin>
    <groupId>org.xolstice.maven.plugins</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
    <version>0.6.1</version>
</plugin>

<!-- OS Maven Plugin -->
<plugin>
    <groupId>kr.motd.maven</groupId>
    <artifactId>os-maven-plugin</artifactId>
    <version>1.7.1</version>
</plugin>
```

## 3. Protocol Buffers (Proto Files)

### File Location
- Main proto file: `src/main/proto/{service_name}_service.proto`

### Proto File Template
```protobuf
syntax = "proto3";

package uitgo.{servicename};

option java_multiple_files = true;
option java_package = "uitgo.{servicename}.grpc";
option java_outer_classname = "{ServiceName}ServiceProto";

service {ServiceName}Service {
  // Define RPC methods here
}

// Standard response pattern
message {EntityName}Response {
  string id = 1;
  // Entity-specific fields
  bool success = 98;
  string message = 99;
}
```

### Response Pattern Standards
- All responses should include `success` (boolean) and `message` (string) fields
- Use consistent field numbering (98-99 for standard fields)
- Use snake_case for field names in proto files

## 4. Entity Design Patterns

### Standard Entity Template
```java
@Entity
@Table(name = "{plural_entity_name}", schema = "{service_name}")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class {EntityName} {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "{entity}_id", columnDefinition = "uuid")
    private UUID {entity}Id;

    // Other fields with proper JPA annotations
    
    @Column(name = "created_at")
    private Long createdAt;
    
    @Column(name = "updated_at")
    private Long updatedAt;
}
```

### Entity Conventions
- Use UUID as primary key
- Use snake_case for database column names
- Include `created_at` and `updated_at` timestamps
- Use Lombok annotations for boilerplate code
- Use proper JPA validation annotations

## 5. gRPC Service Implementation

### Standard Implementation Template
```java
@Slf4j
@GrpcService
public class {ServiceName}ServiceGrpcImpl extends {ServiceName}ServiceGrpc.{ServiceName}ServiceImplBase {

    private final {EntityName}Service {entity}Service;

    public {ServiceName}ServiceGrpcImpl({EntityName}Service {entity}Service) {
        this.{entity}Service = {entity}Service;
    }

    @Override
    public void {methodName}({RequestType} request, 
                           StreamObserver<{ResponseType}> responseObserver) {
        try {
            // Business logic
            {ResponseType} response = {ResponseType}.newBuilder()
                .setSuccess(true)
                .setMessage("Operation completed successfully")
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in {methodName}: ", e);
            {ResponseType} errorResponse = {ResponseType}.newBuilder()
                .setSuccess(false)
                .setMessage("Error: " + e.getMessage())
                .build();
            
            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }
}
```

## 6. Configuration Standards

### Application.yml Template
```yaml
spring:
  application:
    name: {service-name}-service

  datasource:
    url: jdbc:postgresql://{service-name}-service-db:5432/{service_name}_db
    username: {service_name}_user
    password: {service_name}_pass
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        use_sql_comments: true
    show-sql: false

  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000
      database: 0

grpc:
  server:
    port: 9090

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

## 7. Docker Configuration

### Standard Dockerfile
```dockerfile
# Stage 1: Build stage
FROM maven:3.9.0-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom.xml
COPY pom.xml .

# Download dependencies
RUN mvn dependency:resolve

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy jar from builder
COPY --from=builder /build/target/{service-name}-*.jar app.jar

# Create non-root user
RUN addgroup -g 1000 appuser && \
    adduser -D -s /bin/sh -u 1000 -G appuser appuser

# Change ownership
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose ports
EXPOSE 8080 9090

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 8. Database Schema Standards

### Standard Schema Template
```sql
-- {Service Name} Database Schema

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create schema
CREATE SCHEMA IF NOT EXISTS {service_name};

CREATE TABLE {service_name}.{plural_entity_name} (
    {entity}_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- Entity-specific fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 9. Testing Standards

### Test Package Structure
```
src/test/java/uitgo/{servicename}/
├── integration/     # Integration tests
├── service/        # Service layer tests
├── grpc/          # gRPC tests
└── repository/    # Repository tests
```

### Test Dependencies
```xml
<!-- Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

## 10. Docker Configuration for Hybrid Architecture

### Go gRPC Service Dockerfile
```dockerfile
# grpc-services/Dockerfile.{service}
FROM golang:1.21-alpine AS builder

WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download

COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o {service}-grpc ./{service}-service

FROM alpine:latest
RUN apk --no-cache add ca-certificates
WORKDIR /root/

COPY --from=builder /app/{service}-grpc .

EXPOSE 9090
CMD ["./{service}-grpc"]
```

### Java Spring Boot Service Dockerfile  
```dockerfile
# backend/{service-name}/Dockerfile
FROM maven:3.9.0-eclipse-temurin-17 AS builder

WORKDIR /build
COPY pom.xml .
RUN mvn dependency:resolve

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl
COPY --from=builder /build/target/{service-name}-*.jar app.jar

RUN addgroup -g 1000 appuser && \
    adduser -D -s /bin/sh -u 1000 -G appuser appuser && \
    chown -R appuser:appuser /app

USER appuser

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose Integration
```yaml
# infra/docker-compose.yml
version: '3.8'
services:
  # Java Spring Boot Service
  {service-name}-service:
    build: ../backend/{service-name}
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://{service-name}-db:5432/{service_name}_db
    depends_on:
      - {service-name}-db
  
  # Go gRPC Service
  {service-name}-grpc:
    build: 
      context: ../grpc-services
      dockerfile: Dockerfile.{service}
    ports:
      - "9090:9090"
    environment:
      - SPRING_BOOT_URL=http://{service-name}-service:8080
    depends_on:
      - {service-name}-service
```

## 11. Implementation Checklist

### For Go gRPC Services:
- [ ] Uses shared proto files from `grpc-services/proto/`
- [ ] Implements proper gRPC server with generated protobuf code
- [ ] Makes HTTP calls to corresponding Spring Boot service
- [ ] Handles errors and converts between gRPC and HTTP formats
- [ ] Uses environment variables for configuration
- [ ] Includes proper logging and monitoring

### For Java Spring Boot Services:
- [ ] Package structure follows `uitgo.{servicename}` convention
- [ ] Maven dependencies are standardized (versions, artifacts)  
- [ ] Entity uses UUID primary keys and proper JPA annotations
- [ ] REST controllers provide endpoints for gRPC layer to call
- [ ] Configuration follows application.yml template
- [ ] Database schema includes proper constraints and indexing
- [ ] Health checks and monitoring endpoints are configured

### For Shared Components:
- [ ] Proto files follow consistent naming conventions
- [ ] Docker configurations for both layers
- [ ] Environment-specific configuration files
- [ ] Inter-service communication patterns established

## 12. Next Steps - Implementation Plan

1. **Standardize Shared Proto Files**
   - Create consistent proto definitions in `grpc-services/proto/`
   - Generate Go and Java code from shared protos

2. **Standardize Trip Service** 
   - Update Go gRPC service to use proper protobuf
   - Refactor Java Spring Boot service structure
   - Ensure proper HTTP endpoints for gRPC calls

3. **Standardize User Service**
   - Follow same pattern as trip service
   - Implement proper gRPC-to-HTTP communication

4. **Update API Gateway**
   - Ensure compatibility with standardized gRPC services

**Ready to proceed with implementation. Which service would you like to standardize first?**