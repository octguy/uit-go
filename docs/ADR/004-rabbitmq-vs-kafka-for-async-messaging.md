# ADR-004: Chọn RabbitMQ cho Thông báo Chuyến đi đến tài xế

## Bối cảnh

Nền tảng UIT-Go yêu cầu thông báo chuyến đi mới cho tài xế gần khu vực pickup theo thời gian thực. Khi hành khách tạo chuyến đi, hệ thống cần tìm tài xế gần nhất (thông qua Redis GEORADIUS) và gửi thông báo cho họ để họ có thể chấp nhận hoặc từ chối chuyến đi trong khoảng thời gian giới hạn (15 giây).

### Yêu cầu

1. **Tách biệt service**: Trip Service và Driver Service không phụ thuộc trực tiếp vào nhau
2. **Xử lý bất đồng bộ**: Trip creation không bị block bởi quá trình gửi thông báo
3. **Độ tin cậy cao**: Messages không được mất trong quá trình truyền
4. **Retry mechanism**: Tự động retry khi xử lý message thất bại
5. **Đơn giản và dễ maintain**: Phù hợp cho môi trường học tập và demo
6. **Monitoring**: Có thể theo dõi trạng thái messages và queues

### Quy mô hiện tại

```
Chuyến đi mới: 100-500 chuyến/giờ (hiện tại) → 2,000 chuyến/giờ (6 tháng)
Message rate: ~0.1-0.5 msg/s (hiện tại) → 2-5 msg/s (6 tháng)
Nearby drivers/trip: 1 tài xế
Message size: ~300-500 bytes (JSON)
Timeout: 15 giây cho mỗi thông báo
```

### Các phương án được xem xét

1. **RabbitMQ với AMQP Protocol**
2. **Apache Kafka với Event Streaming**
3. **Redis Pub/Sub**
4. **Amazon SQS (Simple Queue Service)**
5. **REST API đồng bộ** (baseline comparison)

---

## Quyết định

**Nhóm em chọn RabbitMQ với AMQP Protocol** để xử lý thông báo chuyến đi bất đồng bộ từ Trip Service đến Driver Service.

---

## Lý do lựa chọn

### Ưu điểm của RabbitMQ

#### 1. **Mô hình Exchange-Queue-Consumer trực quan**

RabbitMQ cung cấp mô hình routing linh hoạt với Exchange và Queue:

**Kiến trúc trong UIT-Go:**

```
Trip Service → RabbitTemplate → trip.exchange (TopicExchange)
                                       ↓
                                (routing key: trip.notification)
                                       ↓
                              trip.notification.queue (durable)
                                       ↓
                                @RabbitListener
                                       ↓
                              Driver Service
```

**Lợi ích:**

- **Routing linh hoạt**: Topic Exchange cho phép routing phức tạp trong tương lai
- **Decoupling hoàn toàn**: Publisher không cần biết về consumer
- **Multiple consumers**: Có thể thêm nhiều listeners nếu cần
- **Dead Letter Queue**: Tự động chuyển failed messages sang DLQ

#### 2. **Đảm bảo Message Delivery**

**So sánh cơ chế đảm bảo delivery:**

| Tính năng              | RabbitMQ          | Redis Pub/Sub  | REST API      |
| ---------------------- | ----------------- | -------------- | ------------- |
| **Message Durability** | ✅ Durable queues | ❌ No persist  | N/A           |
| **ACK/NACK**           | ✅ Manual ACK     | ❌ Fire-forget | ❌ No retry   |
| **Retry Mechanism**    | ✅ Auto retry     | ❌ No retry    | ❌ Manual     |
| **Dead Letter Queue**  | ✅ Built-in       | ❌ None        | ❌ None       |
| **Ordering**           | ✅ FIFO per queue | ⚠️ Best effort | ✅ Sequential |

**Configuration trong dự án:**

```java
@Bean
public Queue tripNotificationQueue() {
    return new Queue(tripNotificationQueue, true); // durable = true
}
```

**Tại sao quan trọng:**

- Nếu Driver Service restart, messages vẫn được giữ trong queue
- Nếu processing thất bại, message được retry tự động
- Không mất thông báo chuyến đi quan trọng

#### 3. **Tích hợp xuất sắc với Spring Boot**

