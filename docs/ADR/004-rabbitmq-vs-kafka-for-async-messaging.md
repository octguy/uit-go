### Tại sao không chọn Kafka?

# ADR-004: RabbitMQ cho Async Messaging (rút gọn)

**Trạng thái**: Đã chấp nhận
**Ngày**: 25/11/2025
**Người quyết định**: Nhóm phát triển UIT-Go
**Tags**: #messaging #rabbitmq #async

---

## Tóm tắt quyết định

Chọn RabbitMQ làm hệ thống messaging cho các luồng bất đồng bộ như thông báo chuyến đi. RabbitMQ cung cấp đủ tính năng (durable queue, ACK/NACK, DLQ, routing) cho nhu cầu hiện tại và dễ triển khai cho môi trường học tập.

---

## Lý do chính (ngắn)

- Mô hình Exchange → Queue → Consumer trực quan và phù hợp cho notifications.
- Hỗ trợ đảm bảo delivery (durable queues, ACK/NACK), retry và DLQ.
- Tích hợp tốt với Spring Boot và có management UI thuận tiện cho phát triển.
- Triển khai nhẹ nhàng trên Docker, phù hợp cho laptop sinh viên.

---

## Alternatives (rút gọn)

- Kafka: Mạnh cho throughput, retention và replay nhưng quá phức tạp và resource-heavy so với nhu cầu notifications.
- Redis Pub/Sub: Nhẹ nhưng thiếu persistence và ACK.
- Amazon SQS: Cloud-dependent, khác trải nghiệm local cho sinh viên.

---

## Hậu quả chính

- Pros: decoupling giữa services, reliable delivery, dễ phát triển local.
- Cons: thêm component cần vận hành, eventual consistency, cần theo dõi broker health.

---

## Khi nào xem lại

- Khi cần throughput rất lớn, long-term retention, replay hoặc stream processing → cân nhắc Kafka.

---

**Cập nhật lần cuối**: 25/11/2025

- Call Driver Service REST API
- Redis GEORADIUS query
- Lấy 5-10 nearby drivers

3. **Publish Notification:**

   - Tạo TripNotificationRequest object
   - Include: trip info + nearby driver IDs
   - RabbitTemplate.convertAndSend()
   - Trip Service continues (không đợi)

4. **RabbitMQ Routing:**

   - Message đến trip.exchange
   - Route theo routing key
   - Queue trong trip.notification.queue

5. **Driver Service Consumes:**

   - @RabbitListener trigger
   - Deserialize JSON → TripNotificationRequest
   - Lưu vào in-memory Map<driverId, List<Notification>>
   - Set TTL = 3 phút

6. **Driver Views Notifications:**

   - Driver calls GET /api/drivers/notifications
   - Lấy từ in-memory store
   - Display list of available trips

7. **Driver Accepts:**
   - Driver calls POST /api/drivers/notifications/{tripId}/accept
   - Call Trip Service để update trip
   - Remove notification khỏi store

### Configuration trong UIT-Go

**RabbitMQ Server (Docker):**

- Image: rabbitmq:3.13-management-alpine
- AMQP Port: 5672
- Management UI: 15672
- Default user: guest/guest

**Spring Configuration:**

- Exchange: trip.exchange (TopicExchange)
- Queue: trip.notification.queue (durable=true)
- Routing Key: trip.notification
- Message Converter: Jackson2JsonMessageConverter

**Connection:**

- Host: rabbitmq (Docker service name)
- Port: 5672
- Auto-reconnect enabled
- Connection pooling

### Error Handling

**Publisher side (Trip Service):**

- Try-catch khi publish
- Log errors
- Không fail trip creation nếu publish fail
- Graceful degradation

**Consumer side (Driver Service):**

- Try-catch trong listener
- Log processing errors
- Re-throw exception → RabbitMQ retry
- Dead Letter Queue cho failed messages

**Retry mechanism:**

