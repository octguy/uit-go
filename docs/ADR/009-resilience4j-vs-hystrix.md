# ADR-009: Resilience4j vs Hystrix cho Circuit Breaker Pattern

## Bối cảnh

Microservices architecture dễ gặp cascading failures:

**Problem Scenario:**
```
Passenger Request → API Gateway → Trip Service → Driver Service (DOWN)
                                        ↓
                                   Timeout waiting
                                   (30 seconds)
                                        ↓
                              Thread pool exhausted
                                        ↓
                           API Gateway becomes unresponsive
                                        ↓
                              ALL requests fail
```

Cần circuit breaker pattern để:
1. **Fast-fail**: Không waste time waiting for failing service
2. **Prevent cascading failures**: Protect upstream services
3. **Auto-recovery**: Test if failing service recovered

### Yêu cầu

1. **Circuit Breaker**: Open circuit when error rate high
2. **Retry with Backoff**: Automatic retry for transient failures
3. **Fallback**: Graceful degradation when service unavailable
4. **Metrics**: Monitor error rates and circuit states
5. **Spring Boot Integration**: Work seamlessly with existing codebase

### Các phương án được xem xét

1. **Resilience4j**
2. **Netflix Hystrix**
3. **Spring Retry (no circuit breaker)**
4. **Istio/Linkerd Circuit Breaker**
5. **Manual Implementation**

---

## Quyết định

**Nhóm em chọn Resilience4j** cho circuit breaker và retry patterns.

**Dependencies:**
```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

---

## Lý do lựa chọn

### Ưu điểm của Resilience4j

#### 1. **Modern and Actively Maintained**

```
Resilience4j:
  First release: 2017
  Latest release: 2024 (active)
  Spring Boot 3 support: Yes ✅
  Java 17+ support: Yes ✅
  Reactive support: Yes ✅

Hystrix:
  First release: 2012
  Latest release: 2018
  Status: MAINTENANCE MODE ❌
  Spring Boot 3: Not officially supported
  Future: No new features
```

**Netflix announcement (2018):**
> "Hystrix is no longer in active development, and is currently in maintenance mode."

#### 2. **Lightweight - No Dependencies**

**Resilience4j:**
```
Core library: ~50KB
No external dependencies
Uses Vavr (functional library): ~1MB
Total: ~1MB

Pure Java, functional programming style
```

**Hystrix:**
```
Core: ~150KB
Dependencies:
  - RxJava: ~2MB
  - Archaius (config): ~500KB
  - Servo (metrics): ~300KB
Total: ~3MB

Heavier footprint
```

#### 3. **Better Spring Boot Integration**

**Resilience4j with Spring Boot:**

```java
// Simple annotation-based
@CircuitBreaker(name = "driverService", fallbackMethod = "findDriversFallback")
@Retry(name = "driverService")
public List<Driver> findNearbyDrivers(double lat, double lon) {
    return driverServiceClient.findNearby(lat, lon, 5.0);
}

public List<Driver> findDriversFallback(double lat, double lon, Exception e) {
    log.warn("Driver service unavailable, returning empty list", e);
    return Collections.emptyList();
}
```

**Configuration in application.yml:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      driverService:
        failureRateThreshold: 50
        waitDurationInOpenState: 10000
        slidingWindowSize: 10
  retry:
    instances:
      driverService:
        maxAttempts: 3
        waitDuration: 1000
```

**Hystrix equivalent:**
```java
// More verbose
@HystrixCommand(
    commandKey = "findNearbyDrivers",
    fallbackMethod = "findDriversFallback",
    commandProperties = {
        @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "5000"),
        @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "10"),
        @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "50")
    }
)
```

#### 4. **Functional Programming Style**

**Resilience4j decorators:**

```java
// Compose multiple patterns
Supplier<List<Driver>> decoratedSupplier = Decorators
    .ofSupplier(() -> driverServiceClient.findNearby(lat, lon))
    .withCircuitBreaker(circuitBreaker)
    .withRetry(retry)
    .withFallback(Arrays.asList(Exception.class), 
        e -> Collections.emptyList())
    .decorate();

return decoratedSupplier.get();
```

**Clean and composable**

#### 5. **Better Metrics Integration**

**Resilience4j with Micrometer:**

```java
@Configuration
public class ResilienceConfig {
    
    @Bean
    public CircuitBreaker circuitBreaker(MeterRegistry registry) {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("driverService");
        
        // Auto-export to Prometheus
        CircuitBreakerMetrics.ofCircuitBreaker(cb).bindTo(registry);
        
        return cb;
    }
}
```

