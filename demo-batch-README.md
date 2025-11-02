# UIT-Go Comprehensive Demo Scripts

This directory contains comprehensive demonstration scripts that showcase the complete UIT-Go ride-sharing platform functionality. These demos illustrate real-world scenarios and full integration between all microservices.

## Available Demo Scripts

### 1. Complete Ride Demo (`demo-ride-complete.bat`)
**Duration:** 10-15 minutes  
**Purpose:** End-to-end ride-sharing workflow demonstration

**Features:**
- Complete passenger journey from app opening to ride completion
- Driver onboarding and real-time location tracking
- Trip creation, matching, and assignment
- Real-time GPS tracking during the ride
- Payment processing and rating system
- Cross-service integration demonstration

**Demonstrates:**
- User Service: Registration, validation, profile management
- Driver Service: Location updates, status management, driver matching
- Trip Service: Trip lifecycle, fare calculation, completion
- API Gateway: Request routing and load balancing
- RabbitMQ: Event-driven messaging and real-time updates

**Usage:**
```cmd
demo-ride-complete.bat
```

### 2. gRPC Services Integration (`demo-grpc.bat`)
**Duration:** 8-12 minutes  
**Purpose:** Deep dive into gRPC communication patterns

**Features:**
- Service discovery and health checks
- Cross-service gRPC communication
- Real-time location updates simulation
- Error handling and validation
- Performance testing with concurrent requests
- Message structure inspection

**Demonstrates:**
- gRPC service reflection and introspection
- Inter-service communication patterns
- Real-time data synchronization
- Advanced gRPC features and debugging

**Usage:**
```cmd
demo-grpc.bat
```

### 3. RabbitMQ Message Flow (`demo-rabbitmq-flow.bat`)
**Duration:** 6-10 minutes  
**Purpose:** Event-driven architecture and message flow

**Features:**
- Driver location and status event publishing
- Trip lifecycle event flow
- User notification and communication events
- Cross-service event integration
- Queue monitoring and analytics

**Demonstrates:**
- Event-driven architecture patterns
- Asynchronous message processing
- Real-time event propagation
- Message queue monitoring
- Service decoupling through events

**Usage:**
```cmd
demo-rabbitmq-flow.bat
```

### 4. Complete Service Integration (`demo-service-integration.bat`)
**Duration:** 12-18 minutes  
**Purpose:** Comprehensive service integration showcase

**Features:**
- Full ride scenario with all services involved
- Multi-protocol communication (gRPC + HTTP + RabbitMQ)
- Real-time tracking and updates
- Payment processing integration
- Data consistency across services
- System monitoring and health checks

**Demonstrates:**
- Enterprise-grade microservices architecture
- Multi-service transaction coordination
- Real-time data synchronization
- Service fault tolerance
- Complete business workflow implementation

**Usage:**
```cmd
demo-service-integration.bat
```

## Prerequisites

### Required Tools
- **grpcurl**: For gRPC testing
  ```cmd
  go install github.com/fullstorydev/grpcurl/cmd/grpcurl@latest
  ```
- **curl**: For HTTP API testing (usually pre-installed on Windows 10+)

### Required Services
All demos require the following services to be running:

#### gRPC Services
- User Service gRPC: `localhost:50051`
- Driver Service gRPC: `localhost:50050`
- Trip Service gRPC: `localhost:50052`

#### HTTP APIs
- API Gateway: `localhost:8080`
- User Service API: `localhost:8081`
- Trip Service API: `localhost:8082`

#### Infrastructure
- RabbitMQ Management: `localhost:15672`

### Starting Services
```cmd
# Start all services with Docker
.\restart-docker.bat

# Or build and start manually
.\build-sequential.bat
```

## Demo Scenarios

### Scenario 1: First-Time User Experience
- User registration and profile setup
- Finding nearby drivers
- Booking first ride
- Payment and rating

### Scenario 2: Daily Commute
- Returning user with saved preferences
- Quick ride booking
- Real-time tracking
- Completion and automatic payment

### Scenario 3: Peak Hours
- Multiple concurrent ride requests
- Driver availability and matching
- Queue management
- Service performance under load

### Scenario 4: System Monitoring
- Health check across all services
- Message queue analytics
- Performance metrics
- Error handling demonstration

## Technical Architecture Demonstrated

### Microservices Patterns
- Service discovery and registration
- API Gateway pattern
- Event-driven architecture
- Saga pattern for distributed transactions
- CQRS (Command Query Responsibility Segregation)

### Communication Protocols
- **gRPC**: High-performance inter-service communication
- **HTTP/REST**: Web and mobile client APIs
- **RabbitMQ/AMQP**: Asynchronous event messaging
- **WebSocket**: Real-time client updates (demonstrated conceptually)

### Data Consistency
- Eventual consistency across services
- Event sourcing for audit trails
- Distributed transaction coordination
- Conflict resolution strategies

### Monitoring and Observability
- Service health checks
- Message queue monitoring
- Performance metrics collection
- Distributed tracing (demonstrated conceptually)

## Educational Value

These demos are designed to showcase:

1. **Enterprise Architecture Patterns**: Real-world microservices implementation
2. **Scalability Considerations**: How services handle growth and load
3. **Reliability Patterns**: Fault tolerance and error recovery
4. **Performance Optimization**: Efficient communication and data flow
5. **Security Integration**: Authentication and authorization flows
6. **DevOps Practices**: Service deployment and monitoring

## Troubleshooting

