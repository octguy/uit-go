# ADR-006: Redis Read Replicas vs Redis Cluster cho Driver Location Scaling

## Bối cảnh

Driver Service có **read-heavy workload** với Redis:

- **Writes:** 2,000 location updates/second (driver position updates every 5s)
- **Reads:** 5,000 driver searches/second (find nearby drivers)
- **Read:Write ratio = 2.5:1**

Single Redis instance đang gặp bottleneck:
- P99 read latency: 20ms (target < 15ms)
- CPU usage: 65% during peak
- Read operations contend with writes

### Yêu cầu

1. **Read Performance**: Target P99 < 15ms for driver searches
2. **Write Performance**: Maintain P99 < 10ms for location updates
3. **Scalability**: Handle 8,000+ reads/s capacity
4. **Availability**: No single point of failure
5. **Consistency**: Eventual consistency acceptable (location data changes every 5s)
6. **Operational Simplicity**: Phù hợp cho learning environment

### Các phương

 án được xem xét

1. **Redis Read Replicas với CQRS Pattern**
2. **Redis Cluster (Sharding)**
3. **Vertical Scaling (Bigger Instance)**
4. **Redis Sentinel (HA only)**
5. **Client-side Caching**

---

## Quyết định

**Nhóm em chọn Redis Read Replicas với CQRS Pattern** - tách biệt read và write paths.

**Architecture:**
```
Writes (location updates) → Master Redis
Reads (find nearby)       → Replica Redis
Master → Async Replication → Replica (1-10ms lag)
```

---

## Lý do lựa chọn

### Ưu điểm của Redis Read Replicas

#### 1. **Directly Solves Read Bottleneck**

Offload 71% traffic từ master:

```
Total operations: 7,000 ops/s
  Reads: 5,000 ops/s  (71%) → Replica
  Writes: 2,000 ops/s (29%) → Master

Master load: 65% → 35% (reduced)
Replica load: 0% → 40% (balanced)
```

**Result:** Master CPU freed up for writes, reads get dedicated resources

#### 2. **CQRS Pattern Match**

Read và write operations hoàn toàn khác nhau:

```java
// COMMAND (Write) - Master only
public void updateDriverLocation(String driverId, double lat, double lon) {
    masterRedis.opsForGeo().add("drivers:locations", 
        new Point(lon, lat), driverId);
}

// QUERY (Read) - Replica only
public List<Driver> findNearbyDrivers(double lat, double lon, double radiusKm) {
    return replicaRedis.opsForGeo().radius("drivers:locations",
        new Circle(new Point(lon, lat), radiusKm));
}
```

**Benefit:** Clear separation of concerns

#### 3. **Horizontal Read Scaling**

Easy to add more replicas:

```
Current:  1 Master + 1 Replica = 5,000 reads/s
Scale up: 1 Master + 2 Replicas = 10,000 reads/s
Scale up: 1 Master + 3 Replicas = 15,000 reads/s
```

**Cost:** Just add replica pods, no application changes

#### 4. **Eventual Consistency Acceptable**

Location data changes slowly:

```
Driver updates location every 5 seconds
Replication lag: 1-10ms
Lag << Update interval (10ms << 5000ms)

Search result with 10ms old location:
  Distance error: ~0.5 meters (negligible for 5km search)
```

**Verdict:** 10ms staleness is acceptable

#### 5. **Fault Tolerance Bonus**

Replica can be promoted to master if master fails:

```
Normal: Master (writes + monitor) → Replica (reads)
Failure: Master down → Promote Replica → New master
Recovery time: ~30 seconds (manual) or use Redis Sentinel
```

---

### Tại sao không chọn Redis Cluster?

**Strategy:** Shard data across multiple Redis nodes using consistent hashing

```
Hash Slot Distribution:
  Node 1: slots 0-5460     (33%)
  Node 2: slots 5461-10922 (33%)
  Node 3: slots 10923-16383(34%)
```

#### Nhược điểm:

**1. GEORADIUS Cross-node Queries** ❌

Geospatial queries might need multiple nodes:

```
GEORADIUS drivers:locations 106.66 10.76 5 km

With cluster:
  Driver1 → Node 1 (hash slot 1234)
  Driver2 → Node 2 (hash slot 5678)
  Driver3 → Node 3 (hash slot 9012)

Must query ALL 3 nodes → Merge results
Latency: 5ms × 3 = 15ms (vs 5ms single node)
```