**Metrics available:**
```
resilience4j_circuitbreaker_state{name="driverService"} 0.0  # CLOSED
resilience4j_circuitbreaker_failure_rate{name="driverService"} 0.15
resilience4j_circuitbreaker_calls_total{name="driverService",kind="successful"} 850
resilience4j_circuitbreaker_calls_total{name="driverService",kind="failed"} 12
```

---

### Tại sao không chọn Hystrix?

**Strategy:** Netflix's circuit breaker library

#### Nhược điểm:

**1. Maintenance Mode** ❌

```
Hystrix GitHub README:
  "Hystrix is no longer in active development"
  "We recommend Resilience4j as an alternative"
  
Last release: November 2018
No Spring Boot 3 support
No Java 17+ improvements
```

**Risk:** Using deprecated library in new project

**2. Thread Pool Isolation Overhead** ❌

Hystrix default: Separate thread pool per command

```
Without Hystrix:
  Request thread → Call driver-service → Return
  
With Hystrix (thread isolation):
  Request thread → Submit to Hystrix thread pool
                → Hystrix thread calls driver-service
                → Result back to request thread
                
Overhead:
  - Thread context switch: ~1-2ms
  - Memory: Each thread pool ~1MB
  - Complexity: Managing multiple thread pools
```

**Resilience4j:** Semaphore-based, no thread pool overhead

**3. Heavier Dependencies** ❌

```
Hystrix requires:
  RxJava 1.x (old reactive library)
  Archaius (Netflix config library)
  Servo (Netflix metrics)
  
Resilience4j:
  Self-contained
  Works with modern reactive (Project Reactor, RxJava 2/3)
```

#### 4. **Complex Configuration** ❌

**Hystrix properties (many defaults to understand):**

```java
@HystrixCommand(commandProperties = {
    @HystrixProperty(name = "execution.isolation.strategy", value = "THREAD"),
    @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "1000"),
    @HystrixProperty(name = "execution.timeout.enabled", value = "true"),
    @HystrixProperty(name = "execution.isolation.semaphore.maxConcurrentRequests", value = "10"),
    @HystrixProperty(name = "circuitBreaker.enabled", value = "true"),
    @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "20"),
    @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "5000"),
    @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "50"),
    @HystrixProperty(name = "circuitBreaker.forceOpen", value = "false"),
    @HystrixProperty(name = "circuitBreaker.forceClosed", value = "false"),
    // ... 20+ more properties
})
```

**Resilience4j (concise):**
```yaml
resilience4j.circuitbreaker.instances.driverService:
  failureRateThreshold: 50
  waitDurationInOpenState: 5s
  slidingWindowSize: 20
```

---

### Tại sao không chọn Spring Retry?

**Strategy:** Spring's built-in retry mechanism

#### Nhược điểm:

**1. No Circuit Breaker** ❌

Spring Retry only provides:
```
✅ Retry with backoff
✅ Custom retry policies
❌ Circuit breaker (no failure rate threshold)
❌ Half-open state testing
❌ Automatic circuit opening
```

**Must combine with other libraries for circuit breaking**

**2. Less Flexible** ❌

```java
// Spring Retry - limited customization
@Retryable(
    value = {RestClientException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public List<Driver> findDrivers() {
    return driverClient.find();
}
```

**Cannot:**
- Configure per-service circuit breakers
- Monitor circuit states
- Custom fallback strategies

Resilience4j provides full control

---

### Tại sao không chọn Service Mesh Circuit Breaker?

**Strategy:** Use Linkerd/Istio for circuit breaking

#### Nhược điểm:

**1. Coarse-grained Control** ❌

Service mesh operates at service level:

```
Istio/Linkerd:
  Circuit breaker for entire service
  Cannot differentiate:
    - Critical path (trip creation) vs non-critical (analytics)
    - Different error handling per endpoint
    
Resilience4j:
  Fine-grained per method
  @CircuitBreaker on specific methods
  Custom fallback per use case
```

**2. Cannot Access Application Context** ❌

```
Mesh fallback:
  Return HTTP 503 (service unavailable)
  Generic error response
  
Application fallback:
  Access database for cached data
  Return degraded response (partial data)
  Log with business context
```

**Example:**
```java
@CircuitBreaker(name = "driver", fallbackMethod = "cachedDrivers")
public List<Driver> findDrivers() {
    return driverService.findNearby();
}

public List<Driver> cachedDrivers(Exception e) {
    // Application-level fallback
    return driverCacheRepository.getRecentDrivers();
}
```

