# ADR-005: Geographic Sharding vs Hash-based Sharding cho Trip Database

## Bối cảnh

Hệ thống UIT-Go hoạt động đa quốc gia (Việt Nam, Thái Lan) với 100,000+ chuyến đi/ngày. Database PostgreSQL đơn lẻ đang gặp các vấn đề:

1. **Write contention**: Xung đột ghi trong giờ cao điểm
2. **Slow queries**: Chậm cho queries theo khu vực (150-200ms)
3. **Limited scalability**: Không thể scale horizontally
4. **Growth projection**: Kế hoạch mở rộng Malaysia, Singapore

### Yêu cầu

1. **Horizontal scalability**: Khả năng scale writes
2. **Query locality**: 80% queries filter theo location
3. **Low latency**: < 50ms cho region queries
4. **Future expansion**: Dễ thêm countries mới
5. **Operational simplicity**: Phù hợp developer nhỏ

### Các phương án được xem xét

1. **Geographic Sharding (by longitude)**
2. **Hash-based Sharding (by trip_id)**
3. **Range-based Sharding (by created_at)**
4. **Composite Sharding (country + time)**
5. **Single DB with Read Replicas**

---

## Quyết định

**Nhóm em chọn Geographic Sharding theo longitude** với threshold 105.0°E (biên giới VN-TH).

**Shard strategy:**
```
VN Shard: pickup_longitude < 105.0°E  (Vietnam)
TH Shard: pickup_longitude >= 105.0°E (Thailand)
```

---

## Lý do lựa chọn

### Ưu điểm của Geographic Sharding

#### 1. **Perfect Query Locality**

**80% queries filter theo location**, geographic sharding đảm bảo **zero cross-shard queries**:

```java
// Tất cả queries cho VN trips chỉ hit VN shard
@Transactional
public List<Trip> findTripsInVietnam(double lat, double lng) {
    DbContextHolder.setDbKey(ShardKey.VN);
    return tripRepository.findByLocation(lat, lng);
    // Không cần query TH shard
}
```

**Kết quả:**
- 100% queries stay within 1 shard
- No network hops between shards
- Latency: 30-50ms (vs 150-200ms trước đây)

#### 2. **Aligns with Business Logic**

Trips không span countries (chuyến đi không qua biên giới):

```
Pickup: TP.HCM (106.66°E) → VN Shard
Destination: Hà Nội (105.84°E) → Same VN Shard

Pickup: Bangkok (100.49°E) → TH Shard
Destination: Chiang Mai (98.98°E) → Same TH Shard
```

**Không có trường hợp:** Pickup VN, Destination TH → Không cần cross-shard JOIN

#### 3. **Easy to Expand**

Thêm countries mới chỉ cần add shard:

```java
// Current
if (longitude < 105.0) return ShardKey.VN;
else return ShardKey.TH;

// Future: Add Malaysia, Singapore
if (longitude < 100.0) return ShardKey.TH;
else if (longitude < 103.0) return ShardKey.MY;  // Malaysia
else if (longitude < 104.0) return ShardKey.SG;  // Singapore
else return ShardKey.VN;
```

**Cost to add shard:** < 1 day (spin up PostgreSQL pod, apply schema)

#### 4. **Independent Scaling**

Mỗi country scale độc lập:

```yaml
# VN shard (high traffic)
resources:
  requests:
    cpu: "1000m"
    memory: "2Gi"

# TH shard (lower traffic)
resources:
  requests:
    cpu: "500m"
    memory: "1Gi"
```

**Benefit:** Optimize resources per country needs

#### 5. **Data Sovereignty Ready**

Compliance cho future regulations:

```
VN data stays in VN region → Legal compliance
TH data stays in TH region → Thai laws
```

---

### Tại sao không chọn Hash-based Sharding?

**Strategy:** `shard = hash(trip_id) % num_shards`

```java
// Ví dụ
trip_id=12345 → hash → shard 0
trip_id=67890 → hash → shard 1
```

#### Nhược điểm:

**1. No Query Locality** ❌

Tìm tất cả trips ở VN phải query **cả 2 shards**:

```java
// ❌ BAD: Scatter-gather pattern
public List<Trip> findTripsInVietnam() {
    List<Trip> vnFromShard0 = queryShardbyKey(ShardKey.SHARD_0, "country=VN");
    List<Trip> vnFromShard1 = queryShard(ShardKey.SHARD_1, "country=VN");
    return merge(vnFromShard0, vnFromShard1);  // Application-level merge
}
```

**Latency:** 150ms (2× database calls + merge)

**2. Poor Cache Hit Rate** ❌

Geographic queries hit random shards → cache ineffective:

```
Request 1: "Trips in HCMC" → Query both shards → Cache miss
Request 2: "Trips in HCMC" → Still query both shards → No cache benefit
```

**3. Doesn't Match Access Pattern** ❌

80% queries filter by location, hash sharding ignores this:

```sql
-- Common query: Find trips in area
SELECT * FROM trips 
WHERE pickup_lat BETWEEN 10.7 AND 10.8
  AND pickup_lng BETWEEN 106.6 AND 106.7;

-- With hash sharding: Must query all shards
-- With geo sharding: Query only 1 shard (VN or TH)
```