**Redis Cluster không optimize cho geospatial:**
- Cannot guarantee nearby drivers are in same slot
- GEORADIUS becomes scatter-gather query
- Performance degradation for our use case

**2. Cannot Use Multi-key Operations** ❌

```redis
# ❌ CANNOT: Multi-key batch operations in cluster
MGET driver:1:status driver:2:status driver:3:status
# Fails if keys are on different nodes

# ❌ CANNOT: Pipeline with keys on different slots
PIPELINE
  GET driver:1:availability
  GET driver:2:availability
END
```

**Impact:** Our N+1 optimization (pipeline) won't work

**3. Operational Complexity** ❌

Cluster requires minimum 6 nodes:

```
3 Master nodes  (for data distribution)
3 Replica nodes (for HA)

Resource usage:
  6 Redis pods × 256MB = 1.5GB RAM
vs
  2 Redis pods (master + replica) = 512MB RAM
```

**Cost:** 3x resource usage for minimal benefit

**4. We Don't Have Write Bottleneck** ❌

Cluster shards WRITES across nodes:

```
Current write load: 2,000 writes/s
Single Redis capacity: 100,000+ writes/s

Utilization: 2% of capacity
```

**Verdict:** Sharding writes is unnecessary, we need to scale READS

---

### Tại sao không chọn Vertical Scaling?

**Strategy:** Increase CPU/RAM of single Redis instance

```
Current: 2 CPU, 2GB RAM
Upgrade: 4 CPU, 4GB RAM
```

#### Nhược điểm:

**1. Doesn't Scale Horizontally** ❌

Limited by single machine:

```
Max practical: 32 CPU, 64GB RAM
Read throughput: ~20,000 reads/s (estimated)

Cannot grow beyond single node limit
```

**2. Still Single Point of Failure** ❌

```
Redis down → Entire Driver Service down
No redundancy, no failover
```

**3. Expensive** ❌

```
Vertical: 4 CPU → 8 CPU = 2x cost
Horizontal: 1 replica = 1x additional cost (same capacity)

Vertical scaling has diminishing returns
```

**4. Read Contention Remains** ❌

Reads and writes still compete:

```
5,000 reads + 2,000 writes = 7,000 ops on same CPU
Reads must wait for write operations
```

**Verdict:** Doesn't address fundamental architecture

---

### Tại sao không chọn Redis Sentinel?

**Strategy:** High availability with automatic failover

```
Sentinel 1 ─┐
Sentinel 2 ─┼→ Monitor Master & Replica → Auto-failover
Sentinel 3 ─┘
```

#### Nhược điểm:

**1. Doesn't Scale Reads** ❌

Sentinel provides HA, not read scaling:

```
With Sentinel:
  Master handles: 7,000 ops/s (same bottleneck)
  Replica: Idle (only used if master fails)

Read performance: No improvement
```

**Problem:** We need read scaling, not just HA

**2. Extra Components** ❌

Need 3+ Sentinel processes:

```
3 Sentinel pods
1 Master pod
1+ Replica pods

Total: 5+ pods (complexity)
```

**3. Solves Wrong Problem** ❌

```
Our problem: Read bottleneck (performance)
Sentinel solves: Master failure (availability)

Mismatch between solution and problem
```

**Note:** Sentinel can be added LATER for HA, but doesn't solve current issue

---

### Tại sao không chọn Client-side Caching?

**Strategy:** Cache frequently accessed data in application memory

```java
@Cacheable(value = "nearbyDrivers", key = "#lat + ':' + #lon")
public List<Driver> findNearbyDrivers(double lat, double lon) {
    return redisTemplate.execute(...);
}
```

#### Nhược điểm:

**1. Data Too Dynamic** ❌

Driver locations change every 5 seconds:

```
T=0s:  Driver at (10.762, 106.660) → Cache
T=5s:  Driver moved to (10.765, 106.662) → Cache stale!
T=10s: Driver moved to (10.768, 106.664) → Cache very stale!

Cache hit rate: Low (data invalidated every 5s)
```

**2. Cache Invalidation Complexity** ❌