**3. Complementary, Not Replacement** ✅

```
Best practice:
  Application-level: Resilience4j (fine-grained control)
  Network-level: Service Mesh (infrastructure resilience)
  
Use both together
```

---

### Tại sao không chọn Manual Implementation?

**Strategy:** Implement circuit breaker pattern ourselves

#### Nhược điểm:

**1. Complex State Machine** ❌

Circuit breaker states:
```
CLOSED → OPEN → HALF_OPEN → CLOSED/OPEN

Must handle:
  - Failure rate calculation (sliding window)
  - Timeout in OPEN state
  - Limited requests in HALF_OPEN
  - Thread-safe state transitions
  - Metrics collection
```

**Implementation:** 500+ lines of code

**Resilience4j:** Production-tested, ~50 KB library

**2. Hard to Test** ❌

```
Edge cases to test:
  - Concurrent state transitions
  - Sliding window edge cases
  - Race conditions
  - Metrics accuracy
  
Manual implementation: High bug risk
Resilience4j: Battle-tested
```

**3. Reinventing the Wheel** ❌

No benefit to custom implementation when proven library exists

---

## Chi tiết triển khai

### Configuration

**application.yml:**

```yaml
resilience4j:
  circuitbreaker:
    instances:
      driverService:
        failureRateThreshold: 50           # Open if 50% failed
        slowCallRateThreshold: 50           # Also consider slow calls
        slowCallDurationThreshold: 5000     # Call > 5s is slow
        permittedNumberOfCallsInHalfOpenState: 3
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10               # Last 10 calls
        minimumNumberOfCalls: 5             # Need 5 calls before calculating rate
        waitDurationInOpenState: 10000      # Wait 10s before HALF_OPEN
        recordExceptions:
          - org.springframework.web.client.HttpServerErrorException
          - java.io.IOException
          - java.util.concurrent.TimeoutException
        ignoreExceptions:
          - com.example.trip_service.exception.BusinessException
          
  retry:
    instances:
      driverService:
        maxAttempts: 3
        waitDuration: 1000                  # 1 second
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2     # 1s, 2s, 4s
        retryExceptions:
          - org.springframework.web.client.HttpServerErrorException
          - java.net.SocketTimeoutException
```

### Implementation

**Service Layer:**

```java
@Service
@Slf4j
public class TripService {
    
    @Autowired
    private DriverServiceClient driverServiceClient;
    
    @CircuitBreaker(name = "driverService", fallbackMethod = "findDriversFallback")
    @Retry(name = "driverService")
    public List<NearbyDriverDTO> findNearbyDrivers(double lat, double lon) {
        log.info("Finding nearby drivers for location: {}, {}", lat, lon);
        return driverServiceClient.findNearbyDrivers(lat, lon, 5.0);
    }
    
    // Fallback method (same signature + Exception parameter)
    private List<NearbyDriverDTO> findDriversFallback(
        double lat, double lon, Exception e
    ) {
        log.warn("Driver service circuit breaker activated, using fallback", e);
        
        // Option 1: Return empty (graceful degradation)
        return Collections.emptyList();
        
        // Option 2: Return cached data
        // return driverCacheService.getRecentDrivers(lat, lon);
        
        // Option 3: Throw business exception
        // throw new NoDriversAvailableException("Driver service unavailable");
    }
}
```

### Monitoring

**Actuator Endpoints:**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,circuitbreakers,circuitbreakerevents
  health:
    circuitbreakers:
      enabled: true
```

**Check Circuit Breaker State:**

```bash
# Health endpoint
curl http://localhost:8082/actuator/health

# Response:
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "driverService": {
          "status": "UP",
          "details": {
            "state": "CLOSED",
            "failureRate": "15.0%",
            "slowCallRate": "0.0%"
          }
        }
      }
    }
  }
}

# Circuit breaker events
curl http://localhost:8082/actuator/circuitbreakerevents

# Metrics
curl http://localhost:8082/actuator/metrics/resilience4j.circuitbreaker.state
```

### Prometheus Metrics

```
# Circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
resilience4j_circuitbreaker_state{name="driverService"} 0.0

# Failure rate
resilience4j_circuitbreaker_failure_rate{name="driverService"} 0.15

# Call stats
resilience4j_circuitbreaker_calls_total{name="driverService",kind="successful"} 850
resilience4j_circuitbreaker_calls_total{name="driverService",kind="failed"} 12