### Common Issues
1. **Service Not Running**: Ensure all services are started with `.\restart-docker.bat`
2. **Port Conflicts**: Check if ports are available using `netstat -an`
3. **Missing Tools**: Install grpcurl and verify curl is available
4. **Network Issues**: Verify localhost connectivity

### Debugging Commands
```cmd
# Check service health
grpcurl -plaintext localhost:50051 user.UserService/HealthCheck
grpcurl -plaintext localhost:50050 driver.DriverService/HealthCheck
grpcurl -plaintext localhost:50052 trip.TripService/HealthCheck

# Check HTTP APIs
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health

# Check RabbitMQ
curl -u guest:guest http://localhost:15672/api/overview
```

## Learning Outcomes

After completing these demos, you will understand:
- How to design and implement microservices architecture
- Event-driven communication patterns
- Real-time data synchronization techniques
- Service integration and orchestration
- Performance optimization strategies
- Monitoring and observability best practices

These demos provide a comprehensive learning experience for understanding modern distributed systems architecture and implementation patterns used in production ride-sharing platforms.

**Features:**
- Interactive menu to choose demo type
- No command-line parameters needed
- Pure batch file execution

**Usage:**
```cmd
demo-rabbitmq.bat
```

## Prerequisites

1. **Services Running:** Ensure all services are up via Docker Compose:
   ```cmd
   docker compose -f infra/docker-compose.yml up -d
   ```

2. **curl Available:** The batch files use `curl` for API calls (included in Windows 10/11)

3. **Test Data:** The scripts automatically reference existing test driver data

4. **RabbitMQ Access:** RabbitMQ Management UI should be accessible at:
   - URL: http://localhost:15672
   - Username: guest
   - Password: guest

## What Gets Demonstrated

### Message Flows
1. **Driver Location Updates**
   - REST API → Database → Event → RabbitMQ → Queue
   - Queue: `driver.location.updates`
   - Exchange: `driver.events`
   - Routing Key: `driver.location.updated`

2. **Trip Request Flow**
   - Trip creation → Driver matching → Assignment
   - Queue: `trip.created.queue`
   - Exchange: `trip.events`

3. **Status Updates**
   - Driver status changes → Notifications
   - Queue: `driver.status.changes`
   - Exchange: `driver.events`

### Technical Components
- **REST API Integration:** Direct curl calls to driver service endpoints
- **RabbitMQ Management API:** Queue status monitoring via HTTP API
- **Message Flow Simulation:** Shows complete event-driven workflows
- **Real-time Testing:** Actual API calls trigger real RabbitMQ messages

## Expected Output

### Quick Demo
```
📊 Current RabbitMQ Queue Status:
   ✅ RabbitMQ Management API is accessible
   🟢 driver.location.updates - Queue exists

📍 Testing Driver Location Update...
   ✅ Driver Service is accessible
   ✅ Location update successful!
   📤 RabbitMQ message published to driver.location.updates

🎯 Demo Summary:
   ✓ Driver service REST API connectivity verified
   ✓ RabbitMQ Management API accessibility confirmed
```

### Full Demo
Shows complete message flow scenarios with:
- Simulated driver location updates with JSON payloads
- Trip request and driver matching workflows  
- Status update sequences (DRIVER_ASSIGNED → DRIVER_ARRIVED → TRIP_STARTED → TRIP_COMPLETED)
- Queue monitoring and management interface information

## System Requirements

- **Windows:** Windows 10/11 (for built-in curl support)
- **Services:** Docker containers running (driver-service, RabbitMQ)
- **Network:** Access to localhost ports 8083, 15672
- **No Dependencies:** Pure batch files, no PowerShell or external tools required

## Troubleshooting

### Common Issues

1. **Services Not Running**
   ```
   ❌ Driver Service: UNREACHABLE
   ```
   **Solution:** Start services with `docker compose -f infra/docker-compose.yml up -d`

2. **RabbitMQ Not Accessible**
   ```
   ⚠️ Could not connect to RabbitMQ Management API
   ```
   **Solution:** Check RabbitMQ container is running and port 15672 is accessible

3. **curl Not Found**
   ```
   'curl' is not recognized as an internal or external command
   ```
   **Solution:** Use Windows 10/11 or install curl manually

### Verification Steps

1. **Check Services:**
   ```cmd
   curl http://localhost:8083/api/driver-service/drivers/health
   ```

2. **Check RabbitMQ:**
   ```cmd
   curl -u guest:guest http://localhost:15672/api/queues
   ```

3. **View Service Logs:**
   ```cmd
   docker logs driver-service --tail 20
   ```

## File Structure

```
├── demo-rabbitmq.bat              # Main menu (interactive)
├── demo-rabbitmq-quick.bat        # Quick 30-second demo
├── demo-rabbitmq-flow.bat         # Full 2-3 minute demo
└── demo-batch-README.md           # This documentation
```

## Next Steps

After running the demos:

1. **Explore RabbitMQ Management UI** to see real queue activity
2. **Implement message consumers** to process the queued messages
3. **Add more event types** (trip completion, payments, etc.)
4. **Set up monitoring** for queue depths and processing rates
5. **Implement dead letter queues** for failed message handling

The demos prove that your event-driven architecture is working and ready for building real-time features like live tracking, instant notifications, and automatic trip matching.

## Advantages of Batch File Approach

- **No Dependencies:** Works on any Windows system without PowerShell
- **Simple Deployment:** Single file execution, no script policy concerns
- **Direct API Testing:** Uses curl to make actual REST calls
- **Cross-Compatible:** Works in Command Prompt, PowerShell, or any Windows terminal
- **Easy Integration:** Can be called from CI/CD pipelines or other batch processes