**Spring AMQP cung cấp:**

- **Auto-configuration**: Minimal setup trong application.yml
- **RabbitTemplate**: Dễ dàng publish messages
- **@RabbitListener**: Annotation-based consumer
- **Jackson Integration**: Auto JSON serialization/deserialization
- **Error Handling**: Built-in retry và error handling

**Ví dụ code trong dự án:**

**Publisher (Trip Service):**

```java
@Service
public class TripNotificationServiceImpl {
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.trip}")
    private String tripExchange;

    public void notifyNearbyDrivers(TripNotificationRequest notification) {
        rabbitTemplate.convertAndSend(tripExchange, tripNotificationRoutingKey, notification);
        log.info("Trip notification published successfully");
    }
}
```

**Consumer (Driver Service):**

```java
@Component
public class TripNotificationListener {

    @RabbitListener(queues = "${rabbitmq.queue.trip-notification}")
    public void handleTripNotification(TripNotificationRequest notification) {
        tripNotificationService.handleTripNotification(notification);
        log.info("Successfully processed trip notification: {}", notification.getTripId());
    }
}
```

**Chỉ với vài dòng code**, có được:

- Automatic connection management
- Auto JSON deserialization
- Retry on failure
- Graceful error handling

#### 4. **Management UI mạnh mẽ**

RabbitMQ Management Plugin cung cấp web UI tại http://localhost:15672:

**Monitoring features:**

- **Overview**: Message rates, queue statistics, connection info
- **Queues**: Xem messages trong queue, purge queue, get message details
- **Exchanges**: Xem bindings và routing configuration
- **Connections**: Monitor active connections từ services
- **Manual testing**: Publish test messages trực tiếp từ UI

**Trong quá trình development:**

- Debug message flow một cách visual
- Verify messages được publish đúng format
- Monitor queue buildup nếu consumer chậm
- Test retry mechanism bằng cách tắt consumer

**Ví dụ screenshot (conceptual):**

```
Queue: trip.notification.queue
Messages Ready: 5
Messages Unacknowledged: 2
Message Rate (in/out): 0.5 msg/s / 0.4 msg/s
Consumers: 1
```

#### 5. **Dễ triển khai với Docker**

**Docker Compose configuration:**

```yaml
rabbitmq:
  image: rabbitmq:3.13-management-alpine
  container_name: rabbitmq
  ports:
    - "5672:5672" # AMQP port
    - "15672:15672" # Management UI
  environment:
    RABBITMQ_DEFAULT_USER: guest
    RABBITMQ_DEFAULT_PASS: guest
  volumes:
    - rabbitmq-data:/var/lib/rabbitmq
  healthcheck:
    test: ["CMD", "rabbitmq-diagnostics", "ping"]
    interval: 10s
    timeout: 5s
    retries: 5
```

**Lợi ích:**

- **Single command**: `docker-compose up` để start tất cả
- **Lightweight**: Alpine image chỉ ~40 MB
- **Persistent storage**: Messages được lưu trong volume
- **Health check**: Đảm bảo RabbitMQ ready trước khi services connect
- **Phù hợp laptop sinh viên**: RAM usage < 200 MB

---

### Tại sao không chọn Kafka?

#### 1. **Quá phức tạp cho use case đơn giản**

Kafka được thiết kế cho **high-throughput event streaming**, không phải traditional messaging:

**Kafka yêu cầu:**

- Hiểu về partitions, consumer groups, offset management
- ZooKeeper hoặc KRaft mode cho metadata management
- Topic replication và leader election
- Consumer offset tracking và commit strategies

**RabbitMQ chỉ cần:**

- Exchange và Queue (concepts đơn giản)
- Consumer nhận message và ACK
- Retry tự động khi fail

**Ví dụ so sánh code:**

**RabbitMQ:**

```java
@RabbitListener(queues = "trip.notification.queue")
public void handleTripNotification(TripNotificationRequest notification) {
    // Process notification
}
```

**Kafka (equivalent):**

```java
@KafkaListener(topics = "trip.notification", groupId = "driver-service")
public void handleTripNotification(ConsumerRecord<String, TripNotificationRequest> record) {
    // Must handle offset management
    // Must consider partition assignment
    // Must handle rebalancing
}
```

