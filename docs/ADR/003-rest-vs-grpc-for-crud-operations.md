# ADR-003: Choose REST over gRPC for CRUD Operations and User-Facing APIs

**Status**: Accepted  
**Date**: 2025-11-25  
**Decision Makers**: UIT-Go Development Team  
**Tags**: #communication #api-design #rest #simplicity

---

## Context

While we chose gRPC for high-frequency, real-time operations (driver location updates), we need to decide on the communication protocol for:

1. **User Management**: Registration, login, profile updates
2. **Trip Management**: Create trip, get trip details, cancel trip, trip history
3. **Driver Management**: Register driver, update status, get driver profile
4. **Admin Operations**: CRUD operations for system management
5. **Client-Facing APIs**: Mobile app and web dashboard

These operations are characterized by:

- **Lower Frequency**: Occasional requests (not continuous streams)
- **Simple Request/Response Pattern**: One request → one response
- **Human-Readable Data**: Beneficial for debugging and monitoring
- **Wide Client Support**: Mobile apps, web browsers, third-party integrations
- **Varying Payload Sizes**: From small (login) to medium (trip history)

### Scale & Performance Requirements

```
User Operations:
  - Login frequency: ~10 req/sec (peak: 50 req/sec)
  - Registration: ~2 req/sec (peak: 10 req/sec)
  - Profile updates: ~5 req/sec (peak: 20 req/sec)
  - Response time requirement: < 500ms

Trip Operations:
  - Create trip: ~20 req/sec (peak: 100 req/sec)
  - Get trip details: ~30 req/sec (peak: 150 req/sec)
  - Trip history: ~5 req/sec (peak: 25 req/sec)
  - Response time requirement: < 500ms

Total CRUD load: ~100 req/sec (peak: ~400 req/sec)
```

### Options Considered

1. **REST API with JSON**
2. **gRPC with Protocol Buffers**
3. **GraphQL**
4. **SOAP**
5. **Hybrid Approach (REST for external, gRPC for internal)**

---

## Decision

**We chose REST API with JSON** for all CRUD operations, user management, trip management, and client-facing APIs.

**We use gRPC only for**: High-frequency real-time operations (driver location streaming).

---

## Rationale

### REST Advantages for CRUD Operations

#### 1. **Simplicity and Developer Experience**

**REST Example** (User Login):

```java
// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        String token = authService.authenticate(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new LoginResponse(token));
    }
}

// Request
POST /api/users/login HTTP/1.1
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass123"
}

// Response
HTTP/1.1 200 OK
Content-Type: application/json

{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 86400
}
```

**gRPC Example** (Same operation):

```protobuf
// Define .proto file
service UserService {
  rpc Login(LoginRequest) returns (LoginResponse);
}

message LoginRequest {
  string email = 1;
  string password = 2;
}

message LoginResponse {
  string token = 1;
  int32 expiresIn = 2;
}

// Compile proto file
protoc --java_out=... --grpc-java_out=... user.proto

// Implement service
@GrpcService
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {
    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        String token = authService.authenticate(request.getEmail(), request.getPassword());
        LoginResponse response = LoginResponse.newBuilder()
            .setToken(token)
            .setExpiresIn(86400)
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

**Winner: REST** - Less boilerplate, more straightforward for simple operations.

#### 2. **Browser and Third-Party Integration**

**REST**:

```javascript
// Web browser (JavaScript)
fetch('https://api.uitgo.com/api/users/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email: 'john@example.com', password: 'Pass123' })
})
  .then(res => res.json())
  .then(data => console.log(data.token));

// cURL (command line)
curl -X POST https://api.uitgo.com/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"Pass123"}'

// Postman (API testing)
# No special setup needed, just add URL and JSON body
```

**gRPC**:

```javascript
// Web browser requires gRPC-Web + Envoy proxy
import {UserServiceClient} from './generated/user_grpc_web_pb';
import {LoginRequest} from './generated/user_pb';

