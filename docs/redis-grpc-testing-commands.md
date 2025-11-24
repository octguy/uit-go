# 🚗 UIT-Go Driver-Service: Redis & gRPC Testing Guide

Tài liệu này tổng hợp **tất cả các lệnh quan trọng** để kiểm tra hệ thống Driver-Service, Trip-Service, Redis, và Driver-Simulator.

---

## 1. Vào Redis CLI

Nếu chạy Redis bằng Docker Compose:

```bash
docker exec -it redis redis-cli
```

Nếu chạy Redis local:

```bash
redis-cli
```

---

## 2. Kiểm tra danh sách Key trong Redis

```bash
KEYS *
```

---

## 3. Lấy toạ độ GEO của tài xế

```bash
GEOPOS drivers:locations <driverId>
```

Ví dụ:

```bash
GEOPOS drivers:locations 4236bc9f-afb8-4d62-a966-ab79b8bf830a
```

---

## 4. Tìm tài xế gần một vị trí cụ thể (Redis trực tiếp)

```bash
GEOSEARCH drivers:locations FROMLONLAT <lng> <lat> BYRADIUS 3 km WITHDIST WITHCOORD
```

Ví dụ:

```bash
GEOSEARCH drivers:locations FROMLONLAT 106.69064909219742 10.773321541456605 BYRADIUS 3 km WITHDIST WITHCOORD
```

---

## 5. Xem thông tin driver trong Redis (HASH)

```bash
HGETALL driver:<driverId>
```

Ví dụ:

```bash
HGETALL driver:4236bc9f-afb8-4d62-a966-ab79b8bf830a
```

---

## 6. Gọi API `/nearby` trực tiếp vào Driver-Service (8083)

```bash
curl -s "http://localhost:8083/api/internal/drivers/nearby?lat=<lat>&lng=<lng>&radiusKm=3&limit=5" | jq
```

Ví dụ thực tế:

```bash
curl -s "http://localhost:8083/api/internal/drivers/nearby?lat=10.773321541456605&lng=106.69064909219741821&radiusKm=3&limit=5" | jq
```

---

## 7. Gọi API `/nearby` thông qua Trip-Service (8082)

```bash
curl -s "http://localhost:8082/api/trips/driver/get-nearby-drivers?lat=<lat>&lng=<lng>&radiusKm=3&limit=5" | jq
```

Ví dụ thực tế:

```bash
curl -s "http://localhost:8082/api/trips/driver/get-nearby-drivers?lat=10.773321541456605&lng=106.69064909219741821&radiusKm=3&limit=5" | jq
```

---

## 8. Bắt đầu mô phỏng tài xế chạy (Driver-Simulator – port 8084)

```bash
curl -X POST "http://localhost:8084/api/simulate/start-all?startLat=<lat1>&startLng=<lng1>&endLat=<lat2>&endLng=<lng2>&steps=200&delayMillis=1000"
```

Ví dụ:

```bash
curl -X POST "http://localhost:8084/api/simulate/start-all?startLat=10.762622&startLng=106.660172&endLat=10.776889&endLng=106.700806&steps=200&delayMillis=1000"
```

---

## 9. Theo dõi Redis realtime

Trong Redis CLI:

```bash
MONITOR
```

Bạn sẽ thấy:

```
GEOADD drivers:locations <lng> <lat> <driverId>
HSET driver:<id> lat <value>
HSET driver:<id> lng <value>
HSET driver:<id> updatedAt <timestamp>
```

---

## 10. Xoá toàn bộ dữ liệu driver trong Redis (reset)

```bash
DEL drivers:locations
```

Xoá toàn bộ driver hash-key:

```bash
SCAN 0 MATCH "driver:*" COUNT 99999
```

Rồi xoá từng key:

```bash
DEL driver:<id>
```

---

## 11. Kiểm tra khoảng cách giữa 2 tài xế (Redis)

```bash
GEODIST drivers:locations <driver1> <driver2> km
```

---

# DONE!

Bạn có thể sử dụng file README này để kiểm tra toàn bộ hệ thống: 
- Driver-Simulator → gRPC → Driver-Service
- Driver-Service → Redis GEO
- Trip-Service → Feign → Driver-Service
- Toạ độ, khoảng cách, nearby drivers
