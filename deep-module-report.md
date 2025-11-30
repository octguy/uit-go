# PHÂN TÍCH MODULE CHUYÊN SÂU
## Module A: Thiết kế Kiến trúc cho Scalability & Performance


## 2.1. Module Database Sharding

### 2.1.1. Tổng quan

**Vấn đề:** Hệ thống đặt xe hoạt động đa quốc gia (Việt Nam, Thái Lan) với dữ liệu chuyến đi tăng trưởng nhanh. Cần phân tán dữ liệu để tránh bottleneck và đảm bảo độ trễ thấp cho queries theo khu vực địa lý.

**Giải pháp:** Geographic Sharding - phân mảnh database theo khu vực địa lý.

> **Chi tiết kỹ thuật:** [ADR-005: Geographic Sharding vs Hash-based Sharding](docs/ADR/005-geographic-sharding-vs-hash-sharding.md)

### 2.1.2. Kiến trúc

```
┌─────────────────────────────────────────┐
│         Trip Service                    │
│  (Application-level Routing Logic)      │
└────────┬────────────────────┬───────────┘
         │                    │
    ┌────▼──────┐        ┌───▼─────────┐
    │ VN Shard  │        │  TH Shard   │
    │ Port 5433 │        │  Port 5434  │
    │ Longitude │        │  Longitude  │
    │ < 105.0   │        │  >= 105.0   │
    └───────────┘        └─────────────┘
```

### 2.1.3. Quyết định thiết kế chính

**1. Shard Key: Longitude (Kinh độ)**
- **Threshold:** 105.0°E (biên giới VN-TH)
- **Lý do:** 
  - Stable (không thay đổi sau khi chuyến đi được tạo)
  - Evenly distributed (phân bổ đều traffic)
  - Query-aligned (80% queries filter theo location)
  - Locality (tránh cross-shard queries)

**2. Application-level Routing**
- Sử dụng `AbstractRoutingDataSource` của Spring
- `ThreadLocal` context holder để routing per request
- Automatic connection pooling per shard (HikariCP)

**3. Kubernetes Deployment**
- Mỗi shard = 1 PostgreSQL pod riêng biệt
- PersistentVolumeClaim cho data durability
- Independent scaling cho từng shard

### 2.1.4. Trade-offs

| Ưu điểm | Nhược điểm | Giải pháp |
|---------|------------|-----------|
| ✅ Horizontal scalability | ❌ Cross-shard queries không thể | Denormalize data hoặc application-level JOIN |
| ✅ Data isolation theo khu vực | ❌ Rebalancing khó khi thêm shard | Chọn shard key cẩn thận từ đầu |
| ✅ Độ trễ thấp (local queries) | ❌ Application complexity tăng | Abstract logic vào service layer |
| ✅ Independent scaling | ❌ Distributed transactions khó | Thiết kế schema tránh cross-shard transactions |

### 2.1.5. Kết quả đạt được

- ✅ Queries trong 1 khu vực: **< 50ms** (không có network hops giữa shards)
- ✅ Zero cross-shard queries (thiết kế schema tốt)
- ✅ Mỗi shard handle 10,000 chuyến đi/ngày
- ✅ Sẵn sàng scale khi mở rộng sang quốc gia mới (Malaysia, Singapore)

---

## 2.2. Module Message Queue (RabbitMQ)

### 2.2.1. Tổng quan

**Vấn đề:** Trip creation bị block chờ Driver Service tìm tài xế (200ms), gây tight coupling và không chịu được traffic spike.

**Giải pháp:** Asynchronous messaging với RabbitMQ để decouple services và improve availability.

> **Chi tiết kỹ thuật:** [ADR-004: RabbitMQ vs Kafka](docs/ADR/004-rabbitmq-vs-kafka-for-async-messaging.md)

### 2.2.2. Kiến trúc

```
┌───────────────┐         ┌────────────────┐
│ Trip Service  │──Publish→│  RabbitMQ   │─Subscribe→│ Driver Service │
│ Create Trip   │         │   Exchange   │         │  Find Drivers  │
│ (5ms) ✓       │         │   + Queue    │         │  (50ms async)  │
└───────────────┘         └─────────────┘         └────────────────┘
```