const client = new UserServiceClient('https://api.uitgo.com:9090');
const request = new LoginRequest();
request.setEmail('john@example.com');
request.setPassword('Pass123');

client.login(request, {}, (err, response) => {
  if (err) console.error(err);
  else console.log(response.getToken());
});

// cURL doesn't support gRPC directly, need grpcurl
grpcurl -plaintext -d '{"email":"john@example.com","password":"Pass123"}' \
  api.uitgo.com:9090 user.UserService/Login

// Postman supports gRPC but requires .proto files import
```

**Winner: REST** - Universal support, no special tooling needed.

#### 3. **Human-Readable Debugging**

**REST Debugging**:

```bash
# View request/response in browser DevTools Network tab
Request:
  POST /api/trips/request
  {
    "passengerId": "abc123",
    "pickupLocation": "University Campus",
    "destination": "Downtown Mall"
  }

Response:
  {
    "id": "def456",
    "status": "REQUESTED",
    "fare": null,
    "createdAt": "2025-11-25T10:30:00Z"
  }

# Easily readable and debuggable
# Can copy-paste JSON for testing
# Clear understanding of data structure
```

**gRPC Debugging**:

```bash
# Binary protocol, requires special tools

# Option 1: grpcurl (command line)
grpcurl -d '{"passengerId":"abc123"}' localhost:9090 trip.TripService/CreateTrip

# Option 2: gRPC reflection + GUI tools (e.g., Postman, BloomRPC)

# Option 3: Logging interceptor
@GrpcService
public class TripGrpcService extends TripServiceGrpc.TripServiceImplBase {
    @Override
    public void createTrip(CreateTripRequest request, StreamObserver<TripResponse> responseObserver) {
        // Must add custom logging to see request/response
        logger.info("Request: {}", request);
        // ...
    }
}
```

**Winner: REST** - Immediate visibility in browser/tools, no special setup.

#### 4. **Caching Support**

**REST with HTTP Caching**:

```http
# Request
GET /api/trips/abc123 HTTP/1.1
If-None-Match: "v1.0"

# Response (not modified)
HTTP/1.1 304 Not Modified
ETag: "v1.0"

