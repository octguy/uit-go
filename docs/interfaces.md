# UIT-Go System Interfaces

## API Interface Specifications

### 1. API Gateway Interface (Port 8080)

**REST Endpoints:**
```yaml
# User Management
POST   /api/users/register
POST   /api/users/login
GET    /api/users/profile
PUT    /api/users/profile

# Trip Management
POST   /api/trips
GET    /api/trips/{id}
PUT    /api/trips/{id}/cancel
GET    /api/trips/history

# Driver Management
POST   /api/drivers/register
PUT    /api/drivers/status
GET    /api/drivers/location
PUT    /api/drivers/location
```

**Gateway Configuration:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://user-service:8081
          predicates:
            - Path=/api/users/**
        - id: trip-service
          uri: http://trip-service:8082
          predicates:
            - Path=/api/trips/**
        - id: driver-service
          uri: http://driver-service:8083
          predicates:
            - Path=/api/drivers/**
```

---

### 2. User Service Interface (Port 8081)

**REST API:**
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @PostMapping("/register")
    ResponseEntity<UserResponse> registerUser(@RequestBody RegisterRequest request);
    
    @PostMapping("/login")
    ResponseEntity<LoginResponse> loginUser(@RequestBody LoginRequest request);
    
    @GetMapping("/profile/{userId}")
    ResponseEntity<UserProfile> getUserProfile(@PathVariable String userId);
    
    @PutMapping("/profile/{userId}")
    ResponseEntity<UserProfile> updateUserProfile(@PathVariable String userId, @RequestBody UpdateProfileRequest request);
}
```

**gRPC Service:**
```protobuf
service UserService {
  rpc GetUser(GetUserRequest) returns (UserResponse);
  rpc CreateUser(CreateUserRequest) returns (UserResponse);
  rpc UpdateUser(UpdateUserRequest) returns (UserResponse);
  rpc ValidateUser(ValidateUserRequest) returns (ValidationResponse);
}

message GetUserRequest {
  string user_id = 1;
}

message UserResponse {
  string user_id = 1;
  string email = 2;
  string name = 3;
  string user_type = 4; // PASSENGER, DRIVER
  string phone = 5;
  int64 created_at = 6;
}

message CreateUserRequest {
  string email = 1;
  string password = 2;
  string name = 3;
  string user_type = 4;
  string phone = 5;
}
```

**Database Interface:**
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    private UserType userType; // PASSENGER, DRIVER
    
    private String name;
    private String phone;
    private LocalDateTime createdAt;
}

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByUserType(UserType userType);
}
```

---

### 3. Trip Service Interface (Port 8082)

**REST API:**
```java
@RestController
@RequestMapping("/api/trips")
public class TripController {
    
    @PostMapping
    ResponseEntity<TripResponse> createTrip(@RequestBody CreateTripRequest request);
    
    @GetMapping("/{tripId}")
    ResponseEntity<TripResponse> getTripDetails(@PathVariable String tripId);
    
    @PutMapping("/{tripId}/cancel")
    ResponseEntity<TripResponse> cancelTrip(@PathVariable String tripId);
    
    @GetMapping("/user/{userId}")
    ResponseEntity<List<TripResponse>> getUserTrips(@PathVariable String userId);
}
```

**gRPC Service:**
```protobuf
service TripService {
  rpc CreateTrip(CreateTripRequest) returns (TripResponse);
  rpc GetTrip(GetTripRequest) returns (TripResponse);
  rpc UpdateTripStatus(UpdateTripStatusRequest) returns (TripResponse);
  rpc GetUserTrips(GetUserTripsRequest) returns (UserTripsResponse);
}

message CreateTripRequest {
  string passenger_id = 1;
  string origin = 2;
  string destination = 3;
}

message TripResponse {
  string trip_id = 1;
  string passenger_id = 2;
  string driver_id = 3;
  string origin = 4;
  string destination = 5;
  TripStatus status = 6;
  double fare = 7;
  int64 created_at = 8;
}

enum TripStatus {
  REQUESTED = 0;
  ACCEPTED = 1;
  ONGOING = 2;
  COMPLETED = 3;
  CANCELLED = 4;
}
```

**Database Interface:**
```java
@Entity
@Table(name = "trips")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long passengerId;
    
    private Long driverId;
    
    @Column(nullable = false)
    private String origin;
    
    @Column(nullable = false)
    private String destination;
    
    @Enumerated(EnumType.STRING)
    private TripStatus status;
    
    private BigDecimal fare;
    private LocalDateTime createdAt;
}

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByPassengerId(Long passengerId);
    List<Trip> findByDriverId(Long driverId);
    List<Trip> findByStatus(TripStatus status);
}
```

---

### 4. Driver Service Interface (Port 8083)

**REST API:**
```java
@RestController
@RequestMapping("/api/drivers")
public class DriverController {
    
    @PostMapping("/register")
    ResponseEntity<DriverResponse> registerDriver(@RequestBody DriverRegistrationRequest request);
    
    @PutMapping("/{driverId}/status")
    ResponseEntity<DriverResponse> updateDriverStatus(@PathVariable String driverId, @RequestBody StatusUpdateRequest request);
    
    @PutMapping("/{driverId}/location")
    ResponseEntity<Void> updateLocation(@PathVariable String driverId, @RequestBody LocationUpdateRequest request);
    
    @GetMapping("/nearby")
    ResponseEntity<List<DriverResponse>> getNearbyDrivers(@RequestParam double latitude, @RequestParam double longitude);
}
```

**gRPC Service:**
```protobuf
service DriverService {
  rpc GetDriver(GetDriverRequest) returns (DriverResponse);
  rpc UpdateDriverStatus(UpdateDriverStatusRequest) returns (DriverResponse);
  rpc UpdateDriverLocation(UpdateLocationRequest) returns (LocationResponse);
  rpc GetNearbyDrivers(GetNearbyDriversRequest) returns (NearbyDriversResponse);
}

message DriverResponse {
  string driver_id = 1;
  string user_id = 2;
  string license_number = 3;
  string vehicle_info = 4;
  DriverStatus status = 5;
  Location current_location = 6;
}

message Location {
  double latitude = 1;
  double longitude = 2;
}

enum DriverStatus {
  OFFLINE = 0;
  ONLINE = 1;
  BUSY = 2;
}
```

**Database Interface:**
```java
@Entity
@Table(name = "drivers")
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private Long userId;
    
    @Column(unique = true, nullable = false)
    private String licenseNumber;
    
    private String vehicleInfo;
    
    @Enumerated(EnumType.STRING)
    private DriverStatus status;
    
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime createdAt;
}

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByUserId(Long userId);
    List<Driver> findByStatus(DriverStatus status);
}
```

---

## 5. Shared Interfaces

### Message Queue Events (RabbitMQ)
```java
// Shared Event Classes
public class TripCreatedEvent {
    private String tripId;
    private String passengerId;
    private String origin;
    private String destination;
    private LocalDateTime timestamp;
}

public class TripAcceptedEvent {
    private String tripId;
    private String driverId;
    private LocalDateTime timestamp;
}

public class DriverLocationUpdatedEvent {
    private String driverId;
    private double latitude;
    private double longitude;
    private LocalDateTime timestamp;
}

// RabbitMQ Configuration
@Configuration
public class RabbitMQConfig {
    
    @Bean
    public Queue tripCreatedQueue() {
        return new Queue("trip.created", true);
    }
    
    @Bean
    public Queue tripAcceptedQueue() {
        return new Queue("trip.accepted", true);
    }
    
    @Bean
    public Queue driverLocationQueue() {
        return new Queue("driver.location.updated", true);
    }
    
    @Bean
    public TopicExchange tripExchange() {
        return new TopicExchange("trip.exchange");
    }
}
```

### Common DTOs
```java
// Request/Response DTOs
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
}

public class ErrorResponse {
    private String error;
    private String message;
    private int status;
    private LocalDateTime timestamp;
}

public class PaginationRequest {
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdAt";
    private String sortDir = "desc";
}

public class PaginationResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
```

### Common Exception Handling
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            "RESOURCE_NOT_FOUND", 
            ex.getMessage(), 
            404, 
            LocalDateTime.now()
        );
        return ResponseEntity.status(404).body(error);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        ErrorResponse error = new ErrorResponse(
            "VALIDATION_ERROR", 
            ex.getMessage(), 
            400, 
            LocalDateTime.now()
        );
        return ResponseEntity.status(400).body(error);
    }
}
```

### Security Configuration
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/users/register", "/api/users/login").permitAll()
                .requestMatchers("/api/trips/**").hasAnyRole("PASSENGER", "DRIVER")
                .requestMatchers("/api/drivers/**").hasRole("DRIVER")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
    }
}
```

This interface design provides:
- **Clear API contracts** for each service
- **gRPC definitions** for inter-service communication
- **Database entity interfaces** with JPA repositories
- **Shared message queue events** for asynchronous communication
- **Common DTOs and error handling** for consistency
- **Security configuration** for authentication and authorization

Each service maintains its independence while following consistent patterns for integration.