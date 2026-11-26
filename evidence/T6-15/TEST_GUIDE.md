# Nghiệm thu WAF trên VM nhóm

Giữ cùng SHA trong hai chế độ; đổi mode tạo run riêng. Ghi HTTP status, request ID, audit đã che, rule/score nếu có, Expected/Actual và ảnh thật theo checklist.csv.

| ID | Cách chạy và bằng chứng |
|---|---|
| WAF-01 | nginx-t trước/sau deploy; 857 rule là lịch sử cá nhân, lượt nhóm ghi số thực. Kiểm file render, module và policy |
| WAF-02 | So manifest snapshot/after: ID,image,start time DB/backend/frontend không đổi; volume/network/host port đúng stack |
| WAF-03 | HTTPS200; TLS1.2/1.3; headers; đăng nhập/API rate limit; Swagger/api-docs/Actuator404. Kiểm không lộ cổng backend/DB |
| WAF-04 | P1: Employee tạo ticket hợp lệ → Manager phân công → Staff xử lý → Employee xem kết quả; status nghiệp vụ đúng |
| WAF-05 | P3 approve: Employee yêu cầu mở lại ticket đủ điều kiện → Manager duyệt → Staff xử lý tiếp. Ghi ticket seed và role |
| WAF-06 | P3 reject: tạo yêu cầu khác → Manager từ chối → đúng trạng thái/hiển thị theo yêu cầu hiện hành |
| WAF-07 | Upload tệp loại cho phép gần 10 MB; JSON, tiếng Việt, ký tự < > & và đoạn code hợp lệ. Ghi byte size/response/rule nếu cảnh báo; không mặc định payload vô hại sẽ qua CRS |
| WAF-08 | Dữ liệu lab một lần: login với marker riêng, Authorization/Cookie; rà audit/access/error/stdout/stderr. Lưu privacy-check và coverage/request ID. File rỗng không PASS |
| WAF-09 | smoke.py ở cả hai mode. XSS/SQLi/traversal/command/scanner cần Actual403 ở On và audit cùng giao dịch chứng minh WAF can thiệp. Không quy RBAC403 thành WAF |
| WAF-10 | UI gửi nội dung bị WAF từ chối: thông báo thân thiện, có request ID, không raw HTML/rule/stack, phiên đăng nhập còn. Chụp màn hình thật |
| WAF-11 | Employee gọi API chỉ Admin được quyền: 403 backend có code/body nguyên bản, không đổi thành GATEWAY_FORBIDDEN. Đối chiếu upstream_status và audit |
| WAF-12 | Lặp WAF-04…07 và lab-check ở On; không có WAF403 trên ca hợp lệ. Đánh riêng ca bị false positive |
| WAF-13 | Hoà nối nguồn WAF với R3; đối chiếu request ID, timestamp, interruption/status, thiếu trường và loại trừ RBAC. Trả alert thật đã che |
| WAF-14 | Stats trước/lúc/sau; đo load hữu hạn; thực tập DetectionOnly fallback hoặc khôi phục snapshot, kiểm HTTPS/P1/P3 và không đổi dữ liệu |
| WAF-15 | Nhi/Khánh xác nhận PL1, inbound5/outbound4, scope, lịch và người chạy. Ghi người/ngày/quyết định, không tự điền Approved |

Exclusion chỉ giới hạn rule + route + parameter sau khi tái hiện ca hợp lệ và review; không tắt toàn CRS hoặc bỏ toàn bộ kiểm body. Nếu chưa kịp xử lý trước 27/11, nhóm quyết định giữ DetectionOnly và ghi risk trong 02 §8. 28/11 review nội bộ, 29/11 nộp Deployment theo kế hoạch.