How to invalidate across multiple pods?

```
3 Driver Service pods, each with own cache:
  Pod 1: Cached nearby drivers for location A
  Pod 2: Cached nearby drivers for location A (different)
  Pod 3: Cached nearby drivers for location A (different)

Driver updates location → Invalidate all 3 caches?
```

**Distributed cache invalidation:** Complex and error-prone

**3. Memory Usage** ❌

Each pod caches independently:

```
Cache size per pod: 50MB
3 pods × 50MB = 150MB

vs Redis (shared):
  50MB total (all pods use same Redis)
```

**4. Not Suitable for Location Data** ❌

Location searches are unique:

```
Search 1: lat=10.762, lng=106.660, radius=5km
Search 2: lat=10.763, lng=106.661, radius=5km

Different keys → Cache miss
Even though results are 99% similar
```

**Cache hit rate:** Very low for geospatial queries

---

## Chi tiết triển khai

### Dual Connection Factories

```java
@Configuration
public class RedisConfig {
    
    @Bean
    @Primary
    public RedisConnectionFactory masterConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("redis-master-service");
        config.setPort(6379);
        config.setPassword("uitgo123");
        return new LettuceConnectionFactory(config);
    }
    
    @Bean("replicaConnectionFactory")
    public RedisConnectionFactory replicaConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("redis-replica-service");
        config.setPort(6379);
        config.setPassword("uitgo123");
        return new LettuceConnectionFactory(config);
    }
    
    @Bean
    @Primary
    public RedisTemplate<String, Object> masterRedisTemplate() {
        return createTemplate(masterConnectionFactory());
    }
    
    @Bean("replicaRedisTemplate")
    public RedisTemplate<String, Object> replicaRedis Template(
        @Qualifier("replicaConnectionFactory") RedisConnectionFactory factory
    ) {
        return createTemplate(factory);
    }
}
```

### CQRS Implementation

```java
@Service
public class DriverLocationService {
    
    @Qualifier("masterRedisTemplate")
    private final RedisTemplate<String, Object> masterRedis;
    
    @Qualifier("replicaRedisTemplate")
    private final RedisTemplate<String, Object> replicaRedis;
    
    // COMMAND - Write to Master
    public void updateDriverLocation(String driverId, double lat, double lon) {
        masterRedis.opsForGeo().add("drivers:locations",
            new Point(lon, lat), driverId);
    }
    
    // QUERY - Read from Replica
    public List<NearbyDriverDTO> findNearbyDrivers(
        double lat, double lon, double radiusKm
    ) {
        GeoResults<GeoLocation<Object>> results = replicaRedis.opsForGeo()
            .radius("drivers:locations",
                new Circle(new Point(lon, lat), radiusKm),
                args().includeDistance().limit(10));
        
        return mapToDTO(results);
    }
}
```

### Replication Configuration

**Master:**
```conf
# redis-master.conf
port 6379
requirepass uitgo123
appendonly yes
appendfsync everysec
```

**Replica:**
```conf
# redis-replica.conf
port 6379
requirepass uitgo123
replicaof redis-master-service 6379
masterauth uitgo123
replica-read-only yes
```

### Kubernetes Deployment

```yaml
# Master
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis-master
spec:
  replicas: 1
  template:
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        command: ["redis-server", "--requirepass", "uitgo123", "--appendonly", "yes"]
---
# Replica
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis-replica
spec:
  replicas: 1
  template:
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        command: 
        - redis-server
        - --replicaof
        - redis-master-service
        - "6379"
        - --masterauth
        - uitgo123
```

### N+1 Query Optimization

**Before (N+1 problem):**
```java
// 1 query to find drivers
List<String> driverIds = findNearbyDriverIds(...);

// N queries to check availability
return driverIds.stream()
    .map(id -> replicaRedis.opsForValue().get("availability:" + id))  // N queries!
    .collect(Collectors.toList());
```

**After (Pipeline):**
```java
List<String> driverIds = findNearbyDriverIds(...);

// 1 pipelined query for all availabilities
List<Object> availabilities = replicaRedis.executePipelined(
    (RedisOperations ops) -> {
        driverIds.forEach(id -> ops.opsForValue().get("availability:" + id));
        return null;
    }
);

// Performance: 20ms → 3ms (85% improvement)
```