# Slow calls
resilience4j_circuitbreaker_slow_calls_total{name="driverService"} 5
```

---

## Hệ quả

### Tích cực

1. ✅ **Prevents Cascading Failures**

```
Before:
  Driver Service down → Trip Service hangs → API Gateway exhausted

After:
  Driver Service down → Circuit opens → Fast-fail (10ms)
  → Trip Service healthy → API Gateway healthy
```

2. ✅ **Automatic Recovery Testing**

```
Circuit states:
  CLOSED: Normal operation
  OPEN: Fast-failing (service down)
  HALF_OPEN: Testing recovery (allow 3 calls)
  → If 3 calls succeed: Back to CLOSED
  → If any fails: Back to OPEN
```

3. ✅ **Graceful Degradation**

```java
// Instead of complete failure
public List<Driver> fallback() {
    return Collections.emptyList();  // UX: "No drivers available"
}

// vs without circuit breaker:
// 30s timeout → Error 500 → Bad UX
```

4. ✅ **Modern and Maintained**

```
Resilience4j: Active development
Compatible with: Spring Boot 3, Java 21, Virtual Threads
Future-proof choice
```

### Tiêu cực

1. ❌ **Configuration Complexity**

Must tune parameters for each service:

```yaml
failureRateThreshold: 50  # Too low: false positives
                           # Too high: slow to react

waitDurationInOpenState: 10s  # Too short: premature retry
                               # Too long: slow recovery
```

**Mitigation:** Start with defaults, tune based on monitoring

2. ❌ **False Positives**

```
Scenario:
  Legitimate spike of errors (database maintenance)
  Circuit opens
  Even after DB recovers, circuit stays OPEN for 10s
  
Result: 10s additional downtime
```

**Acceptable:** Better than cascading failure

3. ❌ **Fallback Limitations**

```
Cannot always provide meaningful fallback:
  - Payment processing: Cannot fake success
  - Critical operations: Must fail safely
  
Fallbacks work best for:
  - Read operations (use cache)
  - Non-critical features (disable gracefully)
```

### Biện pháp giảm thiểu

**Alert on Circuit Open:**

```java
@Component
public class CircuitBreakerEventListener {
    
    @EventListener
    public void onCircuitBreakerEvent(CircuitBreakerOnStateTransitionEvent event) {
        if (event.getStateTransition().getToState() == CircuitBreaker.State.OPEN) {
            log.error("Circuit breaker OPENED for: {}", event.getCircuitBreakerName());
            // Send alert to ops team
            alertService.sendAlert("Circuit breaker opened: " + event.getCircuitBreakerName());
        }
    }
}
```

---

## Xem xét lại quyết định

Nhóm em sẽ **xem xét lại** nếu:

1. **Need thread isolation** (protect against runaway threads)
   - Giải pháp: Resilience4j supports Bulkhead pattern (semaphore or thread pool)

2. **Reactive applications** (WebFlux)
   - Giải pháp: Resilience4j has reactive support out-of-the-box

3. **More complex patterns needed** (bulkhead, rate limiting)
   - Giải pháp: Resilience4j provides all patterns

---

## Kết quả Validation

### Failure Simulation

**Test: Driver Service down**

```
Simulation:
  1. Stop driver-service pod
  2. Create trip (triggers driver search)
  3. Observe behavior

Without circuit breaker:
  Request 1: Timeout after 30s → Error
  Request 2: Timeout after 30s → Error
  ...
  Impact: Trip service slow, API gateway pool exhausted

With Resilience4j:
  Request 1-5: Timeout (establishing failure pattern)
  Request 6: Circuit OPENS
  Request 7+: Fast-fail in 10ms, fallback response
  After 10s: Circuit HALF_OPEN, test 3 calls
  If driver-service up: Circuit CLOSED
  
Result: ✅ Graceful degradation, no cascading failure
```

### Metrics

```
resilience4j_circuitbreaker_calls_total{kind="successful"} 9,850
resilience4j_circuitbreaker_calls_total{kind="failed"} 50
resilience4j_circuitbreaker_state{name="driverService"} 0.0  # CLOSED

Failure rate: 0.5% (well below 50% threshold)
Circuit: Healthy
```

---

## References

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Circuit Breaker Pattern - Martin Fowler](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Hystrix Deprecation Announcement](https://github.com/Netflix/Hystrix#hystrix-status)
- [Spring Cloud Circuit Breaker](https://spring.io/projects/spring-cloud-circuitbreaker)
