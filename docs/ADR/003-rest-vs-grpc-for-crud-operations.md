# ADR-003: Lựa chọn REST thay vì gRPC cho các thao tác CRUD và API hướng người dùng

---

## Bối cảnh

Mặc dù chúng ta đã chọn gRPC cho các thao tác real-time tần suất cao (cập nhật vị trí tài xế), nhưng cần quyết định giao thức truyền thông cho:

1. **Quản lý người dùng**: Đăng ký, đăng nhập, cập nhật hồ sơ
2. **Quản lý chuyến đi**: Tạo chuyến đi, lấy chi tiết, hủy chuyến, lịch sử
3. **Quản lý tài xế**: Đăng ký tài xế, cập nhật trạng thái, lấy hồ sơ
4. **Thao tác Admin**: Các thao tác CRUD cho quản lý hệ thống
5. **API hướng client**: Ứng dụng mobile và web dashboard

Các thao tác này có đặc điểm:

- **Tần suất thấp hơn**: Requests không thường xuyên (không phải continuous streams)
- **Pattern Request/Response đơn giản**: Một request → một response
- **Dữ liệu dễ đọc**: Có lợi cho debugging và monitoring
- **Hỗ trợ client đa dạng**: Mobile apps, web browsers, tích hợp bên thứ ba
- **Kích thước payload biến đổi**: Từ nhỏ (login) đến trung bình (lịch sử chuyến đi)

### Quy mô & yêu cầu hiệu suất

**Thao tác người dùng:**

- Login: ~10 req/giây (peak: 50 req/giây)
- Đăng ký: ~2 req/giây (peak: 10 req/giây)
- Cập nhật hồ sơ: ~5 req/giây (peak: 20 req/giây)
- Yêu cầu thời gian phản hồi: < 500ms

**Thao tác chuyến đi:**

- Tạo chuyến đi: ~20 req/giây (peak: 100 req/giây)
- Lấy chi tiết: ~30 req/giây (peak: 150 req/giây)
- Lịch sử: ~5 req/giây (peak: 25 req/giây)
- Yêu cầu thời gian phản hồi: < 500ms

**Tổng tải CRUD**: ~100 req/giây (peak: ~400 req/giây)

### Các phương án được xem xét

1. **REST API với JSON**
2. **gRPC với Protocol Buffers**
3. **GraphQL**
4. **SOAP**
5. **Hybrid Approach (REST cho external, gRPC cho internal)**

- Không có native caching support

- `GET /api/trips/{id}` - Lấy chi tiết chuyến đi

- Consistent structure giúp clients dễ dàng parse
- Clear error messages giúp debugging
- Pagination tránh overload server và client

#### 4. **API Versioning**

**URL versioning (UIT-Go sử dụng):**

- `/api/v1/users/login`
- `/api/v2/users/login`
- Đơn giản, rõ ràng, dễ route

**Header versioning (alternative):**

- Same URL, version trong header
- `Accept: application/vnd.uitgo.v1+json`
- Linh hoạt nhưng phức tạp hơn

**Lợi ích:**

- Backward compatibility
- Có thể maintain nhiều versions
- Clients upgrade theo tốc độ của họ

### Spring Boot Implementation Pattern

**Controller Layer:**

- Sử dụng `@RestController` annotation
- `@RequestMapping` cho base path
- Method-specific mappings: `@PostMapping`, `@GetMapping`, v.v.
- Validation với `@Valid`
- Authentication với `@AuthenticationPrincipal`

**Service Layer:**

- Business logic tách biệt khỏi controller
- Transaction management
- Error handling

**Repository Layer:**

- JPA repositories cho database access
- Query methods hoặc custom queries

### OpenFeign cho Inter-Service Communication

**Mục đích:**

- Trip Service gọi Driver Service để tìm nearby drivers
- Trip Service gọi User Service để validate users
- Driver Service gọi Trip Service để accept trip

**Đặc điểm OpenFeign:**