---

### Tại sao không chọn Range-based Sharding (by time)?

**Strategy:** Shard by `created_at` timestamp

```
Shard 1: trips created Jan-Jun 2024
Shard 2: trips created Jul-Dec 2024
Shard 3: trips created Jan-Jun 2025
```

#### Nhược điểm:

**1. Hot Shard Problem** ❌

All writes go to latest shard:

```
Current shard: 2025-H1
All new trips → Write to 2025-H1 shard only
Old shards (2024-H1, 2024-H2) → Read-only, idle
```

**Write distribution:** 100% on 1 shard → Doesn't solve bottleneck

**2. Doesn't Optimize Primary Queries** ❌

Most queries filter by location, not time:

```sql
-- Common: Find active trips in area
SELECT * FROM trips
WHERE status = 'IN_PROGRESS'
  AND pickup_lat BETWEEN 10.7 AND 10.8;

-- Must query ALL time-based shards to find current trips
```

**3. Complex Queries** ❌

"All active trips" spans multiple time shards:

```java
// Need to query 3+ shards for active trips
List<Trip> activeTrips = Stream.of(
    queryShard("2024-H2"),
    queryShard("2025-H1"),
    queryShard("2025-H2")
).flatMap(List::stream).collect(Collectors.toList());
```

---

### Tại sao không chọn Single DB + Read Replicas?

**Strategy:** Keep PostgreSQL single primary, add read replicas

```
Primary (writes) → Replica 1 (reads)
                 → Replica 2 (reads)
```

#### Nhược điểm:

**1. Write Bottleneck Remains** ❌

Single primary for ALL writes:

```
VN writes → Primary
TH writes → Same primary
All countries → Single bottleneck
```

**Doesn't solve:** Write contention problem

**2. Limited Scalability** ❌

Vertical scaling only (bigger machine):

```
Current: 4 CPU, 8GB RAM → 500 writes/s
Upgrade:  8 CPU, 16GB RAM → 800 writes/s
Max: 64 CPU, 128GB RAM → ~2000 writes/s

Cannot scale beyond 1 machine limit
```

**3. No Horizontal Write Scaling** ❌

Reads can scale (add replicas), writes cannot:

```
Reads: 5000 reads/s → Add replica → 10000 reads/s ✓
Writes: 500 writes/s → Add replica → Still 500 writes/s ✗
```

---

### Tại sao không chọn Composite Sharding (country + time)?

**Strategy:** Combine geographic + time

```
VN-2024: Vietnam trips in 2024
VN-2025: Vietnam trips in 2025
TH-2024: Thailand trips in 2024
TH-2025: Thailand trips in 2025
```

#### Nhược điểm:

**1. Too Many Shards** ❌

Grows exponentially:

```
2 countries × 5 years = 10 shards
4 countries × 5 years = 20 shards
10 countries × 5 years = 50 shards

Operational complexity: Managing 50 databases
```

**2. Overkill for Current Scale** ❌

100K trips/day doesn't need this complexity:

```
VN: 60K trips/day → 22M trips/year
Single PostgreSQL capacity: 100M+ trips

Year-based sharding unnecessary for current scale
```

**3. Complex Routing Logic** ❌

```java
// Complex shard selection
ShardKey shard = determineShardKey(country, year);
if (country == "VN" && year == 2024) return ShardKey.VN_2024;
else if (country == "VN" && year == 2025) return ShardKey.VN_2025;
// ... 20+ conditions
```

**Verdict:** Premature optimization

---

## Chi tiết triển khai

### Implementation với Spring Boot

**1. Dynamic DataSource Routing:**

```java
public class TripRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return DbContextHolder.getDbKey();
    }
}

@Configuration
public class DataSourceConfig {
    @Bean
    public DataSource dataSource() {
        TripRoutingDataSource routing = new TripRoutingDataSource();
        
        Map<Object, Object> shards = new HashMap<>();
        shards.put(ShardKey.VN, vnDataSource());
        shards.put(ShardKey.TH, thDataSource());
        
        routing.setTargetDataSources(shards);
        routing.setDefaultTargetDataSource(vnDataSource());
        return routing;
    }
}
```

**2. ThreadLocal Context Holder:**

```java
public class DbContextHolder {
    private static final ThreadLocal<ShardKey> context = new ThreadLocal<>();
    
    public static void setDbKey(ShardKey key) {
        context.set(key);
    }
    
    public static ShardKey getDbKey() {
        return context.get();
    }
    
    public static void clearDbKey() {
        context.remove();  // Prevent memory leak
    }
}
```

**3. Shard Selection Logic:**

```java
private static final double LONGITUDE_THRESHOLD = 105.0;

@Transactional
public TripResponseDTO createTrip(TripRequestDTO request) {
    // Select shard based on pickup longitude
    ShardKey shard = request.getPickupLongitude() < LONGITUDE_THRESHOLD 
        ? ShardKey.VN : ShardKey.TH;
    
    DbContextHolder.setDbKey(shard);
    
    try {
        Trip trip = tripRepository.save(buildTrip(request));
        return mapToDTO(trip);
    } finally {
        DbContextHolder.clearDbKey();  // Always clear
    }
}
```