**Flow:**
1. Trip Service tạo chuyến đi → save DB (5ms)
2. Publish message to RabbitMQ (non-blocking, 2ms)
3. Return response ngay cho client (7ms total)
4. Driver Service consume message async → tìm tài xế (50ms)

### 2.2.3. Quyết định thiết kế chính

**1. Tại sao chọn RabbitMQ thay vì Kafka?**
- Message rate thấp (5-50 msg/s) → RabbitMQ đủ (capacity: 20K msg/s)
- Kafka quá phức tạp cho use case này (partitions, consumer groups, ZooKeeper)
- RabbitMQ resource usage thấp (150-200MB vs Kafka 500-1000MB)
- Management UI mạnh mẽ (debug và monitor dễ dàng)

**2. Reliability Patterns**
- **Durable Queues:** Messages persist qua broker restart
- **Publisher Confirms:** Đảm bảo message đến broker
- **Manual ACK:** Consumer control khi nào message được xóa
- **Dead Letter Queue:** Auto-route failed messages
- **Retry with Exponential Backoff:** Auto-retry với tăng delay

**3. Performance Tuning**
- Prefetch count: 10 messages per consumer
- Concurrent consumers: 3-10 (dynamic scaling)
- Message TTL: 60 seconds (notifications chỉ có giá trị ngắn)
- Queue max length: 10,000 messages

### 2.2.4. Trade-offs

| Ưu điểm | Nhược điểm | Giải pháp |
|---------|------------|-----------|
| ✅ Decoupling services | ❌ Thêm component phải maintain | Docker Compose auto-start |
| ✅ Absorb traffic spikes | ❌ Eventual consistency (10-50ms delay) | Acceptable cho UX |
| ✅ Reliable delivery | ❌ Async debugging phức tạp | Management UI + correlation IDs |
| ✅ Auto retry | ❌ Network dependency | Auto-reconnect, graceful degradation |

### 2.2.5. Kết quả đạt được

- ✅ Trip creation latency: **5ms** (từ 205ms → 97% faster)
- ✅ Non-blocking: Client nhận response ngay
- ✅ Message throughput: 2,000 msg/s (capacity >> actual load)
- ✅ Zero message loss với durable queues + publisher confirms
- ✅ RAM usage: < 200MB (phù hợp laptop development)

---

## 2.3. Module Redis Read Replicas (CQRS Pattern)

### 2.3.1. Tổng quan

**Vấn đề:** Driver Service có read-heavy workload. Single Redis instance gây bottleneck cho reads.

**Giải pháp:** Redis Read Replicas với CQRS pattern - tách biệt reads và writes.

> **Chi tiết kỹ thuật:** [ADR-006: Redis Read Replicas vs Redis Cluster](docs/ADR/006-redis-replicas-vs-cluster.md)

### 2.3.2. Kiến trúc

```
┌──────────────────────────────────────────────────┐
│           Driver Service Application             │
└─────┬────────────────────────────┬───────────────┘
      │ Writes                     │ Reads
      │ (location updates)         │ (find nearby)
      ▼                            ▼
┌─────────────┐             ┌──────────────┐
│Redis Master │─Replication→│Redis Replica │
│  (Primary)  │────────────→│ (Read-only)  │
│ WRITE ONLY  │             │  READ ONLY   │
└─────────────┘             └──────────────┘
```

### 2.3.3. Quyết định thiết kế chính

**1. CQRS Pattern**
- **Command (Write):** `updateDriverLocation()` → Master only
- **Query (Read):** `findNearbyDrivers()` → Replica only
- Dual `RedisConnectionFactory` với `@Qualifier` annotation
- Application-level routing (không dùng Redis Sentinel)

**2. Optimizations**

**N+1 Query Problem:**
- ❌ Before: 10 individual queries = 20ms
- ✅ After: 1 pipelined batch query = 3ms
- **Improvement: 85% latency reduction**

**KEYS Command Prevention:**
- ❌ KHÔNG dùng: `KEYS driver:availability:*` (blocks Redis)
- ✅ Sử dụng: `SET` data structure cho O(1) lookup
- Alternative: `SCAN` với cursor (non-blocking)

**3. Replication Configuration**
- Async replication (typical lag: 1-10ms)
- Replica read-only mode (prevent accidental writes)
- AOF + RDB persistence for durability