#### 2. **Resource usage cao hơn nhiều**

**So sánh resource consumption** (single instance, idle state):

| Metric       | RabbitMQ        | Kafka            |
| ------------ | --------------- | ---------------- |
| RAM Usage    | ~150-200 MB     | ~500-1000 MB     |
| Disk I/O     | Low (in-memory) | High (log-based) |
| Startup Time | 5-10 giây       | 20-30 giây       |
| CPU Usage    | 1-2%            | 5-10%            |

**Tại sao Kafka nặng hơn:**

- **Log-based storage**: Mọi message được write to disk
- **Partition management**: Overhead từ partition replication
- **ZooKeeper**: Thêm dependency cho cluster coordination (hoặc KRaft)
- **Consumer group coordination**: Protocol phức tạp cho rebalancing

**Trong môi trường sinh viên:**

- Laptop thường có 8-16 GB RAM
- Docker đã dùng RAM cho Postgres, Redis, Spring apps
- RabbitMQ phù hợp hơn cho resource-constrained environment

#### 3. **Không cần tính năng của Kafka**

**Kafka strengths** (mà UIT-Go không cần):

**Event Replay:**

- Kafka giữ messages trong days/weeks, có thể replay từ offset bất kỳ
- UIT-Go: Thông báo chỉ có giá trị trong 15 giây, không cần replay

**High Throughput:**

- Kafka: 100,000+ msg/s per partition
- UIT-Go: ~0.5 msg/s hiện tại, ~5 msg/s trong tương lai (Kafka overkill)

**Stream Processing:**

- Kafka Streams cho real-time analytics và transformations
- UIT-Go: Chỉ cần simple point-to-point messaging

**Multiple Consumers với Independent Offsets:**

- Kafka: Mỗi consumer group có offset riêng, có thể consume cùng message
- UIT-Go: Chỉ cần Driver Service consume messages một lần

#### 4. **Operational complexity**

**Kafka vận hành:**

- Monitor partition leaders và replica status
- Handle partition rebalancing
- Manage consumer lag per partition
- Configure retention policies
- ZooKeeper maintenance (hoặc KRaft cluster)

**RabbitMQ vận hành:**

- Check queue stats trong Management UI
- Monitor message rates
- Purge queue nếu cần
- That's it!

**Học tập:**

- Sinh viên dành nhiều thời gian học Kafka concepts
- Thay vì tập trung vào business logic
- RabbitMQ đơn giản hơn, học nhanh hơn

---

### Tại sao không chọn Redis Pub/Sub?

#### 1. **Thiếu Message Persistence**

**Redis Pub/Sub:**

- Messages chỉ ở trong RAM
- Nếu subscriber không online, message bị mất
- Không có queue để giữ messages

**Vấn đề với UIT-Go:**

```
1. Trip Service publish thông báo chuyến đi
2. Driver Service đang restart (deployment)
3. Message bị mất → Tài xế không nhận được thông báo
4. Chuyến đi không có tài xế nhận
```

**RabbitMQ:**

```
1. Trip Service publish thông báo chuyến đi
2. Driver Service đang restart
3. Message được giữ trong durable queue
4. Driver Service restart xong, nhận message từ queue
5. Thông báo được xử lý thành công ✅
```

#### 2. **Không có ACK/Retry mechanism**

**Redis Pub/Sub là fire-and-forget:**

- Publisher gửi message
- Nếu subscriber nhận được → OK
- Nếu subscriber không nhận được → Message mất, không retry

**RabbitMQ với ACK:**

```java
@RabbitListener(queues = "trip.notification.queue")
public void handleTripNotification(TripNotificationRequest notification) {
    try {
        tripNotificationService.handleTripNotification(notification);
        // Auto ACK khi method return successfully
    } catch (Exception e) {
        log.error("Error processing notification", e);
        throw e; // Re-throw → RabbitMQ retry
    }
}
```

**Khi có lỗi:**

- RabbitMQ tự động retry message (configurable số lần retry)
- Sau khi hết retries → chuyển vào Dead Letter Queue
- Admin có thể xem failed messages và tìm nguyên nhân

#### 3. **Không phù hợp cho critical notifications**

**Trip notifications là critical:**

