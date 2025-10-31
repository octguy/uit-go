2. Bối cảnh & Yêu cầu Nghiệp vụ
UIT-Go là một ứng dụng cho phép hành khách đặt xe và kết nối với các tài xế ở gần. Hệ thống
cần xử lý các luồng nghiệp vụ cơ bản sau:
● Hành khách:
○ User Story 1: Là một người dùng mới, tôi muốn có thể đăng ký tài khoản bằng email
và mật khẩu để sử dụng ứng dụng.
○ User Story 2: Là một hành khách, tôi muốn nhập điểm đi và điểm đến để có thể yêu
cầu một chuyến xe và xem trước giá cước ước tính.
○ User Story 3: Trong lúc chờ xe, tôi muốn thấy được vị trí của tài xế đang di chuyển
trên bản đồ theo thời gian thực để biết khi nào họ sẽ tới.
○ User Story 4: Là một hành khách, tôi muốn có thể hủy chuyến đi nếu có việc đột xuất.
○ User Story 5: Sau khi chuyến đi kết thúc, tôi muốn có thể đánh giá tài xế (từ 1-5 sao)
và để lại bình luận để phản hồi về chất lượng dịch vụ.
● Tài xế:
○ User Story 1: Là một tài xế, tôi muốn có thể đăng ký thông tin cá nhân và phương tiện
để được xét duyệt tham gia hệ thống.
○ User Story 2: Khi bắt đầu ca làm việc, tôi muốn bật trạng thái "Sẵn sàng" (Online) để
bắt đầu nhận các yêu cầu chuyến đi mới.
○ User Story 3: Khi có yêu cầu chuyến đi ở gần, tôi muốn nhận được thông báo và có 15
giây để quyết định chấp nhận hay từ chối.
○ User Story 4: Trong suốt quá trình di chuyển đến điểm đón và chở khách, vị trí của tôi
cần được cập nhật liên tục về hệ thống.
○ User Story 5: Khi đến nơi, tôi muốn có thể bấm nút "Hoàn thành" để kết thúc chuyến
đi và hệ thống ghi nhận doanh thu.
3. Giai đoạn 1: "Bộ Xương" Microservices (Bắt buộc
cho tất cả các nhóm)
Mục tiêu của giai đoạn này là xây dựng nền tảng vững chắc cho UIT-Go, nơi các service có thể
giao tiếp với nhau một cách tin cậy.
3.1. Kiến trúc yêu cầu
Mỗi nhóm cần xây dựng và triển khai ít nhất 3 microservices cơ bản sau:
1. UserService:
○ Trách nhiệm: Quản lý thông tin người dùng (hành khách và tài xế), xử lý đăng ký,
đăng nhập và hồ sơ.
○ API gợi ý: POST /users, POST /sessions (login), GET /users/me.
○ Công nghệ gợi ý: Node.js/Go/Python, PostgreSQL/MySQL (RDS).
2. TripService:
○ Trách nhiệm: Dịch vụ trung tâm, xử lý logic tạo chuyến đi, quản lý các trạng thái của
chuyến (đang tìm tài xế, đã chấp nhận, đang diễn ra, hoàn thành, đã hủy).
○ API gợi ý: POST /trips, GET /trips/{id}, POST /trips/{id}/cancel.
○ Công nghệ gợi ý: Node.js/Go/Python, PostgreSQL/MongoDB (RDS/DocumentDB).
3.2. Yêu cầu kỹ thuật cho "Bộ Xương"
● Giao tiếp: Các service giao tiếp với nhau qua API. Hãy phân tích và lựa chọn giữa RESTful
(đơn giản, phổ biến) và gRPC (hiệu năng cao, phù hợp cho giao tiếp nội bộ).
● Containerization: Toàn bộ các service phải được đóng gói bằng Docker và có thể chạy
độc lập thông qua Docker Compose trên môi trường local.
● Cơ sở dữ liệu: Mỗi service phải có cơ sở dữ liệu riêng, tuân thủ nguyên tắc "Database per
Service" để đảm bảo sự độc lập và khả năng mở rộng.
● Infrastructure as Code (IaC): Sử dụng Terraform để định nghĩa và triển khai các tài
nguyên hạ tầng cơ bản (VPC, Subnets, Security Groups, IAM Roles, Databases). Việc này
đảm bảo hạ tầng của bạn có thể được tạo lại một cách nhất quán và được quản lý phiên
bản như code.
● Triển khai: Các container được triển khai trên AWS. Hãy cân nhắc giữa việc tự quản lý
trên EC2 (linh hoạt tối đa) và sử dụng các dịch vụ điều phối như ECS/EKS (giảm gánh
nặng vận hành).