---

## Hệ quả

### Tích cực

1. ✅ **Read Throughput Increased**: 3,000 → 5,000 reads/s (67% ↑)
2. ✅ **Read Latency Improved**: P99 20ms → 12ms (40% ↓)
3. ✅ **Master Load Reduced**: CPU 65% → 35% (offloaded reads)
4. ✅ **Horizontal Scaling**: Easy to add more replicas
5. ✅ **Fault Tolerance**: Can promote replica if master fails
6. ✅ **Simple Implementation**: Just add `@Qualifier` annotations

### Tiêu cực

1. ❌ **Eventual Consistency**: 1-10ms replication lag

**Impact Analysis:**
```
Driver updates location: T=0ms
Replication completes: T=5ms (max 10ms)
Search queries replica: T=3ms

Worst case: Search gets 10ms old location
  Driver speed: 60 km/h = 16.7 m/s
  Distance error: 16.7 m/s × 0.01s = 0.17 meters
  
Search radius: 5000 meters
Error: 0.17 meters = 0.0034% error
```

**Verdict:** Negligible for ride-hailing use case

2. ❌ **2x Storage Cost**: Data duplicated in replica

```
10,000 drivers × 100 bytes = 1MB per instance
Master: 1MB
Replica: 1MB
Total: 2MB

Cost: Negligible (Redis lightweight)
```

3. ❌ **Application Complexity**: Need to route reads vs writes

**Mitigation:** Abstracted in service layer
```java
// Clear separation, developers don't think about routing
updateDriverLocation()  // Internally uses master
findNearbyDrivers()     // Internally uses replica
```

### Biện pháp giảm thiểu

**Monitor Replication Lag:**

```java
@Scheduled(fixedDelay = 10000)  // Every 10 seconds
public void monitorReplicationLag() {
    long masterOffset = getMasterReplOffset();
    long replicaOffset = getReplicaReplOffset();
    long lag = masterOffset - replicaOffset;
    
    if (lag > 1000) {  // > 1KB behind
        log.warn("High replication lag: {} bytes", lag);
        // Optionally: fallback to master for reads
    }
}
```

**Fallback Strategy:**
```java
public List<Driver> findNearbyDrivers(...) {
    if (replicationLagHigh()) {
        return findFromMaster(...);  // Fallback to master
    }
    return findFromReplica(...);  // Normal path
}
```

---

## Xem xét lại quyết định

Nhóm em sẽ **xem xét lại** nếu:

1. **Read load exceeds 8,000 reads/s**
   - Giải pháp: Add 2nd replica (capacity → 12,000 reads/s)

2. **Write load exceeds 50,000 writes/s**
   - Giải pháp: Evaluate Redis Cluster for write sharding

3. **Replication lag consistently > 100ms**
   - Giải pháp: Investigate network issues or upgrade Redis version

4. **Need strict consistency** (business requirements change)
   - Giải pháp: All queries to master, sacrifice read performance

---

## Kết quả Validation

### Performance Results

**Before (Single Redis):**
- Read throughput: 3,000 req/s
- Read latency P99: 20ms
- Write latency P99: 8ms
- Master CPU: 65%

**After (Master + Replica):**
- Read throughput: **5,000 req/s** (67% increase ✅)
- Read latency P99: **12ms** (40% improvement ✅)
- Write latency P99: **8ms** (unchanged ✅)
- Master CPU: **35%** (reduced ✅)
- Replica CPU: **40%** (dedicated for reads)
- Replication lag avg: **< 5ms** ✅

### Capacity Planning

```
Current load:
  Reads: 5,000/s
  Writes: 2,000/s

Replica capacity: ~8,000 reads/s
Headroom: 60% before needing 2nd replica

Future scaling:
  1 Master + 2 Replicas = 12,000 reads/s
  1 Master + 3 Replicas = 18,000 reads/s
```

---

## References

- [Redis Replication](https://redis.io/docs/management/replication/)
- [CQRS Pattern - Martin Fowler](https://martinfowler.com/bliki/CQRS.html)
- [Spring Data Redis Multi-Connection](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [ADR-001: Redis for Geospatial](001-redis-vs-dynamodb-for-geospatial.md)
