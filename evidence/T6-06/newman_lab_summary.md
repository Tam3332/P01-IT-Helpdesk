# Kết quả chạy Newman – VM lab (T6-06)

- Thời điểm: 27/11/2026 17:41 · Môi trường: VM lab `helpdesk-security-lab` qua Tailscale (`https://100.124.80.30:8443/api/v1`, đi qua Nginx TLS) · bản đang deploy: v0.6 (theo Nhi, deploy 26/11; commit và chế độ WAF chưa xác nhận)
- Collection: `tests/newman/P01_API.postman_collection.json` (nhánh `cuong/t6-06-tests`) · Lệnh: `newman run P01_API.postman_collection.json -e P01_lab.postman_environment.json --env-var "baseUrl=https://100.124.80.30:8443/api/v1" --env-var "demoPassword=***" --insecure`
- **Requests 65/65 · Assertions 217/217 đạt · 0 lỗi**
- Thời gian phản hồi: trung bình 115 ms, nhỏ nhất 64 ms, lớn nhất 498 ms, p95 353 ms; tổng thời gian chạy 68.8 s (gồm khoảng chờ 7 s giữa các lần đăng nhập để không vượt giới hạn Nginx 10 request/phút cho /auth/login)
- Mọi request đều dưới 2.000 ms (NFR-02, sheet ghi NFR04). Đây là thời gian từng request trong luồng chức năng, không phải phép đo tải; số đo hiệu năng với 1.000 ticket ở phần đo riêng.
- File này được trích từ báo cáo JSON của Newman; đã bỏ token và mật khẩu. Ảnh màn hình: `newman_lab_27-11.png`.


## 01 Đăng nhập và phiên

| # | Ca kiểm thử | Request | HTTP | ms | Assertions |
|---|---|---|---|---|---|
| 1 | Đăng nhập employee | `POST /api/v1/auth/login` | 200 | 498 | 5/5 |
| 2 | Đăng nhập employee2 | `POST /api/v1/auth/login` | 200 | 391 | 3/3 |
| 3 | Đăng nhập IT Staff | `POST /api/v1/auth/login` | 200 | 295 | 3/3 |
| 4 | Đăng nhập IT Staff 2 | `POST /api/v1/auth/login` | 200 | 351 | 3/3 |
| 5 | Đăng nhập IT Manager | `POST /api/v1/auth/login` | 200 | 293 | 3/3 |
| 6 | Đăng nhập Admin | `POST /api/v1/auth/login` | 200 | 353 | 3/3 |
| 7 | Sai mật khẩu → 401 | `POST /api/v1/auth/login` | 401 | 373 | 3/3 |
| 8 | Thiếu email, mật khẩu → 400 | `POST /api/v1/auth/login` | 400 | 66 | 3/3 |
| 9 | Xem thông tin chính mình | `GET /api/v1/auth/me` | 200 | 77 | 4/4 |
| 10 | Không gửi token → 401 | `GET /api/v1/auth/me` | 401 | 64 | 3/3 |
| 11 | Token giả → 401 | `GET /api/v1/auth/me` | 401 | 65 | 3/3 |

## 02 Luồng P1: tạo, phân công, xử lý, đóng

| # | Ca kiểm thử | Request | HTTP | ms | Assertions |
|---|---|---|---|---|---|
| 12 | Lấy danh mục đang hoạt động | `GET /api/v1/categories` | 200 | 75 | 4/4 |
| 13 | Employee tạo ticket | `POST /api/v1/tickets` | 201 | 94 | 4/4 |
| 14 | IT Manager tạo ticket → 403 | `POST /api/v1/tickets` | 403 | 73 | 3/3 |
| 15 | Tạo ticket sai dữ liệu → 400 | `POST /api/v1/tickets` | 400 | 73 | 3/3 |
| 16 | Employee tìm theo mã ticket | `GET /api/v1/tickets?search=HD-2026-0004&size=20` | 200 | 85 | 4/4 |
| 17 | size > 100 bị giới hạn về 100 | `GET /api/v1/tickets?size=500` | 200 | 148 | 4/4 |
| 18 | Employee khác xem ticket → 404 (Anti-IDOR) | `GET /api/v1/tickets/4` | 404 | 76 | 3/3 |
| 19 | IT Manager lấy danh sách IT Staff | `GET /api/v1/users/it-staff` | 200 | 75 | 4/4 |
| 20 | IT Staff phân công → 403 | `PUT /api/v1/tickets/4/assign` | 403 | 67 | 3/3 |
| 21 | IT Manager phân công, nâng CRITICAL | `PUT /api/v1/tickets/4/assign` | 200 | 93 | 4/4 |
| 22 | IT Staff khác xem ticket → 404 | `GET /api/v1/tickets/4` | 404 | 74 | 3/3 |
| 23 | IT Staff được giao xem ticket | `GET /api/v1/tickets/4` | 200 | 78 | 3/3 |
| 24 | IT Staff nhận xử lý (IN_PROGRESS) | `PATCH /api/v1/tickets/4/status` | 200 | 78 | 4/4 |
| 25 | IT Staff ghi chú nội bộ | `POST /api/v1/tickets/4/comments` | 201 | 92 | 4/4 |
| 26 | Employee bình luận | `POST /api/v1/tickets/4/comments` | 201 | 78 | 4/4 |
| 27 | Employee khác bình luận → 404 | `POST /api/v1/tickets/4/comments` | 404 | 73 | 3/3 |
| 28 | Admin bình luận → 403 | `POST /api/v1/tickets/4/comments` | 403 | 90 | 3/3 |
| 29 | Employee không thấy ghi chú nội bộ | `GET /api/v1/tickets/4/comments` | 200 | 87 | 4/4 |
| 30 | Employee tải ảnh PNG | `POST /api/v1/tickets/4/attachments` | 201 | 187 | 4/4 |
| 31 | Tải tệp .exe → 400 | `POST /api/v1/tickets/4/attachments` | 400 | 173 | 3/3 |
| 32 | Admin tải tệp → 403 | `POST /api/v1/tickets/4/attachments` | 403 | 135 | 3/3 |
| 33 | Hoàn tất thiếu giải pháp → 400 | `PATCH /api/v1/tickets/4/status` | 400 | 70 | 3/3 |
| 34 | IT Staff hoàn tất (RESOLVED) | `PATCH /api/v1/tickets/4/status` | 200 | 78 | 4/4 |
| 35 | Employee khác đóng ticket → 404 | `POST /api/v1/tickets/4/close` | 404 | 76 | 3/3 |
| 36 | Employee đóng ticket | `POST /api/v1/tickets/4/close` | 200 | 74 | 4/4 |
| 37 | Bình luận ticket đã đóng → 422 | `POST /api/v1/tickets/4/comments` | 422 | 76 | 3/3 |
| 38 | Employee đánh giá 5 sao | `POST /api/v1/tickets/4/feedback` | 201 | 87 | 3/3 |
| 39 | Đánh giá lần 2 → 422 | `POST /api/v1/tickets/4/feedback` | 422 | 71 | 3/3 |
| 40 | Lịch sử ticket đủ vòng đời | `GET /api/v1/tickets/4/history` | 200 | 104 | 4/4 |