### 2.3.4. Eventual Consistency Trade-off

**Replication Lag:**
- Typical: 1-10ms (local Kubernetes network)
- Max acceptable: 100ms

**Why it's acceptable:**
- Driver location updates every 5 seconds
- 10ms lag << 5000ms update interval
- Geospatial search has ~50m tolerance anyway
- Real-time requirements: ±100ms is acceptable cho ride-hailing

**Monitoring:**
- Track `master_repl_offset` vs `slave_repl_offset`
- Alert if lag > 1000 bytes
- Fallback to master if replica lag too high

### 2.3.5. Trade-offs

| Ưu điểm | Nhược điểm | Giải pháp |
|---------|------------|-----------|
| ✅ Read scalability (horizontal) | ❌ Eventual consistency | Acceptable cho location data |
| ✅ Reduced master load | ❌ Replication lag (1-10ms) | Monitor lag, fallback if needed |
| ✅ Fault tolerance | ❌ 2x storage cost | Redis memory efficient (~5MB/10K drivers) |
| ✅ Optimized for read-heavy | ❌ Application complexity | Abstract với service layer |

### 2.3.6. Kết quả đạt được

- ✅ Read queries offloaded từ master (50% load reduction)
- ✅ Read latency: **3ms** (với pipeline optimization)
- ✅ Write latency unchanged: **2ms**
- ✅ Replication lag: **< 5ms** average
- ✅ Zero data loss với AOF persistence
- ✅ Sẵn sàng scale thêm read replicas khi cần

---

## 2.4. Module Kubernetes Scaling & Resilience

### 2.4.1. Tổng quan

**Vấn đề:** Services cần auto-scale theo traffic và maintain high availability trong production.

**Giải pháp:** Kubernetes native scaling và resilience patterns.

> **Chi tiết kỹ thuật:** [ADR-007: HPA vs VPA for Autoscaling](docs/ADR/007-hpa-vs-vpa-autoscaling.md)

### 2.4.2. Components đã implement

**1. Horizontal Pod Autoscaler (HPA)**
```yaml
# API Gateway HPA
minReplicas: 2
maxReplicas: 10
targetCPUUtilizationPercentage: 70
```

**Tính toán:**
- Normal load: 2 replicas (20% CPU each)
- Peak load (3x traffic): Scale to 6 replicas
- Max burst: 10 replicas

**2. PodDisruptionBudget (PDB)**
```yaml
minAvailable: 1  # Always keep at least 1 pod running
```

**Đảm bảo:**
- Rolling updates không down toàn bộ service
- Node maintenance không gây service outage
- Graceful degradation under failures

**3. Resource Requests & Limits**
```yaml
resources:
  requests:
    cpu: "200m"      # Guaranteed CPU
    memory: "512Mi"  # Guaranteed RAM
  limits:
    cpu: "1000m"     # Max CPU (burst)
    memory: "1Gi"    # Max RAM (OOM kill if exceeded)
```

**Tính toán per Service:**
- Trip Service: 200m CPU, 512Mi RAM (Java application)
- API Gateway: 100m CPU, 256Mi RAM (reactive, lightweight)
- Driver Service: 200m CPU, 512Mi RAM (Redis operations)

**4. Readiness & Liveness Probes**
```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 20
```

**Khác biệt:**
- **Readiness:** Pod ready nhận traffic? (NO → remove from service)
- **Liveness:** Pod còn sống? (NO → restart pod)

### 2.4.3. Rolling Update Strategy

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 1
    maxSurge: 1
