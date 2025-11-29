# Load Testing cho API Gateway

## Cài đặt k6

### Windows (dùng Chocolatey)
```bash
choco install k6
```

### Hoặc download trực tiếp
https://k6.io/docs/get-started/installation/

## Các loại test

### 1. Stress Test - Tìm điểm giới hạn
**Mục đích:** Tìm breaking point của API Gateway

```bash
k6 run stress-test-api-gateway.js
```

**Kết quả quan tâm:**
- `http_reqs`: Tổng số requests
- `http_req_duration`: Response time (p95, p99)
- `http_req_failed`: Error rate
- `iterations`: Số lần chạy thành công

### 2. RPS Test - Đo chính xác request/giây
**Mục đích:** Đo chính xác API Gateway chịu được bao nhiêu RPS

```bash
# Test với 100 RPS
k6 run rps-test.js

# Test với 200 RPS (sửa rate: 200 trong file)
k6 run rps-test.js

# Test với 500 RPS
k6 run rps-test.js
```

**Cách tìm max RPS:**
1. Bắt đầu với rate thấp (100 RPS)
2. Tăng dần rate (200, 300, 500, 1000...)
3. Khi error rate > 1% hoặc p95 latency > 1s → đã đạt giới hạn

## Chạy test với output đẹp

### Output ra console với summary
```bash
k6 run --summary-export=summary.json stress-test-api-gateway.js
```

### Output ra HTML report (cần extension)
```bash
# Cài extension
go install go.k6.io/xk6/cmd/xk6@latest
xk6 build --with github.com/grafana/xk6-dashboard@latest

# Chạy với dashboard
./k6 run --out dashboard stress-test-api-gateway.js
```

### Output ra InfluxDB + Grafana (advanced)
```bash
k6 run --out influxdb=http://localhost:8086/k6 stress-test-api-gateway.js
```

## Giám sát trong khi test

### 1. Xem metrics trong Linkerd
```bash
linkerd viz dashboard
```

### 2. Xem Grafana
```bash
kubectl port-forward -n linkerd-viz svc/grafana 3000:3000
```
Mở: http://localhost:3000

### 3. Xem Prometheus
```bash
kubectl port-forward -n linkerd-viz svc/prometheus 9090:9090
```
Mở: http://localhost:9090

### 4. Xem Kubernetes metrics
```bash
# CPU/Memory usage
kubectl top pods

# HPA status (nếu có)
kubectl get hpa

# Pod status
kubectl get pods -w
```

## Kịch bản test đề xuất

### Test 1: Baseline (1 replica)
```bash
# Scale API Gateway về 1 replica
kubectl scale deployment api-gateway --replicas=1

# Chạy stress test
k6 run stress-test-api-gateway.js

# Ghi lại kết quả: Max RPS, p95 latency, error rate
```

### Test 2: Horizontal Scaling (3 replicas)
```bash
# Scale lên 3 replicas
kubectl scale deployment api-gateway --replicas=3

# Chạy lại stress test
k6 run stress-test-api-gateway.js

# So sánh: RPS có tăng gấp 3 không?
```

### Test 3: Với HPA (auto-scaling)
```bash
# Đảm bảo HPA đang enabled
kubectl get hpa api-gateway

# Chạy stress test
k6 run stress-test-api-gateway.js

# Quan sát HPA scale up
kubectl get hpa -w
```

## Phân tích kết quả

### Metrics quan trọng:

1. **Max RPS** - Số request/giây tối đa trước khi error
2. **p95 Latency** - 95% requests có response time < X ms
3. **Error Rate** - % requests bị fail
4. **Resource Usage** - CPU/Memory khi đạt max load

### Ví dụ kết quả tốt:
```
✓ Max RPS: 1000 req/s (1 replica)
✓ p95 Latency: 250ms
✓ Error Rate: 0.5%
✓ CPU Usage: 80%
✓ Memory Usage: 60%
```

### Bottleneck thường gặp:

1. **CPU bound** - CPU 100%, cần scale horizontal
2. **Memory bound** - Memory cao, cần tăng limits
3. **Database** - DB slow, cần optimize queries
4. **Network** - Bandwidth limit

## Tips

- Chạy test **nhiều lần** để có kết quả chính xác
- Test vào **giờ thấp điểm** để không ảnh hưởng production
- **Monitor** hệ thống trong khi test
- **Ghi lại** kết quả để so sánh sau này
- Test **từng service riêng** để tìm bottleneck