- Declarative HTTP client
- Tự động load balancing
- Error handling và retries
- Integration với Spring Cloud

---

## Hậu quả của quyết định

### Tích cực

1. ✅ **Phát triển đơn giản**: Dễ implement và maintain
2. ✅ **Hỗ trợ phổ quát**: Hoạt động trên browsers, mobile apps, curl, Postman
3. ✅ **Debugging dễ dàng**: JSON dễ đọc trong Network tab
4. ✅ **Tích hợp API Gateway**: Tương thích hoàn hảo với Spring Cloud Gateway
5. ✅ **Hỗ trợ Caching**: HTTP caching native
6. ✅ **Tích hợp bên thứ ba**: Dễ dàng cho partners tích hợp
7. ✅ **Team quen thuộc**: Team đã biết REST/JSON
8. ✅ **Công cụ phong phú**: Swagger, Postman, curl, và nhiều tools khác
9. ✅ **Phù hợp học tập**: Sinh viên dễ học và thực hành

### Tiêu cực

1. ❌ **Chậm hơn gRPC 20%**: 45ms vs 35ms (vẫn trong SLA)
2. ❌ **Payload lớn hơn**: JSON vs Protobuf (không đáng kể cho CRUD)
3. ❌ **Không có Type Safety**: Runtime errors thay vì compile-time
4. ❌ **Serialization thủ công**: Không có auto-generated code

### Giải pháp giảm thiểu

**Performance:**

- Đủ tốt cho CRUD operations (< 500ms SLA)
- Optimize database queries
- Thêm caching nếu cần
- Sử dụng HTTP/2 cho header compression
- Database indexing tốt

**Type Safety:**

- Sử dụng `@Valid` annotation cho input validation
- Định nghĩa DTOs rõ ràng với documentation
- Unit tests cho serialization/deserialization
- Request/Response validation layers

**Large Payloads:**

- Sử dụng gzip compression (tự động trong Spring Boot)
- Pagination cho large responses
- Field filtering nếu cần
- Có thể xem xét GraphQL nếu clients cần field selection

---

## Quyết định Kiến trúc Hybrid

### REST được sử dụng cho:

- ✅ Quản lý người dùng (register, login, profile)
- ✅ Quản lý chuyến đi (CRUD operations)
- ✅ Quản lý tài xế (CRUD operations)
- ✅ Thao tác Admin
- ✅ Client-facing APIs (mobile, web)
- ✅ Tích hợp bên thứ ba

### gRPC được sử dụng cho:

- ✅ Cập nhật vị trí tài xế (high-frequency streaming)
- ✅ Các tính năng real-time trong tương lai (ví dụ: live trip tracking)
- ✅ Internal service-to-service calls (nếu cần trong tương lai)

### Ma trận quyết định:

| Tiêu chí           | REST       | gRPC             |
| ------------------ | ---------- | ---------------- |
| Tần suất request   | < 100/giây | > 1000/giây      |
| Cần streaming      | Không      | Có               |
| Hỗ trợ browser     | Bắt buộc   | Không bắt buộc   |
| Dễ đọc             | Ưu tiên    | Không quan trọng |
| Kích thước payload | < 10 KB    | Bất kỳ           |
| Type safety        | Tốt nếu có | Rất quan trọng   |

**Nguyên tắc chọn lựa:**

- Mặc định: Sử dụng REST
- Chỉ dùng gRPC khi: High frequency + Real-time + Performance critical

---

## Các cân nhắc trong tương lai

### Khi nào nên thêm gRPC cho CRUD

Xem xét chuyển CRUD operations sang gRPC nếu:

#### 1. **Quy mô tăng 10 lần**

**Hiện tại:** ~100 req/giây  
**Ngưỡng:** > 1,000 req/giây  
**Lý do:** Performance gains biện minh cho complexity

**Dấu hiệu:**

- Response time bắt đầu vượt quá SLA
- Database load quá cao
- Network bandwidth trở thành bottleneck
- CPU usage cao do JSON serialization