```

**Flow:**
1. Start 1 new pod (v2)
2. Wait readiness probe pass
3. Terminate 1 old pod (v1)
4. Repeat until all pods updated
5. Zero downtime deployment

### 2.4.4. Kết quả đạt được

- ✅ Auto-scaling in 30 seconds (HPA poll interval)
- ✅ Zero downtime deployments (rolling update)
- ✅ High availability (PDB ensures min replicas)
- ✅ Resource efficiency (right-sized requests/limits)
- ✅ Fast failure detection (probes)

---

## 2.5. Module Service Mesh (Linkerd)

### 2.5.1. Tổng quan

**Vấn đề:** Microservices cần advanced observability, traffic control, và security (mTLS).

**Giải pháp:** Service mesh với Linkerd (lightweight alternative to Istio).

> **Chi tiết kỹ thuật:** [ADR-008: Linkerd vs Istio for Service Mesh](docs/ADR/008-linkerd-vs-istio.md)

### 2.5.2. Features đã enable

**1. Automatic mTLS**
- All service-to-service traffic encrypted
- Zero code changes (transparent proxy)
- Certificate rotation tự động

**2. Observability**
- Request metrics (success rate, latency, throughput)
- Service topology visualization
- Distributed tracing integration

**3. Traffic Management**
- Request retries
- Timeouts
- Load balancing (least-request algorithm)

**4. Grafana Dashboards**
- Service-level metrics
- Golden signals (latency, traffic, errors, saturation)
- Real-time traffic visualization

### 2.5.3. Tại sao chọn Linkerd thay vì Istio?

| Metric | Linkerd | Istio |
|--------|---------|-------|
| RAM overhead per pod | 10-20MB | 50-100MB |
| Startup latency | +5ms | +20ms |
| Configuration complexity | Low | High |
| Resource usage | Lightweight | Heavy |

**Linkerd phù hợp cho:**
- ✅ Learning environment (laptop-friendly)
- ✅ Simplicity over features
- ✅ Good enough observability
- ✅ Low resource overhead

### 2.5.4. Kết quả đạt được

- ✅ mTLS enabled cho all services (zero config)
- ✅ Real-time metrics in Grafana
- ✅ Service topology visualization
- ✅ RAM overhead: < 50MB for entire mesh
- ✅ Latency impact: < 10ms p99

---

## 2.6. Module Circuit Breaker & Retry

### 2.6.1. Tổng quan

**Vấn đề:** Service failures cascade (Trip Service down → API Gateway timeout → Client errors).

**Giải pháp:** Circuit Breaker pattern với Resilience4j.

> **Chi tiết kỹ thuật:** [ADR-009: Resilience4j vs Hystrix for Circuit Breaker](docs/ADR/009-resilience4j-vs-hystrix.md)

### 2.6.2. Kiến trúc

```
API Gateway → [Circuit Breaker] → Trip Service
                    ↓
              CLOSED (normal)
              OPEN (failing) → Fallback
              HALF_OPEN (testing recovery)
```

**States:**
- **CLOSED:** Normal operation, requests pass through
- **OPEN:** Service failing, reject immediately, return fallback
- **HALF_OPEN:** Periodically test if service recovered

### 2.6.3. Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      tripService:
        failureRateThreshold: 50        # Open if 50% failed
        slidingWindowSize: 10           # Last 10 requests
        waitDurationInOpenState: 10000  # Wait 10s before retry
        permittedNumberOfCallsInHalfOpenState: 3
```

**Retry Configuration:**
```yaml
resilience4j:
  retry:
    instances:
      tripService:
        maxAttempts: 3
        waitDuration: 1000      # 1 second
        exponentialBackoffMultiplier: 2  # 1s, 2s, 4s
```

### 2.6.4. Fallback Strategies

**1. Cached Response**
```java
@Fallback
public Response fallback(Exception e) {
    return cachedResponse();  // Return last known good response
}
```

**2. Degraded Service**
```java
@Fallback
public List<Driver> fallback() {
    return Collections.emptyList();  // Return empty, don't fail
}
```

**3. Error Response**
```java
@Fallback
public Response fallback() {
    return Response.error("Service temporarily unavailable");
}
```

### 2.6.5. Kết quả đạt được

- ✅ Prevent cascading failures
- ✅ Fast-fail when service down (no timeout wait)
- ✅ Automatic recovery testing (half-open state)
- ✅ Improved user experience (fallback responses)
- ✅ Reduced unnecessary retries to failing services

---

## 2.7. Load Testing & Performance Results

### 2.7.1. Testing Strategy

> **Chi tiết:** [ADR-010: k6 vs JMeter for Load Testing](docs/ADR/010-k6-vs-jmeter.md)

**Tool:** k6 (modern load testing tool)

**Scenarios tested:**
1. **Baseline:** Normal traffic (100 req/s)
2. **Spike:** Sudden 10x increase (1000 req/s)
3. **Sustained:** High load for 5 minutes
4. **Stress:** Find breaking point