# Response (modified)
HTTP/1.1 200 OK
ETag: "v1.1"
Cache-Control: max-age=60
```

Benefits:

- **Browser Caching**: Automatic caching by browsers
- **CDN Support**: Can use CloudFlare, AWS CloudFront
- **Conditional Requests**: ETag, Last-Modified headers
- **Cache Control**: Fine-grained cache policies

**gRPC**:

- No native caching support
- Must implement custom caching layer
- Binary protocol complicates CDN integration

**Winner: REST** - Native HTTP caching support.

#### 5. **API Gateway Compatibility**

**REST**:

```yaml
# Spring Cloud Gateway (our current setup)
routes:
  - id: user-service
    uri: http://user-service:8081
    predicates:
      - Path=/api/users/**
    filters:
      - RewritePath=/api/users/(?<segment>.*), /$\{segment}

  # Easy integration with:
  - Rate limiting
  - Authentication
  - Request transformation
  - Response caching
  - Load balancing
```

**gRPC**:

```yaml
# Requires Envoy proxy or gRPC-specific gateway
# More complex configuration
# Limited support in Spring Cloud Gateway
# Need additional infrastructure
```

**Winner: REST** - Better integration with API Gateway.

#### 6. **Performance is Adequate**

**Benchmark Results** (CRUD operations):

| Operation               | REST (JSON) | gRPC (Protobuf) | Difference         |
| ----------------------- | ----------- | --------------- | ------------------ |
| User Login              | 45ms        | 35ms            | -22% (gRPC faster) |
| Create Trip             | 65ms        | 52ms            | -20% (gRPC faster) |
| Get Trip Details        | 28ms        | 22ms            | -21% (gRPC faster) |
| Trip History (10 trips) | 85ms        | 68ms            | -20% (gRPC faster) |

**Analysis**:

- gRPC is 20-25% faster
- **BUT**: Both meet < 500ms SLA requirement
- Absolute difference: 10-20ms (negligible for user experience)
- **Trade-off**: 20% performance gain vs development complexity

**Decision**: 20% performance improvement doesn't justify additional complexity for CRUD operations.

---

### When gRPC Doesn't Add Value

#### 1. **Low Frequency Operations**

```
Login frequency: 10 req/sec (vs 2,000 location updates/sec for gRPC)

Benefits of gRPC diminish at low frequency:
- Persistent connection overhead not amortized
- Binary serialization savings minimal (45ms → 35ms)
- Streaming not needed (single request/response)
```

#### 2. **Variable Payload Sizes**

```
User login: ~100 bytes (small)
Trip history: ~5 KB (medium)
Trip details: ~500 bytes (small)

gRPC Advantages:
  Small payloads: 20% size reduction (100 bytes → 80 bytes) = 20 bytes saved
  Medium payloads: 15% size reduction (5 KB → 4.25 KB) = 750 bytes saved

Impact on 100 requests:
  REST: 100 × 100 bytes = 10 KB total
  gRPC: 100 × 80 bytes = 8 KB total
  Savings: 2 KB (negligible for modern networks)
```

#### 3. **Schema Evolution**

**REST (JSON)**:

```json
// Version 1
{
  "id": "abc123",
  "status": "REQUESTED"
}

// Version 2 (add field, backward compatible)
{
  "id": "abc123",
  "status": "REQUESTED",
  "estimatedFare": 150000  // New field
}

// Clients ignore unknown fields automatically
// No code regeneration needed
```

**gRPC (Protobuf)**:

```protobuf
// Version 1
message TripResponse {
  string id = 1;
  string status = 2;
}

// Version 2 (add field)
message TripResponse {
  string id = 1;
  string status = 2;
  double estimatedFare = 3;  // New field
}

// Must regenerate code from .proto
mvn clean compile
// Clients must update generated code
```

**Winner: REST** - Easier schema evolution, no code regeneration.

---

### Why Not GraphQL?

GraphQL was considered for user-facing APIs but rejected:

#### 1. **Overkill for Simple CRUD**

```graphql
# GraphQL query (complex)
query {
  trip(id: "abc123") {
    id
    status
    passenger {
      id
      name
      phone
    }
    driver {
      id
      name
      vehicle {
        model
        number
      }
    }
    fare
  }
}

# REST (simple)
GET /api/trips/abc123

# For our use case, we don't need:
- Flexible field selection
- Multiple resource fetching in single request
- Client-driven queries
```

#### 2. **Learning Curve & Complexity**

- Requires GraphQL schema definition
- Custom resolvers for each field
- N+1 query problem requires DataLoader
- Caching is more complex

#### 3. **Performance Overhead**

```
REST: Direct database query
GraphQL:
  1. Parse query
  2. Validate against schema
  3. Execute resolvers
  4. Merge results
Total overhead: 10-30ms
```

**Decision**: GraphQL deferred to future if complex client requirements emerge.

---

## Implementation Details

### REST API Design Standards

#### 1. **Resource-Based URIs**

```
Users:
  POST   /api/users/register         - Create user
  POST   /api/users/login            - Authenticate
  GET    /api/users/profile          - Get profile (authenticated)
  PUT    /api/users/profile          - Update profile

Trips:
  POST   /api/trips/request          - Create trip
  GET    /api/trips/{id}             - Get trip details
  PUT    /api/trips/{id}/cancel      - Cancel trip
  GET    /api/trips/history          - Get trip history

Drivers:
  POST   /api/drivers/register       - Register driver
  PUT    /api/drivers/status         - Update status
  GET    /api/drivers/{id}           - Get driver profile
```

#### 2. **HTTP Methods & Status Codes**

```
POST   - Create resource (201 Created)
GET    - Retrieve resource (200 OK, 404 Not Found)
PUT    - Update resource (200 OK, 404 Not Found)
DELETE - Delete resource (204 No Content, 404 Not Found)
PATCH  - Partial update (200 OK)

Error Codes:
  400 Bad Request - Invalid input
  401 Unauthorized - Missing/invalid authentication
  403 Forbidden - Insufficient permissions
  404 Not Found - Resource doesn't exist
  409 Conflict - Duplicate resource
  500 Internal Server Error - Server error
```

#### 3. **Consistent Response Format**

```json
// Success Response
{
  "data": {
    "id": "abc123",
    "status": "REQUESTED"
  }
}

// Error Response
{
  "error": {
    "code": "INVALID_INPUT",
    "message": "Email address is required",
    "field": "email"
  }
}

// Paginated Response
{
  "data": [...],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalPages": 5,
    "totalItems": 97
  }
}
```

#### 4. **API Versioning**

```
# URL versioning (simple, explicit)
/api/v1/users/login
/api/v2/users/login

# Header versioning (alternative)
GET /api/users/login
Accept: application/vnd.uitgo.v1+json
```

### Spring Boot Implementation

```java
@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    @PostMapping("/request")
    public ResponseEntity<TripResponse> createTrip(
        @Valid @RequestBody CreateTripRequest request,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        TripResponse trip = tripService.createTrip(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(trip);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> getTrip(@PathVariable UUID id) {
        return tripService.getTripById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<TripResponse> cancelTrip(
        @PathVariable UUID id,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        TripResponse trip = tripService.cancelTrip(id, user.getUserId());
        return ResponseEntity.ok(trip);
    }

    @GetMapping("/history")
    public ResponseEntity<List<TripResponse>> getTripHistory(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        List<TripResponse> trips = tripService.getTripHistory(
            user.getUserId(), page, size
        );
        return ResponseEntity.ok(trips);
    }
}
```

### OpenFeign for Inter-Service Communication

```java
@FeignClient(name = "user-service", url = "http://user-service:8081")
public interface UserClient {

    @GetMapping("/api/internal/auth/validate")
    UserValidationResponse validate(@RequestHeader("Authorization") String token);

    @GetMapping("/api/internal/users/{id}")
    UserResponse getUserById(@PathVariable UUID id);
}

@FeignClient(name = "driver-service", url = "http://driver-service:8083")
public interface DriverClient {

    @GetMapping("/api/internal/drivers/nearby")
    List<NearbyDriverResponse> getNearbyDrivers(
        @RequestParam double lat,
        @RequestParam double lng,
        @RequestParam(defaultValue = "3.0") double radiusKm,
        @RequestParam(defaultValue = "5") int limit
    );
}
```

---

## Consequences

### Positive

1. ✅ **Simple Development**: Easy to implement and maintain
2. ✅ **Universal Support**: Works in browsers, mobile apps, curl, Postman
3. ✅ **Easy Debugging**: Human-readable JSON in Network tab
4. ✅ **API Gateway Integration**: Works seamlessly with Spring Cloud Gateway
5. ✅ **Caching Support**: Native HTTP caching
6. ✅ **Third-Party Integration**: Easy for partners to integrate
7. ✅ **Team Familiarity**: Team already knows REST/JSON
8. ✅ **Tooling**: Rich ecosystem (Swagger, Postman, curl)

### Negative

1. ❌ **20% Slower than gRPC**: 45ms vs 35ms (still within SLA)
2. ❌ **Larger Payloads**: JSON vs Protobuf (not significant for CRUD)
3. ❌ **No Type Safety**: Runtime errors vs compile-time errors
4. ❌ **Manual Serialization**: No auto-generated code

### Mitigations

**Performance**:

- Adequate for CRUD operations (< 500ms SLA)
- Optimize database queries, add caching if needed
- Use HTTP/2 for header compression

**Type Safety**:

- Use `@Valid` annotation for input validation
- Define DTOs with clear documentation
- Add unit tests for serialization/deserialization

**Large Payloads**:

- Use gzip compression (automatic in Spring Boot)
- Paginate large responses
- Consider GraphQL if clients need field selection

---

## Hybrid Architecture Decision

### REST for:

- ✅ User management (register, login, profile)
- ✅ Trip management (CRUD operations)
- ✅ Driver management (CRUD operations)
- ✅ Admin operations
- ✅ Client-facing APIs (mobile, web)
- ✅ Third-party integrations

### gRPC for:

- ✅ Driver location updates (high-frequency streaming)
- ✅ Future real-time features (e.g., live trip tracking)
- ✅ Internal service-to-service calls (future, if needed)

### Decision Matrix:

| Criteria          | REST         | gRPC          |
| ----------------- | ------------ | ------------- |
| Request frequency | < 100/sec    | > 1000/sec    |
| Streaming needed  | No           | Yes           |
| Browser support   | Required     | Not required  |
| Human-readable    | Preferred    | Not important |
| Payload size      | < 10 KB      | Any           |
| Type safety       | Nice to have | Critical      |

---

## Future Considerations

### When to Add gRPC for CRUD

Consider migrating CRUD operations to gRPC if:

1. **Scale Increases 10x**

   - Current: ~100 req/sec
   - Threshold: > 1,000 req/sec
   - Reason: Performance gains justify complexity

2. **Mobile Data Costs Become Issue**

   - Current: Unlimited data plans common
   - Threshold: Significant user base on limited data
   - Reason: Smaller payloads save mobile data

3. **Type Safety Becomes Critical**
   - Current: Manageable with validation
   - Threshold: Frequent schema changes cause bugs
   - Reason: Compile-time safety prevents runtime errors

### When to Add GraphQL

Consider GraphQL if:

1. **Complex Client Requirements**

   - Clients need flexible field selection
   - Multiple mobile app versions with different needs
   - Over-fetching becomes a problem

2. **Third-Party Developer API**
   - Public API for external developers
   - Need to support various use cases

---

## References

- [REST API Design Best Practices](https://restfulapi.net/)
- [Spring Boot REST Documentation](https://spring.io/guides/gs/rest-service/)
- [OpenFeign Documentation](https://spring.io/projects/spring-cloud-openfeign)
- [HTTP Status Codes](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status)
- [RESTful API Versioning](https://restfulapi.net/versioning/)
- [JSON vs Protobuf Performance](https://auth0.com/blog/beating-json-performance-with-protobuf/)

---

## Appendix: Performance Benchmark Details

### Test Environment

- **Instance**: AWS c5.2xlarge (8 vCPU, 16 GB RAM)
- **Load**: 100 concurrent users
- **Duration**: 10 minutes
- **Network**: Same VPC (low latency)

### Benchmark Results

| Operation    | REST P50 | REST P99 | gRPC P50 | gRPC P99 |
| ------------ | -------- | -------- | -------- | -------- |
| Login        | 42ms     | 85ms     | 32ms     | 68ms     |
| Register     | 78ms     | 145ms    | 62ms     | 125ms    |
| Get Profile  | 25ms     | 52ms     | 18ms     | 42ms     |
| Create Trip  | 62ms     | 128ms    | 48ms     | 105ms    |
| Get Trip     | 28ms     | 58ms     | 22ms     | 48ms     |
| Cancel Trip  | 45ms     | 92ms     | 35ms     | 75ms     |
| Trip History | 82ms     | 165ms    | 65ms     | 142ms    |

**Conclusion**: gRPC is consistently 20-25% faster, but both meet < 500ms SLA.

---

**Last Updated**: November 25, 2025  
**Review Date**: March 1, 2026
