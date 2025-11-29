# KIẾN TRÚC HỆ THỐNG UIT-GO

## Tổng quan

UIT-Go là một nền tảng đặt xe dựa trên kiến trúc microservices, được xây dựng bằng Spring Boot, sử dụng giao thức truyền thông hybrid (REST + gRPC), PostgreSQL, Redis và Docker.

## Sơ đồ Kiến trúc Tổng quan

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              CLIENTS (Mobile/Web)                                    │
│                         (Passengers & Drivers)                                       │
└──────────────────────────────────┬──────────────────────────────────────────────────┘
                                   │
                                   │ HTTP/REST
                                   │
                    ┌──────────────▼──────────────┐
                    │                             │
                    │      API GATEWAY            │
                    │   (Spring Cloud Gateway)    │
                    │       Port: 8080            │
                    │                             │
                    └──────────────┬──────────────┘
                                   │
                    ┌──────────────┼──────────────┐
                    │              │              │
          ┌─────────▼─────┐   ┌───▼──────┐   ┌──▼───────────┐
          │               │   │          │   │              │
          │ USER SERVICE  │   │  TRIP    │   │   DRIVER     │
          │   Port: 8081  │   │ SERVICE  │   │   SERVICE    │
          │               │   │ Port:8082│   │  Port: 8083  │
          │               │   │          │   │  gRPC: 9092  │
          └───────┬───────┘   └────┬─────┘   └──────┬───────┘
                  │                │                 │
                  │                │                 │
         ┌────────▼────────┐       │       ┌────────▼────────┐
         │   PostgreSQL    │       │       │     Redis       │
         │   user_service  │       │       │  (Geospatial)   │
         │   _db           │       │       │   Port: 6379    │
         │   Port: 5435    │       │       └─────────────────┘
         └─────────────────┘       │
                                   │
         ┌─────────────────────────┼──────────────────────────┐
         │                         │                          │
    ┌────▼──────────┐      ┌──────▼─────────┐      ┌────────▼────────┐
    │ PostgreSQL    │      │  PostgreSQL    │      │   RabbitMQ      │
    │ trip_service  │      │  trip_service  │      │                 │
    │ _db-vn        │      │  _db-th        │      │   Exchange:     │
    │ (Vietnam)     │      │  (Thailand)    │      │ trip.exchange   │
    │ Port: 5433    │      │  Port: 5434    │      │                 │
    └───────────────┘      └────────────────┘      │   Port: 5672    │
                                                    │   Mgmt: 15672   │
                                                    └─────────┬───────┘
                                                              │
                                                              │ AMQP
                                   ┌──────────────────────────┼─────────┐
                                   │                          │         │
                              Subscribe                   Publish       │
                                   │                          │         │
                            ┌──────▼──────┐           ┌──────▼──────┐  │
                            │   DRIVER    │           │    TRIP     │  │
                            │   SERVICE   │           │   SERVICE   │  │
                            │  Listener   │           │  Publisher  │  │
                            └─────────────┘           └─────────────┘  │
                                                                        │
                                                              ┌─────────▼────────┐
                                                              │  DRIVER          │
                                                              │  SIMULATOR       │
                                                              │  Port: 8084      │
                                                              │                  │
                                                              │  gRPC Client ────┼──────┐
                                                              └──────────────────┘      │
                                                                                        │
                                                                             gRPC Stream│
                                                                                        │
                                                              ┌─────────────────────────▼───┐
                                                              │    DRIVER SERVICE           │
                                                              │    gRPC Server: 9092        │
                                                              │ (DriverLocationService)     │
                                                              └─────────────────────────────┘