## 03 Luồng P3: yêu cầu mở lại và duyệt

| # | Ca kiểm thử | Request | HTTP | ms | Assertions |
|---|---|---|---|---|---|
| 41 | Employee tạo ticket 2 | `POST /api/v1/tickets` | 201 | 110 | 3/3 |
| 42 | Phân công ticket 2 | `PUT /api/v1/tickets/5/assign` | 200 | 81 | 3/3 |
| 43 | Nhận xử lý ticket 2 | `PATCH /api/v1/tickets/5/status` | 200 | 78 | 3/3 |
| 44 | Hoàn tất ticket 2 | `PATCH /api/v1/tickets/5/status` | 200 | 105 | 3/3 |
| 45 | Lý do mở lại quá ngắn → 400 | `POST /api/v1/tickets/5/reopen` | 400 | 80 | 3/3 |
| 46 | Employee yêu cầu mở lại | `POST /api/v1/tickets/5/reopen` | 201 | 81 | 4/4 |
| 47 | Gửi lại khi đang chờ duyệt → 422 | `POST /api/v1/tickets/5/reopen` | 422 | 70 | 3/3 |
| 48 | Đóng ticket đang chờ duyệt → 422 | `POST /api/v1/tickets/5/close` | 422 | 82 | 3/3 |
| 49 | IT Staff xem yêu cầu mở lại → 403 | `GET /api/v1/reopen-requests` | 403 | 68 | 3/3 |
| 50 | IT Manager xem yêu cầu chờ duyệt | `GET /api/v1/reopen-requests?status=PENDING` | 200 | 67 | 4/4 |
| 51 | IT Manager duyệt mở lại | `POST /api/v1/reopen-requests/3/approve` | 200 | 85 | 4/4 |
| 52 | Ticket 2 ở trạng thái REOPENED | `GET /api/v1/tickets/5` | 200 | 73 | 4/4 |

## 04 Quản trị, SLA, dashboard

| # | Ca kiểm thử | Request | HTTP | ms | Assertions |
|---|---|---|---|---|---|
| 53 | Employee vào /admin/users → 403 | `GET /api/v1/admin/users` | 403 | 76 | 3/3 |
| 54 | IT Staff lấy danh sách IT Staff → 403 | `GET /api/v1/users/it-staff` | 403 | 66 | 3/3 |
| 55 | IT Manager xem cấu hình SLA | `GET /api/v1/admin/sla-configs` | 200 | 72 | 4/4 |
| 56 | IT Manager sửa SLA → 403 | `PUT /api/v1/admin/sla-configs/HIGH` | 403 | 70 | 3/3 |
| 57 | Admin đặt SLA = 0 giờ → 400 | `PUT /api/v1/admin/sla-configs/HIGH` | 400 | 87 | 3/3 |
| 58 | Admin xem mức SLA không tồn tại → 404 | `GET /api/v1/admin/sla-configs/URGENT` | 404 | 96 | 3/3 |
| 59 | Admin xem audit log | `GET /api/v1/admin/audit-logs` | 200 | 137 | 3/3 |
| 60 | Dashboard: Employee | `GET /api/v1/dashboard/stats` | 200 | 77 | 3/3 |
| 61 | Dashboard: IT Staff | `GET /api/v1/dashboard/stats` | 200 | 73 | 3/3 |
| 62 | Dashboard: IT Manager | `GET /api/v1/dashboard/stats` | 200 | 76 | 3/3 |
| 63 | Dashboard: Admin | `GET /api/v1/dashboard/stats` | 200 | 78 | 3/3 |

## 05 Đăng xuất

| # | Ca kiểm thử | Request | HTTP | ms | Assertions |
|---|---|---|---|---|---|
| 64 | employee2 đăng xuất | `POST /api/v1/auth/logout` | 200 | 75 | 3/3 |
| 65 | Token đã đăng xuất → 401 | `GET /api/v1/auth/me` | 401 | 67 | 3/3 |
