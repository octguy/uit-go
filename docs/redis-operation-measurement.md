# Redis Operation Measurement Guide

## Mục đích

Tool này giúp đo đạc số lượng Redis read/write operations trong quá trình tạo trip và các hành động liên quan. Dữ liệu này sẽ giúp quyết định có nên implement read replicas hay không.

## Cách hoạt động

### 1. RedisOperationCounter (AOP-based)

File: `backend/driver-service/src/main/java/com/example/driver_service/config/RedisOperationCounter.java`

Sử dụng Spring AOP để intercept tất cả Redis operations:

**Read Operations được đếm:**
- `RedisTemplate.opsForValue().get()`
- `RedisTemplate.opsForValue().multiGet()`
- `RedisTemplate.opsForGeo().radius()` (GEORADIUS)
- `RedisTemplate.opsForHash().get()`
- `RedisTemplate.keys()`

**Write Operations được đếm:**
- `RedisTemplate.opsForValue().set()`
- `RedisTemplate.opsForGeo().add()` (GEOADD)
- `RedisTemplate.opsForHash().put()`
- `RedisTemplate.delete()`

### 2. RedisMetricsController

File: `backend/driver-service/src/main/java/com/example/driver_service/controller/RedisMetricsController.java`

REST endpoints để xem và reset counters:

```bash
# Xem thống kê
GET http://localhost:8083/api/driver-service/metrics/redis-ops

# Reset counters
POST http://localhost:8083/api/driver-service/metrics/redis-ops/reset

# Print stats to logs
GET http://localhost:8083/api/driver-service/metrics/redis-ops/print
```

## Cách sử dụng

### Bước 1: Rebuild driver-service

```bash
cd backend/driver-service
mvnw clean package -DskipTests
```

### Bước 2: Restart services

**Windows:**
```bash
cd infra
docker-compose down
docker-compose up -d
```

**Linux:**
```bash
cd infra
docker-compose down
docker-compose up -d
```

### Bước 3: Chạy measurement script

**Windows:**
```bash
cd win-run
measure-redis-ops.bat
```

**Linux:**
```bash
cd linux-run
chmod +x measure-redis-ops.sh
./measure-redis-ops.sh
```

## Kết quả mẫu

Script sẽ thực hiện các bước sau và đếm Redis operations:

1. **Reset counters** - Bắt đầu từ 0
2. **Register driver** - Không có Redis ops (chỉ PostgreSQL)
3. **Update location** - **4 WRITE ops**
   - 1× GEOADD (driver:locations)
   - 3× HSET (lat, lng, updatedAt)
4. **Set status ONLINE** - **1 WRITE op**
   - 1× SET (driver:xxx:status)
5. **Find nearby drivers** - **2+ READ ops** (tùy số driver)
   - 1× GEORADIUS
   - N× GET (status check cho mỗi driver) ⚠️ **N+1 problem!**
6. **Create trip** - Gọi lại find nearby drivers
7. **Get pending notifications** - **1+ READ ops**
   - 1× KEYS (scan pattern)
   - N× GET (mỗi notification)
8. **Accept trip** - **2 READ + 2 WRITE ops**
   - 1× GET (pending notification)
   - 1× SET (update accepted)
   - 1× KEYS (find other notifications)
   - N× DELETE (xóa notifications khác)

### Ví dụ output:

```json
{
  "totalReads": 15,
  "totalWrites": 8,
  "readWriteRatio": "1.88:1",
  "recommendation": "Low read ratio - Read replicas may not be necessary yet"
}
```

## Phân tích kết quả

### Read/Write Ratio thấp (< 5:1)
- ❌ **Không cần read replicas** ở quy mô hiện tại
- ✅ Tối ưu N+1 query problem trước
- ✅ Thêm connection pooling

### Read/Write Ratio trung bình (5:1 - 10:1)
- 🟡 **Cân nhắc read replicas** khi scale lên
- ✅ Monitor Redis CPU usage
- ✅ Load test với k6

### Read/Write Ratio cao (> 10:1)
- ✅ **Nên implement read replicas**
- ✅ Tách read/write operations
- ✅ Optimize với MGET, SCAN

## Vấn đề phát hiện được

### 1. N+1 Query Problem trong `findNearbyDrivers()`

**Hiện tại:**
```java
// 1 query
GeoResults results = redisTemplate.opsForGeo().radius(...);

// N queries (mỗi driver 1 query)
.filter(r -> {
    String status = getStatus(driverId);  // ⚠️ 
    return status.equals("ONLINE");
})
```

**Nên sửa thành:**
```java
// 1 query
GeoResults results = redisTemplate.opsForGeo().radius(...);

// 1 query (batch get tất cả statuses)
List<String> statuses = redisTemplate.opsForValue()
    .multiGet(statusKeys);
```

### 2. KEYS command trong `getPendingNotificationsForDriver()`

**Hiện tại:**
```java
Set<String> keys = redisTemplate.keys(pattern);  // ⚠️ Blocks Redis
```

**Nên sửa thành:**
```java
// Non-blocking SCAN
ScanOptions options = ScanOptions.scanOptions()
    .match(pattern).count(100).build();
Cursor<byte[]> cursor = redisTemplate.scan(options);
```

## Monitoring trong Production

Để monitor trong môi trường production:

### 1. Prometheus Metrics

Thêm vào `RedisOperationCounter.java`:

```java
@Component
public class RedisOperationCounter {
    private final MeterRegistry meterRegistry;
    
    @PostConstruct
    public void init() {
        Gauge.builder("redis.operations.reads", readCount, AtomicLong::get)
            .register(meterRegistry);
        Gauge.builder("redis.operations.writes", writeCount, AtomicLong::get)
            .register(meterRegistry);
    }
}
```

### 2. Grafana Dashboard

Query:
```promql
# Read/Write ratio
rate(redis_operations_reads[5m]) / rate(redis_operations_writes[5m])

# Total operations per second
rate(redis_operations_reads[5m]) + rate(redis_operations_writes[5m])
```

## Kết luận

Tool này giúp bạn:
1. ✅ Đo đạc chính xác số lượng Redis operations
2. ✅ Tính toán read/write ratio
3. ✅ Quyết định có nên implement read replicas
4. ✅ Phát hiện performance bottlenecks (N+1, KEYS)
5. ✅ Có data để justify architectural decisions

**Next steps:**
1. Chạy measurement script
2. Phân tích kết quả
3. Nếu ratio > 10:1 → Implement read replicas
4. Nếu ratio < 10:1 → Fix N+1 problem trước