#### 2. **Mobile Data trở thành vấn đề**

**Hiện tại:** Unlimited data plans phổ biến  
**Ngưỡng:** Nhiều users với limited data plans  
**Lý do:** Payloads nhỏ hơn tiết kiệm mobile data

**Dấu hiệu:**

- User complaints về data usage
- Nhiều users ở emerging markets
- App được dùng nhiều ở nơi có mạng yếu

#### 3. **Type Safety trở nên quan trọng**

**Hiện tại:** Quản lý được với validation  
**Ngưỡng:** Schema changes thường xuyên gây bugs  
**Lý do:** Compile-time safety ngăn runtime errors

**Dấu hiệu:**

- Nhiều bugs do API contract violations
- Khó maintain consistency giữa client và server
- Nhiều teams phát triển độc lập

### Khi nào nên thêm GraphQL

Xem xét GraphQL nếu:

#### 1. **Yêu cầu phức tạp từ Clients**

**Dấu hiệu:**

- Clients cần flexible field selection
- Nhiều mobile app versions với nhu cầu khác nhau
- Over-fetching trở thành vấn đề (fetch quá nhiều data không dùng)
- Under-fetching yêu cầu nhiều requests

#### 2. **Public API cho developers bên ngoài**

**Dấu hiệu:**

- Cần public API cho external developers
- Cần hỗ trợ nhiều use cases khác nhau
- Third-party developers cần flexibility

#### 3. **Mobile performance critical**

**Dấu hiệu:**

- Cần minimize số lượng requests
- Bandwidth optimization quan trọng
- Flexible queries giúp giảm data transfer

---

## Phụ lục: Chi tiết Performance Benchmark

### Môi trường Test

- **Instance**: Local development environment
- **Concurrent users**: 100
- **Duration**: 10 phút
- **Network**: Local Docker network (low latency)

### Kết quả Benchmark

| Thao tác     | REST P50 | REST P99 | gRPC P50 | gRPC P99 |
| ------------ | -------- | -------- | -------- | -------- |
| Login        | 42ms     | 85ms     | 32ms     | 68ms     |
| Register     | 78ms     | 145ms    | 62ms     | 125ms    |
| Get Profile  | 25ms     | 52ms     | 18ms     | 42ms     |
| Create Trip  | 62ms     | 128ms    | 48ms     | 105ms    |
| Get Trip     | 28ms     | 58ms     | 22ms     | 48ms     |
| Cancel Trip  | 45ms     | 92ms     | 35ms     | 75ms     |
| Trip History | 82ms     | 165ms    | 65ms     | 142ms    |

**Giải thích metrics:**

- **P50 (Median)**: 50% requests nhanh hơn giá trị này
- **P99**: 99% requests nhanh hơn giá trị này (worst-case gần như)

**Kết luận**:

- gRPC consistently nhanh hơn 20-25%
- Cả hai đều đáp ứng SLA < 500ms
- Chênh lệch tuyệt đối nhỏ (10-20ms)
- User experience tương đương

---

## Kết luận

Quyết định sử dụng REST cho CRUD operations và gRPC chỉ cho high-frequency streaming là lựa chọn phù hợp vì:

### Lý do chính:

1. **Simplicity > Performance gains**: 20% faster không đáng để trade-off với complexity
2. **Developer experience**: REST dễ học, dễ debug, dễ maintain
3. **Ecosystem**: Rich tooling và universal support
4. **Educational value**: Sinh viên học được REST fundamentals tốt hơn
5. **Pragmatic**: Đáp ứng requirements với least complexity

### Hybrid approach benefits:

- **Best of both worlds**: REST cho CRUD, gRPC cho streaming
- **Right tool for the job**: Mỗi protocol cho use case phù hợp
- **Learning opportunity**: Sinh viên học cả hai technologies
- **Scalable**: Có thể thêm gRPC cho services khác nếu cần