- Default: 3 retries
- Backoff: exponential
- Max interval: 10 seconds
- After retries → DLQ

---

## Hậu quả của quyết định

### Tích cực

1. ✅ **Decoupling services**: Trip và Driver Service độc lập
2. ✅ **Async processing**: Non-blocking, better performance
3. ✅ **Reliability**: Messages không bị mất, auto-retry
4. ✅ **Simple setup**: Docker container, easy configuration
5. ✅ **Dễ học**: Clear concepts, good documentation
6. ✅ **Management UI**: Visual monitoring và debugging
7. ✅ **Spring integration**: Excellent support, minimal code
8. ✅ **Scalable**: Đủ cho current và future needs
9. ✅ **Educational value**: Sinh viên học messaging patterns
10. ✅ **Flexible routing**: Có thể mở rộng cho other use cases

### Tiêu cực

1. ❌ **Additional component**: Thêm RabbitMQ vào infrastructure
2. ❌ **Network dependency**: Cần RabbitMQ server running
3. ❌ **Eventual consistency**: Notification không instant (vài ms delay)
4. ❌ **Debugging complexity**: Async harder to debug than sync
5. ❌ **Learning curve**: Sinh viên cần học messaging concepts

### Giải pháp giảm thiểu

**Additional component:**

- Docker Compose tự động start RabbitMQ
- Healthcheck đảm bảo service ready
- Minimal maintenance needed

**Network dependency:**

- Services có graceful degradation
- Trip creation không fail nếu RabbitMQ down
- Auto-reconnect khi RabbitMQ available

**Eventual consistency:**

- Acceptable cho notification use case
- Delay < 1 giây, user không nhận ra
- Trade-off worth it cho decoupling

**Debugging:**

- Management UI hiển thị messages
- Detailed logging ở cả publisher và consumer
- Message tracing enabled

**Learning curve:**

- Good educational opportunity
- Clear documentation trong project
- Simple use case giúp học nhanh
- Management UI visual learning

---

## Metrics và Monitoring

### RabbitMQ Management UI

**Accessible at**: http://localhost:15672

**Monitoring:**

- **Overview**: Server stats, message rates
- **Connections**: Active connections từ services
- **Channels**: Communication channels
- **Exchanges**: trip.exchange configuration
- **Queues**: trip.notification.queue stats
  - Messages ready
  - Messages unacknowledged
  - Message rate (in/out)
  - Consumer count

**Useful features:**

- Publish test messages
- View message contents
- Purge queues
- Export/import definitions

### Application Metrics

**Publisher (Trip Service):**

- Messages published/second
- Publish success rate
- Publish errors
- Average publish latency

**Consumer (Driver Service):**

- Messages consumed/second
- Processing success rate
- Processing errors
- Average processing time
- Retry count

**Health Checks:**

- RabbitMQ connection status
- Queue health
- Consumer status

---

## Các cân nhắc trong tương lai

### Khi nào cần scale RabbitMQ?

**Dấu hiệu cần scale:**

1. **Message rate quá cao:**

   - Current capacity: 4,000-20,000 msg/s
   - Nếu exceed 15,000 msg/s → cần scale

2. **Queue buildup:**

   - Messages accumulate trong queue
   - Consumer không kịp xử lý
   - Need more consumers hoặc better processing

3. **Latency tăng:**
   - Message delivery > 1 giây
   - Processing time tăng

**Scaling options:**

**Horizontal scaling:**

- Multiple RabbitMQ instances
- Clustering cho high availability
- Load balancing

**Vertical scaling:**

- Increase memory/CPU
- Faster disk I/O
- Better network

**Consumer scaling:**

- Multiple Driver Service instances
- Concurrent consumers
- Better load distribution

### Khi nào cần migrate sang Kafka?

Consider Kafka nếu:

1. **Volume tăng 100x:**

   - Current: ~400 msg/s (peak)
   - Threshold: > 40,000 msg/s
   - Kafka better cho scale này