- Hành khách đang chờ tài xế
- Nếu thông báo bị mất → No driver accepts → Bad UX
- Cần đảm bảo delivery

**Redis Pub/Sub phù hợp cho:**

- Real-time analytics (có thể mất một số data points)
- Cache invalidation notifications
- Live updates (không critical nếu miss)

**RabbitMQ phù hợp cho:**

- Business-critical messages ✅
- Cần guarantee delivery ✅
- Cần retry và error handling ✅

---

### Tại sao không chọn Amazon SQS?

#### 1. **Cloud dependency**

**SQS:**

- Chỉ available trên AWS
- Cần AWS account và credentials
- Không thể chạy local cho development

**Vấn đề cho sinh viên:**

- Không thể develop offline
- Phải setup AWS account (credit card required)
- Khác biệt giữa local dev và cloud deployment
- Khó debug vì không có local instance

**RabbitMQ:**

- Chạy local với Docker
- Develop offline hoàn toàn
- Consistent environment từ dev đến production

#### 2. **Cost considerations**

**SQS pricing** (as of 2024):

- First 1 million requests/month: Free
- After that: $0.40 per million requests
- Data transfer costs

**Tính toán cho UIT-Go:**

```
2,000 trips/hour × 24 hours × 30 days = 1,440,000 messages/month
Cost: Free tier covers (1M) + 440,000 × $0.40/million ≈ $0.18/month
```

**Mặc dù rẻ**, nhưng:

- Phải quản lý billing
- Phải monitor usage để avoid overages
- Phải setup IAM permissions correctly

**RabbitMQ: $0** (chỉ infrastructure cost)

#### 3. **Khác biệt trong learning experience**

**Sinh viên học với SQS:**

- Học AWS-specific concepts
- Không áp dụng được cho on-premise deployments
- Phụ thuộc vào cloud provider

**Sinh viên học với RabbitMQ:**

- Học standard AMQP protocol
- Áp dụng được cho bất kỳ môi trường nào
- Hiểu message broker fundamentals
- Có thể migrate sang bất kỳ AMQP broker nào

---

### Tại sao không dùng REST API đồng bộ?

**Synchronous approach:**

```java
// Trip Service
public TripResponse createTrip(TripRequest request) {
    TripResponse trip = tripRepository.save(trip);

    // Synchronous call to Driver Service
    driverClient.notifyNearbyDrivers(notification); // BLOCKING

    return trip;
}
```

**Vấn đề:**

1. **Coupling**: Trip Service phụ thuộc vào Driver Service availability
2. **Blocking**: Trip creation bị block cho đến khi notification sent
3. **Failure propagation**: Nếu Driver Service down → Trip creation fails
4. **Latency**: User phải đợi cả trip creation + notification

**Asynchronous với RabbitMQ:**

```java
// Trip Service
public TripResponse createTrip(TripRequest request) {
    TripResponse trip = tripRepository.save(trip);

    // Async publish to RabbitMQ (non-blocking)
    rabbitTemplate.convertAndSend(exchange, routingKey, notification);

    return trip; // Return immediately
}
```

**Lợi ích:**

1. **Decoupling**: Services độc lập hoàn toàn
2. **Non-blocking**: Trip creation return ngay lập tức
3. **Resilience**: Driver Service down không affect Trip Service
4. **Better UX**: User không phải đợi notification process

---

## Chi tiết triển khai

### Mô hình dữ liệu Message

**TripNotificationRequest (Publisher):**

```java
@Data
@Builder
public class TripNotificationRequest {
    private UUID tripId;
    private UUID passengerId;
    private String passengerName;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private Double destinationLatitude;
    private Double destinationLongitude;
    private Double estimatedFare;
    private Double distanceKm;
    private String requestedAt;
    private List<String> nearbyDriverIds; // Usually 1 driver
}
```

**Message size**: ~300-500 bytes (JSON format)

**JSON serialization sử dụng Jackson2JsonMessageConverter:**

```java
@Bean
public MessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
}
```

### Flow xử lý thông báo chuyến đi

**End-to-end flow:**