```

## Các Thành phần Chính

### 1. API Gateway (Port 8080)

**Công nghệ:** Spring Cloud Gateway (WebFlux)

**Chức năng:**

- Điểm truy cập thống nhất cho tất cả các client
- Định tuyến thông minh đến các microservices
- Không có business logic, chỉ route requests

**Routes:**

- `/api/users/**` → User Service (8081)
- `/api/trips/**` → Trip Service (8082)
- `/api/drivers/**` → Driver Service (8083)

**Dependencies chính:**

- `spring-cloud-starter-gateway-server-webflux`
- Spring Boot 3.5.7
- Spring Cloud 2025.0.0

### 2. User Service (Port 8081)

**Công nghệ:** Spring Boot, Spring Security, JWT

**Chức năng:**

- Quản lý người dùng (Passenger và Driver)
- Xác thực và phân quyền
- Đăng ký, đăng nhập, quản lý hồ sơ

**Endpoints chính:**

- `POST /api/users/register` - Đăng ký passenger
- `POST /api/users/register-driver` - Đăng ký driver
- `POST /api/users/login` - Đăng nhập (trả về JWT token)
- `GET /api/users/profile` - Lấy thông tin người dùng
- `PUT /api/users/profile` - Cập nhật hồ sơ

**Database:**

- PostgreSQL: `user_service_db` (Port 5435)
- Tables: `user` với các trường id, email, password, role, created_at
- Roles: PASSENGER, DRIVER

**Security:**

- JWT Authentication với secret key
- Token expiration: 24 giờ
- BCrypt password encoding
- Spring Security với JWT Filter

**Dependencies chính:**

- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `spring-boot-starter-web`
- `jjwt` (JWT library)
- `postgresql` driver
- `spring-cloud-starter-openfeign`

### 3. Trip Service (Port 8082)

**Công nghệ:** Spring Boot, JPA, OpenFeign, RabbitMQ

**Chức năng:**

- Quản lý chuyến đi (tạo, hủy, chấp nhận, bắt đầu, hoàn thành)
- Tính toán giá cước dự kiến
- Tìm kiếm tài xế gần nhất (qua Driver Service)
- Gửi thông báo chuyến đi đến tài xế qua RabbitMQ
- Lịch sử chuyến đi

**Endpoints chính:**

- `POST /api/trips/estimate-fare` - Ước tính giá cước
- `POST /api/trips/create` - Tạo chuyến đi mới
- `POST /api/trips/{id}/cancel` - Hủy chuyến đi
- `POST /api/trips/{id}/accept` - Chấp nhận chuyến đi (driver)
- `POST /api/trips/{id}/start` - Bắt đầu chuyến đi
- `POST /api/trips/{id}/complete` - Hoàn thành chuyến đi
- `POST /api/trips/{id}/rate` - Đánh giá chuyến đi
- `GET /api/trips/history` - Lịch sử chuyến đi

**Database Sharding:**
Trip Service sử dụng **Database Sharding theo địa lý** với 2 database PostgreSQL:

1. **Vietnam Database** (Port 5433):

   - Phục vụ các chuyến đi có `pickupLongitude >= 102.0`
   - Database: `trip_service_db`

2. **Thailand Database** (Port 5434):
   - Phục vụ các chuyến đi có `pickupLongitude < 102.0`
   - Database: `trip_service_db`

**Cơ chế Routing:**

- Sử dụng `AbstractRoutingDataSource` để định tuyến động
- `DbContextHolder` (ThreadLocal) lưu trữ shard key ("VN" hoặc "TH")
- Quyết định shard dựa trên tọa độ pickup khi tạo trip

**Trip States:**

- `SEARCHING_DRIVER` - Đang tìm tài xế
- `DRIVER_ASSIGNED` - Đã có tài xế nhận
- `IN_PROGRESS` - Đang di chuyển
- `COMPLETED` - Hoàn thành
- `CANCELLED` - Đã hủy

**Pricing Logic:**

- Tính khoảng cách dựa trên công thức Haversine
- Base fare + per-km rate
- Kết quả trả về dưới dạng BigDecimal (cents)

**Communication Patterns:**

- **REST API** cho client-facing endpoints
- **OpenFeign Client** để gọi Driver Service (tìm tài xế gần)
- **RabbitMQ Publisher** để gửi thông báo chuyến đi

**Dependencies chính:**

- `spring-boot-starter-data-jpa`
- `spring-boot-starter-web`
- `spring-boot-starter-amqp` (RabbitMQ)
- `spring-cloud-starter-openfeign`
- `postgresql` driver

### 4. Driver Service (Port 8083, gRPC: 9092)

**Công nghệ:** Spring Boot, Redis, gRPC, RabbitMQ

**Chức năng:**

- Quản lý vị trí tài xế real-time (Redis Geospatial)
- Tìm kiếm tài xế gần nhất
- Quản lý trạng thái tài xế (AVAILABLE, BUSY, OFFLINE)
- Nhận thông báo chuyến đi từ RabbitMQ
- Quản lý thông báo chuyến đi pending (in-memory)
- Cập nhật vị trí tài xế qua gRPC streaming

**REST Endpoints:**

- `GET /api/drivers/nearby` - Tìm tài xế gần vị trí (internal)
- `POST /api/drivers/notifications/{tripId}/accept` - Chấp nhận chuyến đi
- `GET /api/drivers/notifications` - Lấy danh sách thông báo
- `POST /api/drivers/status` - Cập nhật trạng thái tài xế
- `GET /api/internal/drivers/nearby` - API nội bộ cho Trip Service

**gRPC Service:**

- Service: `DriverLocationService`
- Method: `SendLocation(stream LocationRequest) returns (LocationResponse)`
- Port: 9092
- Protocol: Client streaming - tài xế gửi vị trí liên tục

**Protobuf Schema:**

```proto
message LocationRequest {
  string driverId = 1;
  double latitude = 2;
  double longitude = 3;
  int64 timestamp = 4;
}
```

**Redis Geospatial:**

- Key: `drivers:locations`
- Commands sử dụng:
  - `GEOADD` - Thêm/cập nhật vị trí tài xế
  - `GEORADIUS` / `GEOSEARCH` - Tìm tài xế trong bán kính
  - `GEOPOS` - Lấy vị trí tài xế
  - `GEODIST` - Tính khoảng cách

**Trip Notification Flow:**

1. Trip Service publish notification đến RabbitMQ
2. Driver Service subscribe và nhận notification
3. Lưu vào in-memory store với TTL (thời gian hết hạn)
4. Driver có thể accept hoặc để notification hết hạn
5. Khi accept, gọi lại Trip Service qua OpenFeign

**In-Memory Notification Store:**

- Entity: `PendingTripNotification`
- Chứa: tripId, passengerId, pickup/destination coordinates, fare, expiry time
- Quản lý bởi `TripNotificationService`

**Dependencies chính:**

- `spring-boot-starter-data-redis`
- `spring-boot-starter-web`
- `spring-grpc-server-spring-boot-starter`
- `spring-boot-starter-amqp` (RabbitMQ)
- `spring-cloud-starter-openfeign`
- `grpc-services`, `grpc-protobuf`, `grpc-netty-shaded`

### 5. Driver Simulator (Port 8084)

**Công nghệ:** Spring Boot, gRPC Client

**Chức năng:**

- Mô phỏng vị trí di chuyển của nhiều tài xế
- Gửi location updates đến Driver Service qua gRPC streaming
- Hỗ trợ testing và demo

**Features:**

- Lấy danh sách tất cả drivers từ Driver Service
- Tạo path di chuyển ngẫu nhiên cho mỗi driver với offset
- Multi-threaded simulation (mỗi driver 1 thread)
- Gửi location updates mỗi vài giây

**gRPC Client:**

- Kết nối đến Driver Service gRPC server (port 9092)
- Sử dụng client streaming để gửi location updates

**Path Generation:**

- Base path được tạo ngẫu nhiên
- Mỗi driver có offset ngẫu nhiên 1-3 km từ base path
- Tạo hiệu ứng nhiều tài xế di chuyển trong khu vực

**Dependencies chính:**

- `spring-grpc-client-spring-boot-starter`
- `spring-boot-starter-web`
- `spring-cloud-starter-openfeign`

### 6. PostgreSQL Databases

#### User Service Database (Port 5435)

- Database: `user_service_db`
- User: `user_service_user`
- Schema: `user` table với JWT authentication support

#### Trip Service Databases (Sharded)

- **Vietnam Shard** (Port 5433):
  - Database: `trip_service_db`
  - User: `trip_service_user`
  - Longitude >= 102.0
- **Thailand Shard** (Port 5434):
  - Database: `trip_service_db`
  - User: `trip_service_user`
  - Longitude < 102.0

**Trip Table Schema:**

- id (UUID, PK)
- passenger_id (UUID)
- driver_id (UUID, nullable)
- status (ENUM)
- pickup_latitude, pickup_longitude
- destination_latitude, destination_longitude
- fare (BigDecimal)
- requested_at, started_at, completed_at, cancelled_at (timestamps)

### 7. Redis (Port 6379)

**Công nghệ:** Redis 7 Alpine

**Chức năng:**

- Lưu trữ vị trí tài xế real-time
- Geospatial queries cho tìm kiếm tài xế gần

**Geospatial Operations:**

- Sorted Set với GEOHASH encoding
- Độ phức tạp: O(log(N)) cho radius queries
- Sub-10ms query time cho 10,000+ drivers

**Data Structure:**

- Key: `drivers:locations`
- Member: driverId
- Score: GEOHASH của (latitude, longitude)

**Why Redis:**

- In-memory performance (< 10ms queries)
- Native geospatial support
- Dễ scale và maintain
- Chi phí thấp hơn DynamoDB cho use case này

### 8. RabbitMQ (Port 5672, Management: 15672)

**Công nghệ:** RabbitMQ 3.13 Management Alpine

**Chức năng:**

- Message broker cho async communication
- Gửi thông báo chuyến đi từ Trip Service đến Driver Service

**Configuration:**

- **Exchange:** `trip.exchange` (TopicExchange)
- **Queue:** `trip.notification.queue` (Durable)
- **Routing Key:** `trip.notification`
- **Binding:** Queue ← Exchange (via routing key)

**Message Flow:**

1. Trip Service tạo trip mới với status SEARCHING_DRIVER
2. Tìm nearby drivers qua Driver Service REST API
3. Publish `TripNotificationRequest` đến RabbitMQ exchange
4. RabbitMQ route message đến queue
5. Driver Service listener consume message
6. Lưu notification vào in-memory store cho drivers

**Message Format:**

```java
TripNotificationRequest {
  UUID tripId;
  UUID passengerId;
  String passengerName;
  Double pickupLatitude;
  Double pickupLongitude;
  Double destinationLatitude;
  Double destinationLongitude;
  BigDecimal estimatedFare;
  Double distanceKm;
  List<String> nearbyDriverIds;
}
```

**Message Converter:**

- Jackson2JsonMessageConverter
- Serialize/deserialize Java objects to JSON

## Patterns Giao tiếp

### 1. REST API (Synchronous)

**Sử dụng cho:**

- Client-to-Gateway communication
- CRUD operations
- Request-response patterns

**Services:**

- API Gateway ↔ Clients
- All microservices expose REST endpoints

**Framework:**

- Spring MVC (`spring-boot-starter-web`)
- Jackson for JSON serialization

### 2. gRPC (Synchronous, High-Performance)

**Sử dụng cho:**

- High-frequency location updates
- Low latency requirements
- Binary protocol efficiency

**Implementation:**

- Driver Simulator → Driver Service (client streaming)
- Protocol Buffers for serialization
- HTTP/2 transport

**Advantages:**

- 95% nhỏ hơn về bandwidth so với REST
- ~50 bytes/update vs ~945 bytes/update (REST)
- Persistent connection với streaming
- Type-safe với Protobuf

### 3. OpenFeign (Declarative HTTP Client)

**Sử dụng cho:**

- Service-to-service REST communication
- Declarative client definitions

**Implementations:**

- Trip Service → Driver Service (find nearby drivers)
- Trip Service → User Service (validate users)
- Driver Service → Trip Service (accept trip)
- Driver Service → User Service (validate drivers)

**Configuration:**

```java
@FeignClient(name = "driver-service", url = "http://driver-service:8083")
```

### 4. RabbitMQ (Asynchronous Messaging)

**Sử dụng cho:**

- Async event notifications
- Decoupling services
- Fire-and-forget patterns

**Implementation:**

- Trip Service (Publisher) → Driver Service (Subscriber)
- Topic Exchange with routing keys
- Durable queues for reliability

**Advantages:**

- Non-blocking communication
- Retry mechanisms
- Service decoupling
- Load distribution

## Kiến trúc Dữ liệu

### Database Per Service Pattern

Mỗi microservice có database riêng:

- **User Service:** PostgreSQL (user_service_db)
- **Trip Service:** 2x PostgreSQL sharded (trip_service_db-vn, trip_service_db-th)
- **Driver Service:** Redis (geospatial data)

**Lợi ích:**

- Loose coupling
- Independent scaling
- Technology diversity
- Fault isolation

### Database Sharding (Trip Service)

**Sharding Strategy:** Geographic sharding theo longitude

**Implementation:**

- `AbstractRoutingDataSource` với `determineCurrentLookupKey()`
- ThreadLocal context holder (`DbContextHolder`)
- Automatic routing tại application layer

**Routing Logic:**

```java
if (pickupLongitude >= 102.0) {
    DbContextHolder.setDbType("VN"); // Vietnam
} else {
    DbContextHolder.setDbType("TH"); // Thailand
}
```

**Benefits:**

- Geographic data locality
- Reduced latency cho users ở từng region
- Horizontal scaling
- Isolation cho từng market

### Redis Geospatial Indexing

**Data Structure:**

- Sorted Sets với GEOHASH scoring
- Key: `drivers:locations`
- Members: driver IDs
- Scores: Geohash của coordinates

**Operations:**

```java
// Add/Update location
GEOADD drivers:locations 106.660172 10.762622 driver1

// Find nearby (5km radius, limit 10)
GEORADIUS drivers:locations 106.660172 10.762622 5 km
          WITHDIST WITHCOORD COUNT 10 ASC

// Get specific location
GEOPOS drivers:locations driver1
```

**Performance:**

- < 10ms cho radius queries
- O(log(N)) complexity
- Scales đến 100K+ drivers

## Security

### JWT Authentication

**Flow:**

1. User đăng nhập qua User Service
2. User Service xác thực credentials
3. Tạo JWT token với user ID và role
4. Client gửi token trong Authorization header
5. Services validate token và extract user context

**Implementation:**

- Secret key: Configured trong User Service
- Expiration: 24 hours
- Algorithm: HS256
- Claims: userId (subject), issued time, expiration

**Token Structure:**

```
Header: {"alg": "HS256", "typ": "JWT"}
Payload: {"sub": "user-uuid", "iat": ..., "exp": ...}
Signature: HMACSHA256(base64(header) + "." + base64(payload), secret)
```

### Authorization

**AOP-based Authorization:**

- `@RequireUser` - Yêu cầu user đã đăng nhập
- `@RequirePassenger` - Yêu cầu role PASSENGER
- `@RequireDriver` - Yêu cầu role DRIVER

**Implementation:**

- Aspect interceptors kiểm tra SecurityContext
- Throw UnauthorizedException nếu không đủ quyền

### Cross-Service Authentication

**Pattern:** Token propagation via Feign

**Implementation:**

- Services gửi Authorization header qua Feign clients
- Receiving service validate token
- Extract user context từ token

## Deployment

### Docker Compose (Local Development)

**Services:**

- 5 Application services (Gateway, User, Trip, Driver, Simulator)
- 4 Infrastructure services (Redis, RabbitMQ, 3x PostgreSQL)

**Networks:**

- Single bridge network: `microservices-network`
- Service discovery via DNS (container names)

**Health Checks:**

- All databases: `pg_isready`
- Redis: `redis-cli PING`
- RabbitMQ: `rabbitmq-diagnostics ping`
- Applications: `/actuator/health` endpoint

**Startup Order:**

1. Infrastructure services (databases, Redis, RabbitMQ)
2. Core services (User, Driver, Trip)
3. Gateway
4. Simulator

**Dependencies:**

```yaml
depends_on:
  service:
    condition: service_healthy
```

### Kubernetes (Production-Ready)

**Components:**

- Deployments cho mỗi service
- Services (ClusterIP) cho internal communication
- Databases với persistent volumes

**Configuration:**

- Environment-based configuration
- ConfigMaps cho app settings
- Secrets cho credentials

**Scaling:**

- Horizontal Pod Autoscaling cho app services
- StatefulSets cho databases

## Monitoring & Observability

### Spring Boot Actuator

**Endpoints:**

- `/actuator/health` - Health checks
- `/actuator/info` - Application info

**Exposed Endpoints:**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

**Health Indicators:**

- Database connectivity
- Redis connectivity
- RabbitMQ connectivity
- Disk space
- Custom health checks

## API Flow Examples

### 1. Tạo Chuyến Đi (Create Trip)

```
1. Client → API Gateway
   POST /api/trips/create
   Headers: Authorization: Bearer <jwt-token>
   Body: {
     pickupLatitude, pickupLongitude,
     destinationLatitude, destinationLongitude,
     estimatedFare
   }

2. API Gateway → Trip Service
   POST /api/trips/create

3. Trip Service:
   - Xác định shard (VN/TH) dựa trên pickupLongitude
   - Set DbContextHolder
   - Tạo Trip entity với status SEARCHING_DRIVER
   - Save to appropriate database

4. Trip Service → Driver Service (Feign)
   GET /api/internal/drivers/nearby?lat=...&lng=...&radiusKm=3&limit=5

5. Driver Service → Redis
   GEORADIUS drivers:locations lat lng 3 km COUNT 5

6. Driver Service → Trip Service
   Return: List<NearbyDriverResponse>

7. Trip Service → RabbitMQ
   Publish TripNotificationRequest to trip.exchange

8. Trip Service → Client
   Return: TripResponse with trip details

9. RabbitMQ → Driver Service
   Deliver message to trip.notification.queue

10. Driver Service:
    - Consume message
    - Store PendingTripNotification in-memory
    - Drivers can fetch via GET /api/drivers/notifications
```

### 2. Cập nhật Vị trí Tài xế (Driver Location Update)

```
1. Driver Simulator → Driver Service (gRPC)
   SendLocation(stream LocationRequest)
   Stream: {driverId, latitude, longitude, timestamp}

2. Driver Service → Redis
   GEOADD drivers:locations longitude latitude driverId

3. Driver Service → Driver Simulator (gRPC)
   Return: LocationResponse {status: "Location updated successfully"}
```

### 3. Chấp nhận Chuyến đi (Accept Trip)

```
1. Driver Client → API Gateway
   POST /api/drivers/notifications/{tripId}/accept
   Headers: Authorization: Bearer <driver-jwt-token>

2. API Gateway → Driver Service
   POST /api/drivers/notifications/{tripId}/accept

3. Driver Service:
   - Lấy notification từ in-memory store
   - Validate chưa expired và chưa accepted
   - Mark as accepted

4. Driver Service → Trip Service (Feign)
   POST /api/trips/{tripId}/accept
   Body: {driverId}

5. Trip Service:
   - Route đến correct shard
   - Update trip: driverId, status = DRIVER_ASSIGNED
   - Save to database

6. Trip Service → Driver Service
   Return: Success response

7. Driver Service → Client
   Return: Acceptance confirmation
```

## Architectural Decisions

Hệ thống sử dụng các Architecture Decision Records (ADR) để document các quyết định kiến trúc quan trọng:

### ADR-001: Redis vs DynamoDB for Geospatial Queries

**Decision:** Chọn Redis

**Reasons:**

- Sub-10ms query performance (vs 50-100ms DynamoDB)
- Native geospatial commands (GEOADD, GEORADIUS)
- Đơn giản hơn (không cần custom geohashing)
- Cost-effective hơn cho workload này
- In-memory performance

**Trade-offs:**

- Không persistence by default (có thể enable RDB/AOF)
- Single-point-of-failure (giải quyết bằng Redis Sentinel/Cluster)

### ADR-002: gRPC vs REST for Location Updates

**Decision:** Chọn gRPC với client streaming

**Reasons:**

- Bandwidth efficiency: ~50 bytes vs ~945 bytes per update
- HTTP/2 multiplexing
- Persistent connection giảm overhead
- Binary Protocol Buffers
- Battery efficiency cho mobile

**Use Case:**

- 10,000 drivers × 0.2 updates/sec = 2,000 updates/sec
- Tiết kiệm ~1.7 MB/sec bandwidth

### ADR-003: REST vs gRPC for CRUD Operations

**Decision:** Sử dụng cả hai, hybrid approach

**REST cho:**

- Client-facing APIs (human-readable, debug-friendly)
- CRUD operations
- Compatibility với web browsers

**gRPC cho:**

- High-frequency real-time data (location updates)
- Internal service-to-service (có thể mở rộng sau)

## Technology Stack Summary

| Component        | Technology                  | Version                       |
| ---------------- | --------------------------- | ----------------------------- |
| Language         | Java                        | 17                            |
| Framework        | Spring Boot                 | 3.5.7 - 3.5.8                 |
| Spring Cloud     | Spring Cloud                | 2025.0.0                      |
| API Gateway      | Spring Cloud Gateway        | 2025.0.0                      |
| ORM              | Spring Data JPA / Hibernate | (Spring Boot managed)         |
| Database         | PostgreSQL                  | 15                            |
| Cache/Geospatial | Redis                       | 7 Alpine                      |
| Message Broker   | RabbitMQ                    | 3.13 Alpine                   |
| RPC Framework    | gRPC / Spring gRPC          | 1.76.0 / 0.12.0               |
| Serialization    | Protocol Buffers            | 4.32.1                        |
| Build Tool       | Maven                       | 3.6+                          |
| Containerization | Docker & Docker Compose     | 20.10+                        |
| Orchestration    | Kubernetes                  | (Optional, configs available) |
| HTTP Client      | OpenFeign                   | Spring Cloud 2025.0.0         |
| Security         | Spring Security + JWT       | Spring Boot managed           |
| JWT Library      | jjwt                        | 0.11.5                        |
| Monitoring       | Spring Boot Actuator        | Spring Boot managed           |

## Ports Summary

| Service          | HTTP Port | gRPC Port | Database Port             |
| ---------------- | --------- | --------- | ------------------------- |
| API Gateway      | 8080      | -         | -                         |
| User Service     | 8081      | -         | 5435 (PostgreSQL)         |
| Trip Service     | 8082      | -         | 5433 (VN), 5434 (TH)      |
| Driver Service   | 8083      | 9092      | -                         |
| Driver Simulator | 8084      | -         | -                         |
| Redis            | -         | -         | 6379                      |
| RabbitMQ         | -         | -         | 5672 (AMQP), 15672 (Mgmt) |

## Scaling Considerations

### Horizontal Scaling

**Stateless Services:**

- API Gateway, User Service, Trip Service, Driver Service
- Có thể scale bằng replicas
- Load balancing qua Kubernetes Services

**Stateful Services:**

- Redis: Redis Cluster hoặc Sentinel
- RabbitMQ: Clustering
- PostgreSQL: Read replicas, sharding (đã implement cho Trip Service)

### Vertical Scaling

**Memory-Intensive:**

- Redis (in-memory store)
- Driver Service (in-memory notifications)

**CPU-Intensive:**

- Trip Service (distance calculations, fare estimates)
- Driver Service (geospatial queries)

### Database Scaling

**Current:**

- Geographic sharding cho Trip Service (VN/TH)

**Future Options:**

- Thêm shards cho regions khác
- Read replicas cho User Service
- Caching layer (Redis) cho frequently accessed data

## Future Enhancements

### Potential Improvements

1. **Service Mesh:** Istio hoặc Linkerd cho advanced traffic management
2. **Distributed Tracing:** Jaeger/Zipkin cho request tracing
3. **Centralized Logging:** ELK Stack (Elasticsearch, Logstash, Kibana)
4. **API Documentation:** OpenAPI/Swagger specs
5. **Rate Limiting:** Gateway-level rate limiting
6. **Circuit Breaker:** Resilience4j cho fault tolerance
7. **Event Sourcing:** Cho trip state changes
8. **CQRS:** Tách read/write models cho Trip Service
9. **WebSocket:** Real-time updates cho passengers/drivers
10. **GraphQL Gateway:** Alternative API cho flexible querying

### Performance Optimizations

1. **Caching:**

   - User profiles cache (Redis)
   - Driver status cache
   - Fare calculation cache

2. **Database:**

   - Connection pooling tuning
   - Query optimization
   - Indexes trên frequently queried fields

3. **Message Queue:**

   - Batch processing
   - Priority queues cho urgent notifications

4. **Network:**
   - gRPC cho thêm internal communications
   - HTTP/2 everywhere
   - CDN cho static content

## Conclusion

UIT-Go là một hệ thống microservices được thiết kế tốt với:

- **Separation of Concerns:** Mỗi service có trách nhiệm rõ ràng
- **Technology Diversity:** Sử dụng đúng công nghệ cho đúng use case
- **Scalability:** Horizontal và vertical scaling options
- **Resilience:** Async messaging, health checks, retry mechanisms
- **Performance:** gRPC cho high-frequency, Redis cho geospatial
- **Security:** JWT-based authentication và authorization
- **Observability:** Actuator endpoints, health checks

Kiến trúc hybrid với REST + gRPC + RabbitMQ cung cấp sự cân bằng tốt giữa:

- Developer experience (REST)
- Performance (gRPC)
- Decoupling (RabbitMQ)