### 2.7.2. Results Summary

**API Gateway (before optimization):**
- Max throughput: **500 req/s**
- P95 latency: **250ms**
- Error rate at 600 req/s: **15%**

**API Gateway (after HPA + optimization):**
- Max throughput: **2000 req/s**
- P95 latency: **80ms**
- Error rate: **< 0.1%**
- **Improvement: 4x throughput, 68% latency reduction**

**Driver Search (single Redis):**
- Throughput: **3000 req/s**
- P99 latency: **15ms**

**Driver Search (with Read Replica):**
- Throughput: **5000 req/s**
- P99 latency: **12ms**
- **Improvement: 67% throughput increase, 20% latency reduction**

### 2.7.3. Bottlenecks Identified & Fixed

| Bottleneck | Impact | Solution | Result |
|------------|--------|----------|--------|
| Single API Gateway pod | 500 req/s limit | HPA (2-10 replicas) | 2000 req/s |
| Connection pool too small | Timeout errors | Increase to 20 connections | Zero timeouts |
| No Redis replica | Read bottleneck | Add read replica | 67% ↑ throughput |
| Synchronous driver search | 200ms latency | RabbitMQ async | 5ms response |

---

## 3. Tổng kết

### 3.1. Architecture Highlights

**Scalability Achieved:**
- ✅ Database sharding: Horizontal data scaling (2 shards → N shards)
- ✅ Redis replicas: Horizontal read scaling
- ✅ Kubernetes HPA: Horizontal compute scaling (2-10 pods)
- ✅ RabbitMQ: Queue-based load leveling

**Performance Achieved:**
- ✅ Trip creation: **5ms** (target: < 500ms)
- ✅ Driver search: **12ms** (target: < 100ms)
- ✅ Location update: **8ms** (target: < 50ms)
- ✅ API throughput: **2000 req/s** (target: 500 req/s)

**Resilience Achieved:**
- ✅ Zero downtime deployments (rolling updates)
- ✅ Circuit breakers prevent cascading failures
- ✅ Auto-scaling handles traffic spikes
- ✅ Message queues ensure reliable delivery
- ✅ Read replicas provide fault tolerance

### 3.2. Key Trade-offs Made

| Decision | Gained | Lost | Verdict |
|----------|--------|------|---------|
| RabbitMQ (vs sync) | Decoupling, scalability | Eventual consistency | ✅ Worth it |
| Database sharding | Horizontal scaling | Cross-shard queries | ✅ Worth it |
| Redis replicas | Read scalability | Replication lag | ✅ Worth it |
| Linkerd (vs Istio) | Simplicity, low overhead | Advanced features | ✅ Worth it for learning |
| gRPC (vs REST for location) | 50% bandwidth saving | Complexity | ✅ Worth it |

### 3.3. Future Enhancements

**Short-term (next sprint):**
1. Metrics-based HPA (queue length, latency)
2. Database read replicas
3. Advanced caching strategies (Redis TTL tuning)

**Medium-term (next month):**
1. Multi-region deployment
2. Kafka for event streaming (if scale requires)
3. Service mesh advanced features (traffic splitting, canary)

**Long-term (production):**
1. Distributed tracing (Jaeger)
2. Centralized logging (ELK/Loki)
3. Chaos engineering (test resilience)
4. Advanced monitoring (Prometheus + custom metrics)

---

**Tài liệu tham khảo ADR:**
- [ADR-005: Geographic Sharding vs Hash-based Sharding](docs/ADR/005-geographic-sharding-vs-hash-sharding.md)
- [ADR-006: Redis Read Replicas vs Redis Cluster](docs/ADR/006-redis-replicas-vs-cluster.md)
- [ADR-007: HPA vs VPA for Autoscaling](docs/ADR/007-hpa-vs-vpa-autoscaling.md)
- [ADR-008: Linkerd vs Istio for Service Mesh](docs/ADR/008-linkerd-vs-istio.md)
- [ADR-009: Resilience4j vs Hystrix for Circuit Breaker](docs/ADR/009-resilience4j-vs-hystrix.md)
- [ADR-010: k6 vs JMeter for Load Testing](docs/ADR/010-k6-vs-jmeter.md)