```
1. Passenger creates trip
   → POST /api/trips/create

2. Trip Service:
   a. Validate request
   b. Save trip to PostgreSQL
   c. Query nearby drivers (OpenFeign → Driver Service → Redis GEORADIUS)
   d. Build TripNotificationRequest
   e. Publish to RabbitMQ (non-blocking)
   f. Return TripResponse to user immediately

3. RabbitMQ:
   a. Receive message from Trip Service
   b. Route to trip.notification.queue via trip.exchange
   c. Store message in durable queue

4. Driver Service:
   a. @RabbitListener receives message
   b. Deserialize JSON to TripNotificationRequest
   c. Store in Redis with TTL = 15 seconds:
      Key: "pending_trips:<driverId>:<tripId>"
      Value: PendingTripNotification object
   d. ACK message to RabbitMQ

5. Driver polls notifications:
   → GET /api/drivers/{driverId}/notifications
   → Returns list from Redis

6. Driver accepts trip:
   → POST /api/drivers/notifications/{tripId}/accept
   → Update trip status in Trip Service
   → Remove from Redis
```

---

## Hệ quả

### Tích cực

1. ✅ **Decoupling services**: Trip và Driver Service hoàn toàn độc lập
2. ✅ **Non-blocking**: Trip creation không bị delay bởi notification
3. ✅ **Reliable delivery**: Messages được đảm bảo delivery với ACK/NACK
4. ✅ **Auto retry**: Failed messages được retry tự động
5. ✅ **Simple codebase**: Minimal code với Spring AMQP
6. ✅ **Easy debugging**: Management UI cho visual monitoring
7. ✅ **Dễ học**: Concepts đơn giản, phù hợp sinh viên
8. ✅ **Low resource**: < 200 MB RAM, phù hợp laptop
9. ✅ **Production-ready**: RabbitMQ được dùng rộng rãi trong industry
10. ✅ **Scalable**: Đủ capacity cho growth (2,000+ msg/s)

### Tiêu cực

1. ❌ **Thêm component**: Phải maintain thêm RabbitMQ server
2. ❌ **Network dependency**: Cần RabbitMQ available để publish messages
3. ❌ **Eventual consistency**: Notification không instant (delay ~10-50ms)
4. ❌ **Debugging phức tạp hơn**: Async flow khó debug hơn sync
5. ❌ **Learning curve**: Sinh viên cần học messaging concepts

### Biện pháp giảm thiểu

#### Thêm component

**Giải pháp:**

- Docker Compose tự động start RabbitMQ cùng các services
- Healthcheck đảm bảo RabbitMQ ready trước khi services connect
- Minimal maintenance needed (stateless, self-healing)

```yaml
healthcheck:
  test: ["CMD", "rabbitmq-diagnostics", "ping"]
  interval: 10s
  timeout: 5s
  retries: 5
```

#### Network dependency

**Graceful degradation:**

```java
try {
    rabbitTemplate.convertAndSend(exchange, routingKey, notification);
} catch (AmqpException e) {
    log.error("RabbitMQ unavailable, trip creation still succeeds");
    // Optionally: Store in database for later retry
    // Or: Send via alternative channel (e.g., direct REST call)
}
```

**Auto-reconnect:**

- Spring AMQP tự động reconnect khi RabbitMQ available trở lại
- Messages được buffer trong memory (có limit)

#### Eventual consistency

**Acceptable tradeoff:**

- Notification delay ~10-50ms (human không nhận ra)
- Trade-off worth it cho decoupling và reliability
- Tài xế có 15 giây để accept, delay 50ms không ảnh hưởng UX

#### Debugging complexity

**Tools hỗ trợ:**

- **Management UI**: Visual view of messages trong queue
- **Detailed logging**: Log ở cả publisher và consumer
- **Message tracing**: Enable tracing plugin nếu cần
- **Local testing**: Có thể publish test messages từ UI

#### Learning curve

**Educational value:**

- Sinh viên học industry-standard messaging patterns
- Concepts áp dụng cho bất kỳ message broker nào
- Hands-on experience với async communication
- Resume value: RabbitMQ experience

---

## Xem xét lại các phương án

Nhóm em sẽ **xem xét lại quyết định này** nếu:

1. **Message volume tăng 100x** (> 500 msg/s)
   - Giải pháp: RabbitMQ clustering hoặc cân nhắc Kafka
2. **Cần event replay** cho auditing hoặc analytics
   - Giải pháp: Migrate sang Kafka hoặc add event sourcing layer