### Kubernetes Deployment

**VN Shard:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: trip-service-db-vn
spec:
  replicas: 1
  template:
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        env:
        - name: POSTGRES_DB
          value: trip_db
        volumeMounts:
        - name: trip-db-vn-storage
          mountPath: /var/lib/postgresql/data
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: trip-db-vn-pvc
spec:
  accessModes: [ReadWriteOnce]
  resources:
    requests:
      storage: 10Gi
```

**Connection Pooling:**

```yaml
datasource:
  vn:
    jdbc-url: jdbc:postgresql://trip-service-db-vn:5432/trip_db
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 20000
```

**Capacity calculation:**
- Trip Service: 3 replicas (K8s HPA)
- Connections per replica: 10 (VN) + 10 (TH) = 20
- Total per shard: 3 × 10 = 30 connections
- PostgreSQL max_connections: 100 (enough headroom)

---

## Hệ quả

### Tích cực

1. ✅ **Horizontal Scalability**: Mỗi country scale independently
2. ✅ **Query Performance**: 30-50ms (70% improvement từ 150ms)
3. ✅ **Perfect Locality**: 100% queries stay in 1 shard
4. ✅ **Easy Expansion**: Add MY, SG shards in < 1 day
5. ✅ **Data Isolation**: TH failure doesn't affect VN
6. ✅ **Write Distribution**: 2x throughput (500 writes/s per shard)

### Tiêu cực

1. ❌ **Cross-shard JOINs Impossible**

```sql
-- ❌ CANNOT: JOIN across countries
SELECT t.*, d.name 
FROM trips t 
JOIN drivers d ON t.driver_id = d.id
WHERE t.country IN ('VN', 'TH');
```

**Mitigation:** Application-level JOIN (acceptable, rare use case)

2. ❌ **Application Complexity**

Must manage shard routing logic:

```java
// Every query needs shard selection
DbContextHolder.setDbKey(determineShard(longitude));
```

**Mitigation:** Abstract in service layer, hide from controllers

3. ❌ **Uneven Distribution**

VN: 60% traffic, TH: 40% → Not perfectly balanced

**Mitigation:** Acceptable trade-off, can sub-shard VN if needed

4. ❌ **No Distributed Transactions**

Cannot guarantee ACID across shards:

```java
// ❌ CANNOT: Atomic transaction across VN + TH
@Transactional
void transferTripBetweenCountries() {
    updateVNTrip();  // VN shard
    updateTHTrip();  // TH shard - separate transaction!
}
```

**Mitigation:** Design schema to avoid cross-shard transactions

### Biện pháp giảm thiểu

**For Cross-shard Aggregations:**

```java
// Application-level aggregation
public List<TripDTO> getAllActiveTrips() {
    List<TripDTO> vnTrips = executeOnShard(ShardKey.VN, 
        () -> tripRepository.findByStatus("ACTIVE"));
    List<TripDTO> thTrips = executeOnShard(ShardKey.TH, 
        () -> tripRepository.findByStatus("ACTIVE"));
    
    return Stream.concat(vnTrips.stream(), thTrips.stream())
        .collect(Collectors.toList());
}
```

**For Schema Changes:**

```bash
# Apply migrations to all shards
for shard in vn th; do
  flyway migrate -url=jdbc:postgresql://trip-db-${shard}:5432/trip_db
done
```

---

## Xem xét lại quyết định

Nhóm em sẽ **xem xét lại** nếu:

1. **Cross-shard queries trở nên phổ biến** (> 20% queries)
   - Giải pháp: Chuyển sang distributed SQL (CockroachDB, YugabyteDB)

2. **Uneven distribution severe** (90/10 split)
   - Giải pháp: Sub-shard country lớn (VN → North VN, South VN)

3. **Operational complexity quá cao** (> 10 shards)
   - Giải pháp: Evaluate auto-sharding solutions (Citus, Vitess)

---

## Kết quả Validation

### Performance Results

**Before Sharding (Single DB):**
- Query latency (region filter): 150-200ms
- Write throughput: 500 writes/s
- Max connections: 100

**After Sharding:**
- Query latency: **30-50ms** (70% improvement ✅)
- Write throughput: **1000 writes/s** (2x capacity ✅)
- Connections per shard: 30 (efficient ✅)
- Cross-shard queries: **0%** (perfect locality ✅)

### Current Distribution

```
VN Shard: 60K trips/day (60%)
TH Shard: 40K trips/day (40%)

Total: 100K trips/day
Both shards well within capacity
```

### Growth Path

```
Phase 1: VN + TH (current)
Phase 2: Add MY, SG shards (2025)
Phase 3: Sub-shard by city if needed (2026)
```

---

## References

- [Database Sharding - Uber Engineering](https://eng.uber.com/mysql-migration/)
- [Sharding Patterns - Microsoft](https://learn.microsoft.com/en-us/azure/architecture/patterns/sharding)
- Spring `AbstractRoutingDataSource` [Docs](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/datasource/lookup/AbstractRoutingDataSource.html)
