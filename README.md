# UIT-Go - Hệ thống Đặt xe Microservices

Nền tảng đặt xe dựa trên kiến trúc microservices được xây dựng bằng Spring Boot, gRPC, PostgreSQL, Redis, RabbitMQ và Docker.

## Mục lục

- [Tổng quan Hệ thống](#tổng-quan-hệ-thống)
- [Kiến trúc](#kiến-trúc)
- [Yêu cầu Cài đặt](#yêu-cầu-cài-đặt)
- [Cấu trúc Dự án](#cấu-trúc-dự-án)
- [Cài đặt](#cài-đặt)
- [Chạy Hệ thống](#chạy-hệ-thống)
- [API Endpoints](#api-endpoints)
- [Truy cập Database](#truy-cập-database)
- [Kiểm thử API](#kiểm-thử-api)
- [Xử lý Sự cố](#xử-lý-sự-cố)
- [Quy trình Phát triển](#quy-trình-phát-triển)

## Tổng quan Hệ thống

UIT-Go là hệ thống đặt xe microservices toàn diện, triển khai các patterns cloud-native hiện đại với giao thức truyền thông hybrid (REST + gRPC + RabbitMQ).

### Các Microservices

- **User Service** (Port 8081) - Quản lý người dùng, xác thực và phân quyền
- **Trip Service** (Port 8082) - Đặt chuyến đi, tính giá cước, database sharding theo địa lý
- **Driver Service** (Port 8083) - Quản lý tài xế, theo dõi vị trí real-time với Redis Geospatial
- **Driver Simulator** (Port 8084) - Mô phỏng vị trí tài xế theo thời gian thực
- **API Gateway** (Port 8080) - **Điểm truy cập duy nhất** với định tuyến thông minh

### Thành phần Hạ tầng

- **PostgreSQL** - Database riêng biệt cho từng service (database-per-service pattern)
  - User Service DB (Port 5435)
  - Trip Service DB - VN Shard (Port 5433)
  - Trip Service DB - TH Shard (Port 5434)
- **Redis** (Port 6379) - Geospatial data cho vị trí tài xế và notification storage
- **RabbitMQ** (Port 5672, Management UI: 15672) - Message broker cho thông báo chuyến đi bất đồng bộ
- **Docker** - Containerization hoàn chỉnh với Docker Compose
- **gRPC** (Port 9092) - Inter-service communication hiệu năng cao cho cập nhật vị trí

## Kiến trúc

### Patterns Truyền thông

- **REST APIs**: Client-facing endpoints qua API Gateway (Port 8080)
- **gRPC Client Streaming**: Cập nhật vị trí tài xế real-time (Driver Simulator → Driver Service)
- **RabbitMQ**: Async messaging cho thông báo chuyến đi (Trip Service → Driver Service)
- **OpenFeign**: Declarative HTTP client cho service-to-service calls
- **Redis GEO Commands**: Truy vấn geospatial cho vị trí tài xế

### Sơ đồ Kiến trúc

```
                                    ┌─────────────────┐
                                    │   Client App    │
                                    │  (Web/Mobile)   │
                                    └────────┬────────┘
                                             │
                                             │ HTTP/REST
                                             ▼
                                    ┌─────────────────┐
                                    │  API Gateway    │
                                    │   Port 8080     │◄─── TẤT CẢ requests qua đây
                                    └────────┬────────┘
                         ┌───────────────────┼───────────────────┐
                         │                   │                   │
                         ▼                   ▼                   ▼
              ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
              │  User Service    │ │  Trip Service    │ │ Driver Service   │
              │    Port 8081     │ │    Port 8082     │ │    Port 8083     │
              └────────┬─────────┘ └────────┬─────────┘ └────────┬─────────┘
                       │                    │                    │
                       │                    │                    │
                       ▼                    ▼                    ▼
              ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
              │   PostgreSQL     │ │  PostgreSQL (2)  │ │     Redis        │
              │   Port 5435      │ │  VN: 5433        │ │   Port 6379      │
              │                  │ │  TH: 5434        │ │  (Geospatial)    │
              └──────────────────┘ └──────────────────┘ └──────────────────┘
                                            │                    ▲
                                            │                    │
                                            ▼                    │
                                   ┌──────────────────┐         │
                                   │    RabbitMQ      │         │ gRPC
                                   │   Port 5672      │         │ Streaming
                                   │   (Messaging)    │         │
                                   └──────────────────┘         │
                                                                 │
                                                    ┌────────────┴─────────┐
                                                    │  Driver Simulator    │
                                                    │     Port 8084        │
                                                    └──────────────────────┘
```

### Quyết định Kiến trúc (ADR)

Dự án có các Architecture Decision Records chi tiết:

- **[ADR-001: Redis cho Geospatial](docs/ADR/001-redis-vs-dynamodb-for-geospatial.md)** - Tại sao chọn Redis thay vì DynamoDB
- **[ADR-002: gRPC cho Location Updates](docs/ADR/002-grpc-vs-rest-for-location-updates.md)** - Tại sao chọn gRPC thay vì REST
- **[ADR-003: REST cho CRUD Operations](docs/ADR/003-rest-vs-grpc-for-crud-operations.md)** - Khi nào dùng REST vs gRPC
- **[ADR-004: RabbitMQ cho Async Messaging](docs/ADR/004-rabbitmq-vs-kafka-for-async-messaging.md)** - Tại sao chọn RabbitMQ thay vì Kafka

## Yêu cầu Cài đặt

### Bắt buộc (để chạy với Docker)

- **Docker Desktop** 20.10+ (bao gồm Docker Compose) và ít nhất **4GB** memory
  ```bash
  docker --version
  docker compose version  # hoặc docker-compose --version
  ```
- **Git** - Để clone repository

### Tùy chọn (để phát triển local ngoài Docker)

- **Java 17** hoặc cao hơn
  ```bash
  java -version
  ```
- **Maven 3.6+** (Maven Wrapper đã có sẵn trong mỗi service)
- **Postman** hoặc **curl** - Kiểm thử API
- **psql** hoặc **DBeaver** - Quản lý database
- **Redis CLI** - Kiểm tra và debug Redis

## Cấu trúc Dự án

```
uit-go/
├── backend/
│   ├── api-gateway/        # Spring Cloud Gateway (Port 8080)
│   │   ├── src/
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── mvnw/mvnw.cmd
│   ├── user-service/       # Quản lý người dùng (Port 8081)
│   │   ├── src/main/java/com/example/user_service/
│   │   │   ├── controller/  # REST controllers
│   │   │   ├── service/     # Business logic
│   │   │   ├── repository/  # Data access
│   │   │   ├── entity/      # JPA entities
│   │   │   ├── jwt/         # JWT authentication
│   │   │   └── config/      # Security & CORS config
│   │   ├── pom.xml
│   │   └── Dockerfile
│   ├── trip-service/       # Quản lý chuyến đi (Port 8082)
│   │   ├── src/main/java/com/example/trip_service/
│   │   │   ├── controller/  # REST controllers
│   │   │   ├── service/     # Business logic
│   │   │   ├── repository/  # Multi-datasource (VN/TH sharding)
│   │   │   ├── entity/      # JPA entities
│   │   │   ├── config/      # OpenFeign clients, RabbitMQ, DB routing
│   │   │   └── client/      # OpenFeign interfaces
│   │   ├── pom.xml
│   │   └── Dockerfile
│   ├── driver-service/     # Quản lý tài xế (Port 8083, gRPC: 9092)
│   │   ├── src/main/
│   │   │   ├── java/com/example/driver_service/
│   │   │   │   ├── controller/  # REST controllers
│   │   │   │   ├── service/     # Business logic, Redis Geo
│   │   │   │   ├── grpc/        # gRPC service implementation
│   │   │   │   ├── listener/    # RabbitMQ listener
│   │   │   │   └── config/      # Redis, gRPC, RabbitMQ config
│   │   │   └── proto/       # Protocol Buffer definitions
│   │   ├── pom.xml
│   │   └── Dockerfile
│   └── driver-simulator/   # Mô phỏng vị trí (Port 8084)
│       ├── src/main/java/com/example/driver_simulator/
│       │   ├── controller/  # Simulator REST API
│       │   ├── simulate/    # Path generation logic
│       │   └── config/      # gRPC client config
│       ├── pom.xml
│       └── Dockerfile
├── infra/
│   ├── docker-compose.yml  # Orchestration hoàn chỉnh
│   └── k8s/               # Kubernetes deployment files (optional)
├── schema/                 # Database initialization scripts
│   ├── user-schema.sql
│   └── trip-schema.sql
├── linux-run/              # Scripts tự động hóa cho macOS/Linux
│   ├── start.sh           # Khởi động nhanh tất cả services
│   └── stop.sh            # Dừng tất cả containers
├── win-run/                # Scripts tự động hóa cho Windows
│   ├── rebuild-all.bat
│   ├── restart-docker.bat
│   └── demo-service-integration.bat
└── docs/                   # Tài liệu chi tiết
    ├── ARCHITECTURE.md     # Kiến trúc hệ thống (Tiếng Việt)
    ├── ADR/               # Architecture Decision Records
    │   ├── 001-redis-vs-dynamodb-for-geospatial.md
    │   ├── 002-grpc-vs-rest-for-location-updates.md
    │   ├── 003-rest-vs-grpc-for-crud-operations.md
    │   └── 004-rabbitmq-vs-kafka-for-async-messaging.md
    └── testing-guide/
        ├── API_ENDPOINTS.md
        └── redis-grpc-testing-commands.md
```

## Cài đặt

Làm theo các bước sau để cài đặt và chạy toàn bộ hệ thống với Docker:

1. **Clone repository**

   ```bash
   git clone https://github.com/octguy/uit-go.git
   cd uit-go
   ```

2. **Khởi động Docker Desktop** và xác nhận đang chạy:

   ```bash
   docker ps
   ```

3. **Chạy script tự động build + start**

   **macOS/Linux:**

   ```bash
   cd linux-run
   chmod +x start.sh stop.sh
   ./start.sh
   ```

   **Windows (PowerShell hoặc Command Prompt):**

   ```cmd
   cd win-run
   rebuild-all.bat
   ```

   Scripts này sẽ:

   - Dừng các containers cũ
   - Build tất cả service images (sử dụng Maven wrapper trong Docker)
   - Khởi động toàn bộ stack
   - Hiển thị trạng thái containers

4. **Xác minh hệ thống**

   ```bash
   cd infra
   docker-compose ps          # Trạng thái containers
   docker-compose logs --tail=50 api-gateway  # Logs mẫu
   ```

5. **Dừng khi hoàn thành**
   ```bash
   cd infra
   docker-compose down        # Giữ data volumes
   # Hoặc để reset toàn bộ (bao gồm dữ liệu Postgres/Redis):
   docker-compose down -v
   ```

> **Lưu ý**: Nếu muốn build ngoài Docker, Maven wrapper nằm trong mỗi service (ví dụ: `backend/user-service/mvnw`).

## Chạy Hệ thống

### Khởi động Nhanh với Docker (Khuyến nghị)

**macOS/Linux:**

```bash
cd linux-run && ./start.sh
```

**Windows:**

```bash
cd win-run && rebuild-all.bat
```

**Script này thực hiện:**

- Dừng tất cả containers UIT-Go đang chạy
- Build fresh images cho mọi service
- Khởi động toàn bộ stack với Docker Compose
- Hiển thị containers đang chạy và endpoints chính

### Khởi động Thủ công với Docker Compose

```bash
cd infra
docker-compose up -d --build   # Build images và start
docker-compose ps              # Kiểm tra trạng thái
docker-compose logs -f         # Xem tất cả logs
docker-compose logs -f user-service  # Xem logs một service
docker-compose down            # Dừng (giữ data)
docker-compose down -v         # Dừng và xóa data volumes
```

### Phát triển Service Riêng lẻ

Chạy một service đơn lẻ local (không dùng Docker) để phát triển:

```bash
# Di chuyển đến thư mục service
cd backend/user-service

# Chạy với Maven wrapper (macOS/Linux)
./mvnw spring-boot:run

# Chạy với Maven wrapper (Windows)
mvnw.cmd spring-boot:run
```

**Lưu ý**: Khi chạy services locally, đảm bảo:

- Databases PostgreSQL có thể truy cập được (qua Docker hoặc cài đặt local)
- Redis đang chạy (cho Driver Service)
- Cập nhật `application.yml` với connection strings đúng

## API Endpoints

### 🔑 Quan trọng: TẤT CẢ requests từ client PHẢI đi qua API Gateway (Port 8080)

### Ports của Services

| Service          | HTTP Port | gRPC Port | URL qua Gateway           | URL trực tiếp (chỉ internal)     |
| ---------------- | --------- | --------- | ------------------------- | -------------------------------- |
| **API Gateway**  | **8080**  | -         | **http://localhost:8080** | **← SỬ DỤNG PORT NÀY**           |
| User Service     | 8081      | -         | Qua Gateway               | http://localhost:8081 (internal) |
| Trip Service     | 8082      | -         | Qua Gateway               | http://localhost:8082 (internal) |
| Driver Service   | 8083      | 9092      | Qua Gateway               | http://localhost:8083 (internal) |
| Driver Simulator | 8084      | -         | Qua Gateway               | http://localhost:8084 (testing)  |
| RabbitMQ UI      | 15672     | -         | http://localhost:15672    | guest/guest                      |

### Health Checks

Kiểm tra tất cả services đang chạy:

```bash
# Qua API Gateway (Recommended)
curl http://localhost:8080/actuator/health

# Kiểm tra từng service trực tiếp
curl http://localhost:8081/actuator/health  # User Service
curl http://localhost:8082/actuator/health  # Trip Service
curl http://localhost:8083/actuator/health  # Driver Service
curl http://localhost:8084/actuator/health  # Driver Simulator
```

### Endpoints API Chính (QUA API GATEWAY - PORT 8080)

#### 👤 Quản lý Người dùng

**Tất cả requests đi qua: `http://localhost:8080`**

```bash
# Đăng ký người dùng mới (Passenger)
POST   http://localhost:8080/api/users/register

# Đăng nhập
POST   http://localhost:8080/api/users/login

# Lấy thông tin profile
GET    http://localhost:8080/api/users/me
Header: Authorization: Bearer <JWT-TOKEN>

# Cập nhật profile
PUT    http://localhost:8080/api/users/profile
Header: Authorization: Bearer <JWT-TOKEN>

# Lấy tất cả users
GET    http://localhost:8080/api/users
```

**Request body mẫu - Đăng ký:**

```json
{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**Request body mẫu - Đăng nhập:**

```json
{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

#### 🚗 Quản lý Chuyến đi

**Tất cả requests đi qua: `http://localhost:8080`**

```bash
# Ước tính giá cước
POST   http://localhost:8080/api/trips/estimate-fare

# Tạo chuyến đi mới
POST   http://localhost:8080/api/trips/create
Header: Authorization: Bearer <PASSENGER-TOKEN>

# Lấy thông tin chuyến đi
GET    http://localhost:8080/api/trips/{tripId}
Header: Authorization: Bearer <TOKEN>

# Hủy chuyến đi
POST   http://localhost:8080/api/trips/{tripId}/cancel
Header: Authorization: Bearer <TOKEN>

# Chấp nhận chuyến đi (Driver)
POST   http://localhost:8080/api/trips/{tripId}/accept
Header: Authorization: Bearer <DRIVER-TOKEN>

# Bắt đầu chuyến đi
POST   http://localhost:8080/api/trips/{tripId}/start
Header: Authorization: Bearer <DRIVER-TOKEN>

# Hoàn thành chuyến đi
POST   http://localhost:8080/api/trips/{tripId}/complete
Header: Authorization: Bearer <DRIVER-TOKEN>

# Đánh giá chuyến đi
POST   http://localhost:8080/api/trips/{tripId}/rate
Header: Authorization: Bearer <PASSENGER-TOKEN>

# Lịch sử chuyến đi
GET    http://localhost:8080/api/trips/history
Header: Authorization: Bearer <TOKEN>
```

**Request body mẫu - Ước tính giá:**

```json
{
  "pickupLatitude": 10.762622,
  "pickupLongitude": 106.660172,
  "destinationLatitude": 10.775818,
  "destinationLongitude": 106.695595
}
```

**Request body mẫu - Tạo chuyến đi:**

```json
{
  "pickupLatitude": 10.762622,
  "pickupLongitude": 106.660172,
  "destinationLatitude": 10.775818,
  "destinationLongitude": 106.695595,
  "estimatedFare": 45000
}
```

#### 🚕 Quản lý Tài xế

**Tất cả requests đi qua: `http://localhost:8080`**

```bash
# Đăng ký tài xế
POST   http://localhost:8080/api/drivers/register

# Cập nhật trạng thái tài xế (AVAILABLE/BUSY/OFFLINE)
POST   http://localhost:8080/api/drivers/status
Header: Authorization: Bearer <DRIVER-TOKEN>

# Tìm tài xế gần khu vực
GET    http://localhost:8080/api/drivers/nearby?latitude=10.762622&longitude=106.660172&radius=5

# Lấy thông báo chuyến đi
GET    http://localhost:8080/api/drivers/{driverId}/notifications
Header: Authorization: Bearer <DRIVER-TOKEN>

# Chấp nhận thông báo chuyến đi
POST   http://localhost:8080/api/drivers/notifications/{tripId}/accept
Header: Authorization: Bearer <DRIVER-TOKEN>
```

**Request body mẫu - Đăng ký tài xế:**

```json
{
  "email": "driver@example.com",
  "password": "SecurePass123",
  "name": "Tran Van B",
  "phone": "+84907654321",
  "vehicleType": "SEDAN",
  "licensePlate": "59A-12345"
}
```

#### 🎯 Driver Simulator (Testing)

```bash
# Bắt đầu mô phỏng tài xế
curl -s -X POST "http://localhost:8084/api/simulate/start-all?startLat=10.762622&startLng=106.660172&endLat=10.776889&endLng=106.700806&steps=200&delayMillis=1000"
```

## Truy cập Database

Mỗi service sử dụng PostgreSQL database riêng theo pattern database-per-service của microservices.

### Cấu hình Database

| Service           | Database Name   | Username          | Password          | Port | Container Name     |
| ----------------- | --------------- | ----------------- | ----------------- | ---- | ------------------ |
| User Service      | user_service_db | user_service_user | user_service_pass | 5435 | user-service-db    |
| Trip Service (VN) | trip_service_db | trip_service_user | trip_service_pass | 5433 | trip-service-db-vn |
| Trip Service (TH) | trip_service_db | trip_service_user | trip_service_pass | 5434 | trip-service-db-th |

### Kết nối qua psql

```bash
# User Service Database
psql -h localhost -p 5435 -U user_service_user -d user_service_db
# Password: user_service_pass

# Trip Service Database (Vietnam Shard)
psql -h localhost -p 5433 -U trip_service_user -d trip_service_db
# Password: trip_service_pass

# Trip Service Database (Thailand Shard)
psql -h localhost -p 5434 -U trip_service_user -d trip_service_db
# Password: trip_service_pass
```

### Kết nối qua Docker

```bash
# User Service Database
docker exec -it user-service-db psql -U user_service_user -d user_service_db

# Trip Service Database (VN)
docker exec -it trip-service-db-vn psql -U trip_service_user -d trip_service_db

# Trip Service Database (TH)
docker exec -it trip-service-db-th psql -U trip_service_user -d trip_service_db
```

### Kết nối qua GUI Tools (DBeaver, pgAdmin, DataGrip)

Tạo connection PostgreSQL mới với:

**User Service DB:**

- **Host:** localhost
- **Port:** 5435
- **Database:** user_service_db
- **Username:** user_service_user
- **Password:** user_service_pass

**Trip Service DB (Vietnam):**

- **Host:** localhost
- **Port:** 5433
- **Database:** trip_service_db
- **Username:** trip_service_user
- **Password:** trip_service_pass

**Trip Service DB (Thailand):**

- **Host:** localhost
- **Port:** 5434
- **Database:** trip_service_db
- **Username:** trip_service_user
- **Password:** trip_service_pass

### Truy cập Redis

Driver Service sử dụng Redis cho dữ liệu geospatial và notification storage.

```bash
# Kết nối đến Redis CLI
docker exec -it redis redis-cli

# Test kết nối
PING  # Sẽ trả về PONG

# Kiểm tra vị trí tài xế
GEORADIUS drivers:locations 106.660172 10.762622 5 km WITHDIST

# Xem tất cả keys
KEYS *

# Xem pending trip notifications
KEYS pending_trips:*

# Lấy chi tiết một notification
GET pending_trips:<driverId>:<tripId>
```

### Truy cập RabbitMQ Management UI

```bash
# Mở trình duyệt
http://localhost:15672

# Đăng nhập
Username: guest
Password: guest
```

**Trong Management UI có thể:**

- Xem queues và số lượng messages
- Monitor message rates (in/out)
- Publish test messages
- Xem exchanges và bindings
- Purge queues nếu cần

## Kiểm thử API

### Ví dụ: Đăng ký và Xác thực Người dùng

**⚠️ Quan trọng: Tất cả requests đi qua port 8080 (API Gateway)**

```bash
# 1. Đăng ký người dùng mới
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nguyen.van.a@example.com",
    "password": "MatKhau123",
  }'

# 2. Đăng nhập để lấy JWT token
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nguyen.van.a@example.com",
    "password": "MatKhau123"
  }'

# Response sẽ chứa JWT token:
# {
#   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
# }

# 3. Sử dụng token cho các requests cần xác thực
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```

### Ví dụ: Tạo Chuyến đi

```bash
# 1. Ước tính giá cước trước
curl -X POST http://localhost:8080/api/trips/estimate-fare \
  -H "Content-Type: application/json" \
  -d '{
    "pickupLatitude": 10.762622,
    "pickupLongitude": 106.660172,
    "destinationLatitude": 10.775818,
    "destinationLongitude": 106.695595
  }'

# Response:
# {
#   "distanceKm": 2.5,
#   "estimatedFare": 45000,
# }

# 2. Tạo chuyến đi mới (cần token của passenger)
curl -X POST http://localhost:8080/api/trips/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $PASSENGER_TOKEN" \
  -d '{
    "passengerId": "123e4567-e89b-12d3-a456-426614174000",
    "pickupAddress": "268 Lý Thường Kiệt, Quận 10, TP.HCM",
    "destinationAddress": "Vincom Center, Đồng Khởi, Quận 1",
    "pickupLatitude": 10.762622,
    "pickupLongitude": 106.660172,
    "destinationLatitude": 10.775818,
    "destinationLongitude": 106.695595,
    "estimatedFare": 45000
  }'

# Response:
# {
#   "id": "trip-uuid",
#   "status": "SEARCHING_DRIVER",
#   "estimatedFare": 45000,
#   "createdAt": "2025-11-29T10:30:00"
# }

# 3. Lấy thông tin chuyến đi
curl http://localhost:8080/api/trips/{trip-id} \
  -H "Authorization: Bearer $TOKEN"
```

### Ví dụ: Test Flow Hoàn chỉnh - Tạo Chuyến đi và Thông báo Tài xế

**Script tự động:** `linux-run/test-notify-trip.sh`

Script này test toàn bộ flow từ tạo chuyến đi đến gửi thông báo cho tài xế gần nhất qua RabbitMQ.

**Chạy script:**

```bash
cd linux-run
chmod +x test-notify-trip.sh
./test-notify-trip.sh
```

**Flow của script:**

1. **Setup drivers**: Đưa tất cả drivers online và start simulation
2. **Passenger login**: Đăng nhập để lấy JWT token
3. **Tìm tài xế gần**: Gọi API tìm tài xế trong bán kính 3km
4. **Tạo chuyến đi**: POST /api/trips/create
5. **RabbitMQ xử lý**: Trip Service publish notification đến RabbitMQ
6. **Driver Service nhận**: Consume message và lưu vào Redis với TTL=15s
7. **Kiểm tra thông báo**: Verify tài xế gần nhất nhận được thông báo
8. **Kiểm tra Redis**: Verify pending notification trong Redis

**Ví dụ output:**

```bash
==========================================================
  Auto Trip Creation & Driver Notification Test
==========================================================

Step 1: Logging in as passenger...
Email: user1@gmail.com
✅ Login successful!
Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

Step 2: Finding nearby drivers at pickup location...
Pickup Location: (10.762622, 106.660172)
✅ Found 3 nearby driver(s)

  • Driver ID: 550e8400-e29b-41d4-a716-446655440001
    Distance: 245m | Location: (10.764000, 106.661500)

Step 3: Creating trip...
✅ Trip created successfully!
Trip ID: 123e4567-e89b-12d3-a456-426614174000
Status: SEARCHING_DRIVER

Step 4: Waiting for RabbitMQ to process notification...
✅ Ready

Step 5: Checking trip-service logs for notified driver...
Trip 123e4567 created and notification sent to nearest driver: 550e8400-e29b-41d4-a716-446655440001
✅ Nearest driver notified: 550e8400-e29b-41d4-a716-446655440001

Step 6: Verifying nearest driver received notification...
✅ Nearest driver has 1 pending trip(s)
    Trip ID: 123e4567-e89b-12d3-a456-426614174000
    Fare: 50000 VND
    Distance: 2.5 km
    Expires at: 2025-11-29T10:45:15

Step 7: Checking Redis for pending notifications...
✅ Found pending notifications in Redis:
  • Driver: 550e8400-e29b-41d4-a716-446655440001 | TTL: 13s

SUMMARY
✅ Verification: Nearest driver was correctly notified
```

**Variables có thể customize:**

```bash
# Custom passenger credentials
PASSENGER_EMAIL="custom@email.com" PASSENGER_PASSWORD="password" ./test-notify-trip.sh

# Custom coordinates
PICKUP_LAT=10.762622 PICKUP_LNG=106.660172 ./test-notify-trip.sh

# Custom fare
FARE=75000 ./test-notify-trip.sh
```

### Ví dụ: Test Tài xế Chấp nhận Chuyến đi

**Script tự động:** `linux-run/test-accept-trip.sh`

Script này test flow hoàn chỉnh: tạo chuyến đi → tài xế nhận thông báo → tài xế chấp nhận.

**Chạy script:**

```bash
cd linux-run
chmod +x test-accept-trip.sh
./test-accept-trip.sh
```

**Flow của script:**

1. **Setup drivers**: Online all drivers và start location simulation
2. **Passenger login**: Lấy passenger JWT token
3. **Tìm tài xế gần nhất**: Query nearby drivers
4. **Driver login**: Lấy driver JWT token (tài xế gần nhất)
5. **Tạo chuyến đi**: Passenger creates trip
6. **RabbitMQ notification**: Automatic async notification
7. **Check pending trips**: Verify driver nhận được notification
8. **Driver accepts**: POST /api/trips/{tripId}/accept
9. **Verify assignment**: Check trip được assign cho driver

**Ví dụ output:**

```bash
==========================================================
  Test: Driver Accepts Trip & Trip Assignment
==========================================================

Step 1: Logging in as passenger...
✅ Login successful!

Step 2: Finding nearby drivers at pickup location...
✅ Found 3 nearby driver(s)
Nearest driver ID: 550e8400-e29b-41d4-a716-446655440001

Step 3: Getting driver user information...
✅ Driver info retrieved
Driver Name: Nguyen Van A
Driver Email: driver1@gmail.com

Step 4: Logging in as driver...
✅ Driver login successful!

Step 5: Creating trip as passenger...
✅ Trip created successfully!
Trip ID: 123e4567-e89b-12d3-a456-426614174000
Status: SEARCHING_DRIVER

Step 6: Waiting for RabbitMQ to process notification...
✅ Ready

Step 7: Checking pending trips for nearest driver...
✅ Driver has 1 pending trip(s)

  • Trip ID: 123e4567-e89b-12d3-a456-426614174000
    Passenger: Tran Thi B
    Fare: 50000 VND
    Distance: 2.5 km
    Expires at: 2025-11-29T10:50:30

✅ Our trip 123e4567 is in the pending list

Step 8: Driver accepting trip...
✅ Trip accepted successfully!
New Status: DRIVER_ASSIGNED
Assigned Driver: 550e8400-e29b-41d4-a716-446655440001

Step 9: Verifying trip assignment...
✅ SUCCESS: Trip is assigned to driver 550e8400-e29b-41d4-a716-446655440001
✅ Trip status updated to: DRIVER_ASSIGNED

SUMMARY
Trip ID: 123e4567-e89b-12d3-a456-426614174000
Passenger: user1@gmail.com
Driver ID: 550e8400-e29b-41d4-a716-446655440001
Driver Email: driver1@gmail.com
Trip Status: DRIVER_ASSIGNED

✅ ALL TESTS PASSED!
```

### Ví dụ: Test TTL Expiration - Thông báo Hết hạn sau 15 giây

**Script tự động:** `linux-run/trip-expired-ttl.sh`

Script này test behavior khi tài xế cố chấp nhận chuyến đi SAU KHI notification đã expire (>15 giây).

**Chạy script:**

```bash
cd linux-run
chmod +x trip-expired-ttl.sh
./trip-expired-ttl.sh
```

**Flow của script:**

1. **Setup và login**: Passenger + Driver login
2. **Tạo chuyến đi**: Create trip → RabbitMQ notification sent
3. **Check before expiration**: Verify notification tồn tại trong Redis
4. **Đợi 15 giây**: Countdown timer cho TTL expire
5. **Check after expiration**: Verify notification đã bị xóa khỏi Redis
6. **Attempt to accept**: Driver cố accept trip đã expired
7. **Verify result**: Kiểm tra trip status và driver assignment

**Ví dụ output:**

```bash
==========================================================
  Test: Driver Accepts Trip AFTER Expiration (>15s)
==========================================================

Step 5: Creating trip as passenger...
✅ Trip created successfully!
Trip ID: 123e4567-e89b-12d3-a456-426614174000
Status: SEARCHING_DRIVER
Created at: 2025-11-29 10:55:00

Step 7: Checking pending trips immediately (before expiration)...
✅ Driver has 1 pending trip(s) BEFORE expiration

  • Trip ID: 123e4567-e89b-12d3-a456-426614174000
    Passenger: Tran Thi B
    Fare: 50000 VND
    Expires at: 2025-11-29T10:55:15

Step 8: Waiting for notification to EXPIRE...
Notification TTL: 15 seconds

⏳ Waiting... 15 seconds remaining
⏳ Waiting... 14 seconds remaining
...
⏳ Waiting... 1 second remaining

✅ 15 seconds elapsed - Notification should be EXPIRED now!

Step 9: Checking pending trips AFTER expiration...
✅ EXPECTED: Pending trips list is EMPTY (notification expired)

Step 10: Driver attempting to accept EXPIRED trip...
Driver ID: 550e8400-e29b-41d4-a716-446655440001
Time since creation: >15 seconds

Accept Response:
{
  "message": "Trip not found or already assigned",
  "status": 404
}
❌ Trip acceptance failed!
Error: Trip not found or already assigned

This could be because:
  - Another driver already accepted
  - Trip was cancelled
  - Trip status changed

TEST SUMMARY
==========================================================
Timeline:
  1. Trip created at: 2025-11-29 10:55:00
  2. Notification sent to Redis (TTL=15s)
  3. Waited >15 seconds for expiration
  4. Driver attempted to accept expired trip

Results:
  - Pending trips before expiration: 1
  - Pending trips after expiration: 0
  - Final trip status: SEARCHING_DRIVER
  - Final driver assignment: null

Key Learning:
  Redis notification TTL (15s) only affects the pending notification list.
  Trip acceptance in trip-service may still work if trip status allows it.

✅ Test completed!
```

**Key Points về TTL:**

- **Redis TTL = 15 giây**: Notification tự động expire sau 15s
- **Pending list empty**: Sau 15s, GET /api/drivers/trips/pending trả về empty
- **Trip vẫn tồn tại**: Trip entity vẫn còn trong database với status SEARCHING_DRIVER
- **Accept có thể thành công**: Tùy business logic, driver vẫn có thể accept nếu trip status cho phép

### Flow Hoàn chỉnh: Từ Đăng ký đến Hoàn thành Chuyến đi

```bash
# === BƯỚC 1: Đăng ký Passenger ===
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "passenger@example.com",
    "password": "Pass123",
    "name": "Nguyen Van A",
    "phone": "+84901111111",
    "userType": "PASSENGER"
  }'

# === BƯỚC 2: Đăng ký Driver ===
curl -X POST http://localhost:8080/api/drivers/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "driver@example.com",
    "password": "Pass123",
    "name": "Tran Van B",
    "phone": "+84902222222",
    "vehicleType": "SEDAN",
    "licensePlate": "59A-12345"
  }'

# === BƯỚC 3: Passenger Login ===
PASSENGER_TOKEN=$(curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"email": "passenger@example.com", "password": "Pass123"}' \
  | jq -r '.token')

# === BƯỚC 4: Driver Login ===
DRIVER_TOKEN=$(curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"email": "driver@example.com", "password": "Pass123"}' \
  | jq -r '.token')

# === BƯỚC 5: Start Driver Simulator ===
curl -X POST http://localhost:8084/api/simulate/start \
  -H "Content-Type: application/json" \
  -d '{
    "driverId": "driver-uuid",
    "startLat": 10.762622,
    "startLng": 106.660172,
    "endLat": 10.775818,
    "endLng": 106.695595,
    "speedKmh": 40
  }'

# === BƯỚC 6: Passenger tạo chuyến đi ===
TRIP_ID=$(curl -X POST http://localhost:8080/api/trips/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $PASSENGER_TOKEN" \
  -d '{
    "passengerId": "passenger-uuid",
    "pickupAddress": "268 Lý Thường Kiệt",
    "destinationAddress": "Vincom Center",
    "pickupLatitude": 10.762622,
    "pickupLongitude": 106.660172,
    "destinationLatitude": 10.775818,
    "destinationLongitude": 106.695595,
    "estimatedFare": 45000
  }' | jq -r '.id')

# === BƯỚC 7: Driver nhận thông báo (qua RabbitMQ) ===
# Driver Service tự động nhận notification và lưu vào Redis

# === BƯỚC 8: Driver lấy danh sách notifications ===
curl http://localhost:8080/api/drivers/driver-uuid/notifications \
  -H "Authorization: Bearer $DRIVER_TOKEN"

# === BƯỚC 9: Driver chấp nhận chuyến đi ===
curl -X POST http://localhost:8080/api/drivers/notifications/$TRIP_ID/accept \
  -H "Authorization: Bearer $DRIVER_TOKEN"

# === BƯỚC 10: Driver bắt đầu chuyến đi ===
curl -X POST http://localhost:8080/api/trips/$TRIP_ID/start \
  -H "Authorization: Bearer $DRIVER_TOKEN"

# === BƯỚC 11: Driver hoàn thành chuyến đi ===
curl -X POST http://localhost:8080/api/trips/$TRIP_ID/complete \
  -H "Authorization: Bearer $DRIVER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "actualFare": 48000,
    "completedAt": "2025-11-29T11:00:00"
  }'

# === BƯỚC 12: Passenger đánh giá chuyến đi ===
curl -X POST http://localhost:8080/api/trips/$TRIP_ID/rate \
  -H "Authorization: Bearer $PASSENGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 5,
    "comment": "Tài xế lái xe rất tốt!"
  }'
```

Để biết thêm ví dụ kiểm thử chi tiết, xem:

- [docs/testing-guide/API_ENDPOINTS.md](docs/testing-guide/API_ENDPOINTS.md)
- [docs/testing-guide/redis-grpc-testing-commands.md](docs/testing-guide/redis-grpc-testing-commands.md)

## Xử lý Sự cố

### Vấn đề Thường gặp

#### 1. Port Đã được Sử dụng

Nếu gặp xung đột port:

```bash
# Kiểm tra port đang được sử dụng (macOS/Linux)
lsof -i :8080

# Kiểm tra trên Windows
netstat -ano | findstr :8080

# Kill process (macOS/Linux)
kill -9 <PID>

# Kill process (Windows)
taskkill /PID <PID> /F
```

Hoặc thay đổi ports trong `docker-compose.yml`:

```yaml
ports:
  - "8085:8080" # Map external port 8085 sang internal 8080
```

#### 2. Docker Build Thất bại

```bash
# Dọn dẹp Docker system
docker system prune -a -f

# Xóa volumes
docker volume prune -f

# Rebuild từ đầu
cd infra
docker-compose down -v
docker-compose up --build --force-recreate
```

#### 3. Maven Build Thất bại

```bash
# Clean và rebuild service cụ thể
cd backend/user-service
./mvnw clean install -DskipTests

# Force update dependencies
./mvnw clean install -U

# Xóa Maven cache (nếu bị corrupt)
rm -rf ~/.m2/repository
```

#### 4. Services Không Khởi động

```bash
# Kiểm tra Docker container logs
docker-compose logs user-service
docker-compose logs trip-service-db-vn

# Kiểm tra trạng thái tất cả containers
docker-compose ps

# Restart service cụ thể
docker-compose restart user-service

# Rebuild service cụ thể
docker-compose up -d --build user-service
```

#### 5. Vấn đề Kết nối Database

**Triệu chứng**: Service khởi động nhưng không connect được database

**Giải pháp**:

```bash
# Kiểm tra database containers đang chạy
docker-compose ps

# Kiểm tra database logs
docker-compose logs user-service-db

# Xác minh credentials trong application.yml khớp với docker-compose.yml

# Đợi database sẵn sàng (health checks)
docker-compose up -d --wait

# Restart database containers
docker-compose restart user-service-db trip-service-db-vn trip-service-db-th
```

#### 6. Vấn đề Kết nối Redis

```bash
# Kiểm tra Redis đang chạy
docker-compose ps redis

# Test kết nối Redis
docker exec -it redis redis-cli ping
# Phải trả về: PONG

# Kiểm tra Redis logs
docker-compose logs redis

# Xóa dữ liệu Redis
docker exec -it redis redis-cli FLUSHALL
```

#### 7. Vấn đề RabbitMQ

```bash
# Kiểm tra RabbitMQ đang chạy
docker-compose ps rabbitmq

# Kiểm tra RabbitMQ logs
docker-compose logs rabbitmq

# Truy cập Management UI
# Mở browser: http://localhost:15672
# Login: guest/guest

# Restart RabbitMQ
docker-compose restart rabbitmq

# Purge queue (xóa messages trong queue)
# Qua Management UI: Queues → trip.notification.queue → Purge
```

#### 8. Lỗi gRPC Communication

**Cho Driver Service gRPC**:

```bash
# Kiểm tra gRPC port 9092 có thể truy cập
telnet localhost 9092

# Kiểm tra Driver Service logs
docker-compose logs driver-service

# Xác minh gRPC stub configuration trong client services
# Kiểm tra GrpcClientConfig.java trong driver-simulator

# Restart cả driver-service và driver-simulator
docker-compose restart driver-service driver-simulator
```

#### 9. Out of Memory Errors

```bash
# Tăng Docker memory allocation
# Docker Desktop > Settings > Resources > Memory (khuyến nghị 4GB+)

# Set JVM heap size trong Dockerfile
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Restart Docker Desktop
```

#### 10. Permission Denied (macOS/Linux)

```bash
# Làm cho scripts có thể execute
cd linux-run
chmod +x *.sh

# Hoặc chạy với bash rõ ràng
bash start.sh
```

### Kiểm tra Health Services

```bash
# Health check nhanh tất cả services (qua API Gateway)
curl http://localhost:8080/actuator/health

# Kiểm tra từng service trực tiếp
curl http://localhost:8081/actuator/health  # User Service
curl http://localhost:8082/actuator/health  # Trip Service
curl http://localhost:8083/actuator/health  # Driver Service
curl http://localhost:8084/actuator/health  # Driver Simulator

# Xem trạng thái tất cả containers
docker-compose ps

# Monitor logs real-time
docker-compose logs -f

# Xem logs của service cụ thể
docker-compose logs -f trip-service --tail=100
```

### Reset Hệ thống Hoàn toàn

Nếu mọi cách đều thất bại, thực hiện reset hoàn toàn:

```bash
# Dừng và xóa tất cả containers, networks, volumes
cd infra
docker-compose down -v

# Xóa Docker images
docker rmi $(docker images 'uit-go*' -q)

# Rebuild toàn bộ
cd ../linux-run  # hoặc win-run trên Windows
./start.sh
```

## Quy trình Phát triển

### Thay đổi Code của Service

1. **Chỉnh sửa Code Service**

   ```bash
   # Chỉnh sửa files trong backend/<service-name>/src/
   # Ví dụ: backend/user-service/src/main/java/com/example/user_service/
   ```

2. **Rebuild Service**

   ```bash
   cd backend/<service-name>
   ./mvnw clean package -DskipTests
   ```

3. **Restart Container**

   ```bash
   cd ../../infra
   docker-compose restart <service-name>

   # Hoặc rebuild container image
   docker-compose up -d --build <service-name>
   ```

### Rebuild Toàn bộ Hệ thống

Khi có thay đổi đáng kể trên nhiều services:

**macOS/Linux:**

```bash
cd linux-run
./start.sh
```

**Windows:**

```bash
cd win-run
rebuild-all.bat
```

Script này sẽ:

- Dừng tất cả containers đang chạy
- Build tất cả services với Maven
- Rebuild và restart Docker containers
- Hiển thị trạng thái health của services

### Best Practices Phát triển

1. **Hot Reload cho Development**

   - Thêm Spring Boot DevTools dependency để tự động restart
   - Chạy services locally với `./mvnw spring-boot:run`

2. **Database Migrations**

   - Schema changes nên đặt trong thư mục `schema/`
   - Test migrations locally trước khi deploy

3. **Testing**

   ```bash
   # Chạy tests cho service cụ thể
   cd backend/user-service
   ./mvnw test

   # Chạy tests với coverage
   ./mvnw test jacoco:report
   ```

4. **Logging**

   ```bash
   # Xem service logs
   docker-compose logs -f user-service

   # Xem 100 dòng cuối cùng
   docker-compose logs --tail=100 user-service

   # Xem logs của tất cả services
   docker-compose logs -f
   ```

5. **Code Quality**
   - Tuân theo Java coding conventions
   - Sử dụng commit messages có ý nghĩa
   - Test endpoints trước khi commit

## Công nghệ Sử dụng

### Backend Services

- **Spring Boot 3.5.7** - Framework ứng dụng chính
- **Spring Cloud Gateway** - API Gateway và routing
- **Spring Data JPA** - Database ORM
- **Spring Security** - Authentication và authorization
- **Spring gRPC** - gRPC server/client support
- **Spring AMQP** - RabbitMQ integration
- **OpenFeign** - Declarative HTTP client
- **JWT (jsonwebtoken)** - Token-based authentication

### Communication

- **gRPC 1.76.x** - High-performance RPC framework
- **Protocol Buffers** - Data serialization
- **REST** - HTTP-based APIs
- **RabbitMQ 3.13** - Message broker AMQP

### Data Storage

- **PostgreSQL 15** - Relational database
  - Database sharding theo địa lý (VN/TH)
  - Multi-datasource routing động
- **Redis 7** - In-memory data store
  - Geospatial commands (GEOADD, GEORADIUS)
  - TTL-based notification storage

### Build & Deployment

- **Maven** - Dependency management và build tool
- **Docker** - Container platform
- **Docker Compose** - Multi-container orchestration

### Development Tools

- **Lombok** - Giảm boilerplate code
- **Spring Boot Actuator** - Production-ready monitoring
- **MapStruct** - Bean mapping (tùy chọn)

## Tính năng Dự án

### Tính năng Đã Triển khai

✅ **Quản lý Người dùng**

- Đăng ký và xác thực người dùng
- JWT-based security
- Role-based access control (Passenger/Driver)
- Quản lý profile

✅ **Quản lý Chuyến đi**

- Tạo yêu cầu chuyến đi
- Theo dõi trạng thái chuyến đi (SEARCHING_DRIVER, DRIVER_ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED)
- Tính toán giá cước
- Lịch sử chuyến đi
- **Database sharding theo địa lý** (VN/TH based on pickup location)

✅ **Driver Service**

- Đăng ký và xác minh tài xế
- Theo dõi vị trí real-time với Redis GEO
- Trạng thái khả dụng tài xế (AVAILABLE, BUSY, OFFLINE)
- Tìm kiếm tài xế gần nhất (geospatial queries)
- Cập nhật vị trí qua gRPC streaming
- **Nhận thông báo chuyến đi qua RabbitMQ**
- **In-memory pending trip notifications với TTL**

✅ **Driver Simulator**

- Mô phỏng di chuyển tài xế tự động
- Path generation giữa các waypoints
- Cập nhật vị trí real-time qua gRPC
- Hỗ trợ mô phỏng nhiều tài xế

✅ **API Gateway**

- Routing tập trung
- Path rewriting cho service context paths
- Health monitoring

✅ **Async Messaging (RabbitMQ)**

- Trip notification từ Trip Service đến Driver Service
- Durable queues với ACK/NACK
- Automatic retry mechanism
- Dead Letter Queue
- Management UI để monitoring

✅ **Infrastructure**

- Docker containerization
- Database-per-service pattern
- Multi-database sharding
- Health checks cho tất cả services
- Scripts build tự động

### Tính năng Đang Phát triển

🔄 **In Progress**

- Payment processing integration
- Push notifications (Firebase Cloud Messaging)
- Real-time trip tracking trên map
- Rating và review system nâng cao

📋 **Backlog**

- Admin dashboard
- Analytics và reporting
- Service mesh (Istio) implementation
- Kubernetes deployment
- CI/CD pipeline (GitHub Actions)
- Load testing và performance optimization

## Tài liệu Bổ sung

### Documentation

- **[Tổng quan Kiến trúc](docs/ARCHITECTURE.md)** - Chi tiết thiết kế hệ thống và components (Tiếng Việt)
- **[API Interfaces](docs/testing-guide/API_ENDPOINTS.md)** - Tài liệu API đầy đủ
- **[Redis & gRPC Commands](docs/testing-guide/redis-grpc-testing-commands.md)** - Testing utilities
- **[ADR-001: Redis cho Geospatial](docs/ADR/001-redis-vs-dynamodb-for-geospatial.md)** - Quyết định kiến trúc
- **[ADR-002: gRPC cho Location Updates](docs/ADR/002-grpc-vs-rest-for-location-updates.md)** - Communication protocol
- **[ADR-003: REST cho CRUD](docs/ADR/003-rest-vs-grpc-for-crud-operations.md)** - API design choices
- **[ADR-004: RabbitMQ cho Messaging](docs/ADR/004-rabbitmq-vs-kafka-for-async-messaging.md)** - Message broker selection

### Quick References

**Kiến trúc Pattern**: Database-per-service microservices với database sharding  
**Authentication**: JWT Bearer tokens  
**Inter-Service Communication**: REST (OpenFeign) + gRPC + RabbitMQ  
**Data Storage**: PostgreSQL (relational + sharding) + Redis (geospatial/caching)  
**Container Orchestration**: Docker Compose  
**API Gateway**: Spring Cloud Gateway (tất cả requests qua port 8080)

---

## Giấy phép

Dự án này được phát triển cho mục đích học tập tại Đại học Công nghệ Thông tin (UIT), ĐHQG TP.HCM.

## Liên hệ

Nếu có câu hỏi hoặc vấn đề, vui lòng tạo issue trên GitHub repository.

---

**Cập nhật lần cuối**: 29/11/2025  
**Phiên bản**: 1.0.0