3. **Cần stream processing** (real-time aggregations, windowing)
   - Giải pháp: Kafka Streams hoặc Apache Flink
4. **Multi-region deployment** với complex routing
   - Giải pháp: Evaluate federated RabbitMQ hoặc cloud-native solutions

**Nhưng hiện tại**: RabbitMQ là perfect fit cho requirements của UIT-Go.

---

## Tích hợp với hệ thống

### Dependencies giữa services

**Service dependency flow:**

```
Passenger → API Gateway → Trip Service
                              ↓
                       (1) Query nearby drivers
                              ↓
                    OpenFeign → Driver Service → Redis (GEORADIUS)
                              ↓
                       (2) Publish notification
                              ↓
                         RabbitMQ
                              ↓
                       (3) Consume message
                              ↓
                       Driver Service → Redis (Store pending trip)
```

**RabbitMQ position trong architecture:**

- Sits between Trip Service và Driver Service
- Async communication layer
- Decouples synchronous dependencies

**Management UI metrics:**

- **Queue depth**: Monitor queue buildup
- **Message rates**: In/out rates per second
- **Consumer count**: Verify consumers are active
- **Unacked messages**: Detect processing issues

**Alerting thresholds:**

- Queue depth > 1000 → Consumer too slow
- Message rate out = 0 → Consumer down
- Unacked messages > 100 → Processing errors

---

## Phụ lục: So sánh chi tiết

### Bảng so sánh toàn diện

| Tiêu chí               | RabbitMQ          | Kafka            | Redis Pub/Sub     | SQS               | REST Sync      |
| ---------------------- | ----------------- | ---------------- | ----------------- | ----------------- | -------------- |
| **Setup Complexity**   | ⭐⭐⭐⭐⭐ Rất dễ | ⭐⭐ Phức tạp    | ⭐⭐⭐⭐⭐ Rất dễ | ⭐⭐⭐ Trung bình | ⭐⭐⭐⭐⭐ Dễ  |
| **Message Durability** | ✅ Durable queues | ✅ Log-based     | ❌ No persist     | ✅ SQS storage    | N/A            |
| **Delivery Guarantee** | ✅ At-least-once  | ✅ Configurable  | ❌ Fire-forget    | ✅ At-least-once  | ⚠️ Best-effort |
| **Retry Mechanism**    | ✅ Auto retry     | ⚠️ Manual        | ❌ None           | ✅ Auto retry     | ❌ Manual      |
| **Throughput**         | 20K msg/s         | 100K+ msg/s      | 100K+ msg/s       | 10K msg/s         | 1K req/s       |
| **Latency**            | 10-50ms           | 5-20ms           | 1-5ms             | 50-100ms          | 20-50ms        |
| **RAM Usage**          | 150-200 MB        | 500-1000 MB      | 50-100 MB         | N/A (cloud)       | Minimal        |
| **Learning Curve**     | ⭐⭐⭐⭐ Dễ       | ⭐⭐ Khó         | ⭐⭐⭐⭐⭐ Rất dễ | ⭐⭐⭐ Trung bình | ⭐⭐⭐⭐⭐ Dễ  |
| **Management UI**      | ✅ Excellent      | ⚠️ Basic         | ❌ None           | ✅ AWS Console    | N/A            |
| **Spring Integration** | ✅ Xuất sắc       | ✅ Tốt           | ⭐⭐⭐ OK         | ✅ Tốt            | ✅ Native      |
| **Local Development**  | ✅ Docker         | ✅ Docker        | ✅ Docker         | ❌ Cloud only     | ✅ Native      |
| **Production Use**     | ✅ Wide adoption  | ✅ Wide adoption | ⚠️ Limited        | ✅ AWS ecosystem  | ✅ Standard    |

### Kết luận So sánh

**RabbitMQ wins vì:**

1. **Perfect balance**: Đủ features, không quá phức tạp
2. **Right scale**: Throughput đủ cho current + future needs
3. **Developer experience**: Excellent Spring integration + Management UI
4. **Educational value**: Sinh viên học industry-standard tool
5. **Reliability**: Message guarantees phù hợp cho notifications
6. **Resource-efficient**: Low memory footprint
7. **Easy operations**: Simple deployment và monitoring