2. **Cần Event Sourcing:**

   - Store all events long-term
   - Replay events
   - Audit trail

3. **Stream Processing:**

   - Real-time analytics
   - Complex event processing
   - Kafka Streams, ksqlDB

4. **Multiple consumers với different offsets:**
   - Different services cần replay messages
   - Independent consumption rates

**Nhưng hiện tại:**

- RabbitMQ đáp ứng tốt requirements
- Không cần complexity của Kafka
- Keep it simple

### Mở rộng use cases

**RabbitMQ có thể dùng cho:**

1. **Order notifications:**

   - Trip completed → notify passenger
   - Rating reminders
   - Promotional messages

2. **Analytics events:**

   - Trip events → Analytics Service
   - User behavior tracking
   - Business intelligence

3. **Email/SMS queue:**

   - Async email sending
   - SMS notifications
   - Push notifications

4. **Batch processing:**
   - Daily reports
   - Data aggregation
   - Scheduled tasks

**Pattern:**

- Multiple queues cho different purposes
- Same RabbitMQ instance
- Cost-effective scaling

---

## Bài học kinh nghiệm

### Cho sinh viên

**1. Hiểu Async Messaging:**

- Tại sao cần async communication
- Decoupling benefits
- Trade-offs (eventual consistency)
- Real-world applications

**2. Message Broker patterns:**

- Publisher-Subscriber pattern
- Point-to-Point messaging
- Request-Reply pattern
- Routing và filtering

**3. Reliability concepts:**

- Message durability
- Acknowledgments
- Retries và error handling
- Dead Letter Queues

**4. Hands-on experience:**

- Setup RabbitMQ với Docker
- Publish và consume messages
- Monitor với Management UI
- Debug message flow

### Cho dự án

**1. Start simple:**

- RabbitMQ đủ cho most use cases
- Không cần Kafka unless proven necessary
- Measure before scaling

**2. Design cho reliability:**

- Durable queues
- Proper ACK handling
- Error handling và retries
- Monitoring và alerting

**3. Documentation:**

- Document message formats
- Routing rules
- Error scenarios
- Monitoring procedures

**4. Testing:**

- Unit tests cho publishers/consumers
- Integration tests với RabbitMQ
- Failure scenarios testing
- Performance testing

---

## Kết luận

Quyết định sử dụng RabbitMQ cho async messaging trong UIT-Go là lựa chọn đúng đắn vì:

### Lý do chính:

1. **Perfect fit cho use case**: Simple notification pattern
2. **Right scale**: Đủ capacity cho current và future needs
3. **Developer friendly**: Easy setup, great Spring integration
4. **Educational value**: Sinh viên học messaging fundamentals
5. **Reliable**: Message guarantees, retry mechanisms
6. **Cost-effective**: Single instance đủ, low resources
7. **Operational simplicity**: Docker deployment, Management UI

### So với alternatives:

- **vs Kafka**: Simpler, lighter, sufficient capacity
- **vs Redis Pub/Sub**: More reliable, better features
- **vs SQS**: No cloud dependency, better for learning
- **vs REST sync**: Better decoupling, reliability

### Thông điệp chính:

> "Choose the simplest tool that meets your requirements. RabbitMQ is perfect for traditional messaging patterns at moderate scale."

### Educational outcome:

Sinh viên học được:

- Async communication patterns
- Message broker concepts
- Reliability và error handling
- Microservices best practices
- Industry-standard tools (AMQP, RabbitMQ)

---

## Tài liệu tham khảo

- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
- [Spring AMQP Documentation](https://spring.io/projects/spring-amqp)
- [RabbitMQ Tutorials](https://www.rabbitmq.com/getstarted.html)
- [AMQP Protocol Specification](https://www.amqp.org/)
- [RabbitMQ Best Practices](https://www.cloudamqp.com/blog/part1-rabbitmq-best-practice.html)

---

**Cập nhật lần cuối**: 25/11/2025  
**Ngày review tiếp theo**: 01/03/2026
