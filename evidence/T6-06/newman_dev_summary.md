# Kết quả chạy Newman – máy dev (T6-06)

- Thời điểm: 26/11/2026 09:31 · Môi trường: dev (`http://localhost:8080/api/v1`), nhánh `cuong/t6-06` (614ae92)
- Collection: `tests/newman/P01_API.postman_collection.json` · Lệnh: `newman run P01_API.postman_collection.json -e P01_dev.postman_environment.json --env-var "demoPassword=***"`
- **Requests 65/65 · Assertions 217/217 đạt · 0 lỗi**
- Thời gian phản hồi: trung bình 42 ms, nhỏ nhất 3 ms, lớn nhất 295 ms, p95 285 ms; tổng thời gian chạy 8.1 s
- File này được trích từ báo cáo JSON của Newman; đã bỏ token và mật khẩu.


## 01 Đăng nhập và phiên

| # | Ca kiểm thử | Request | HTTP | ms | Assertions |
|---|---|---|---|---|---|
| 1 | Đăng nhập employee | `POST /api/v1/auth/login` | 200 | 290 | 5/5 |
| 2 | Đăng nhập employee2 | `POST /api/v1/auth/login` | 200 | 258 | 3/3 |
| 3 | Đăng nhập IT Staff | `POST /api/v1/auth/login` | 200 | 285 | 3/3 |
| 4 | Đăng nhập IT Staff 2 | `POST /api/v1/auth/login` | 200 | 266 | 3/3 |
| 5 | Đăng nhập IT Manager | `POST /api/v1/auth/login` | 200 | 289 | 3/3 |
| 6 | Đăng nhập Admin | `POST /api/v1/auth/login` | 200 | 295 | 3/3 |
| 7 | Sai mật khẩu → 401 | `POST /api/v1/auth/login` | 401 | 263 | 3/3 |
| 8 | Thiếu email, mật khẩu → 400 | `POST /api/v1/auth/login` | 400 | 3 | 3/3 |
| 9 | Xem thông tin chính mình | `GET /api/v1/auth/me` | 200 | 10 | 4/4 |
| 10 | Không gửi token → 401 | `GET /api/v1/auth/me` | 401 | 3 | 3/3 |
| 11 | Token giả → 401 | `GET /api/v1/auth/me` | 401 | 3 | 3/3 |

## 02 Luồng P1: tạo, phân công, xử lý, đóng

| # | Ca kiểm thử | Request | HTTP | ms | Assertions |
|---|---|---|---|---|---|
| 12 | Lấy danh mục đang hoạt động | `GET /api/v1/categories` | 200 | 9 | 4/4 |
| 13 | Employee tạo ticket | `POST /api/v1/tickets` | 201 | 21 | 4/4 |
| 14 | IT Manager tạo ticket → 403 | `POST /api/v1/tickets` | 403 | 8 | 3/3 |
| 15 | Tạo ticket sai dữ liệu → 400 | `POST /api/v1/tickets` | 400 | 8 | 3/3 |
| 16 | Employee tìm theo mã ticket | `GET /api/v1/tickets?search=HD-2026-0012&size=20` | 200 | 17 | 4/4 |
| 17 | size > 100 bị giới hạn về 100 | `GET /api/v1/tickets?size=500` | 200 | 12 | 4/4 |
| 18 | Employee khác xem ticket → 404 (Anti-IDOR) | `GET /api/v1/tickets/12` | 404 | 11 | 3/3 |
| 19 | IT Manager lấy danh sách IT Staff | `GET /api/v1/users/it-staff` | 200 | 9 | 4/4 |
| 20 | IT Staff phân công → 403 | `PUT /api/v1/tickets/12/assign` | 403 | 8 | 3/3 |
| 21 | IT Manager phân công, nâng CRITICAL | `PUT /api/v1/tickets/12/assign` | 200 | 23 | 4/4 |
| 22 | IT Staff khác xem ticket → 404 | `GET /api/v1/tickets/12` | 404 | 10 | 3/3 |
| 23 | IT Staff được giao xem ticket | `GET /api/v1/tickets/12` | 200 | 14 | 3/3 |
| 24 | IT Staff nhận xử lý (IN_PROGRESS) | `PATCH /api/v1/tickets/12/status` | 200 | 26 | 4/4 |
| 25 | IT Staff ghi chú nội bộ | `POST /api/v1/tickets/12/comments` | 201 | 19 | 4/4 |
| 26 | Employee bình luận | `POST /api/v1/tickets/12/comments` | 201 | 26 | 4/4 |
| 27 | Employee khác bình luận → 404 | `POST /api/v1/tickets/12/comments` | 404 | 13 | 3/3 |
| 28 | Admin bình luận → 403 | `POST /api/v1/tickets/12/comments` | 403 | 13 | 3/3 |
| 29 | Employee không thấy ghi chú nội bộ | `GET /api/v1/tickets/12/comments` | 200 | 15 | 4/4 |
| 30 | Employee tải ảnh PNG | `POST /api/v1/tickets/12/attachments` | 201 | 30 | 4/4 |
| 31 | Tải tệp .exe → 400 | `POST /api/v1/tickets/12/attachments` | 400 | 18 | 3/3 |
| 32 | Admin tải tệp → 403 | `POST /api/v1/tickets/12/attachments` | 403 | 7 | 3/3 |
| 33 | Hoàn tất thiếu giải pháp → 400 | `PATCH /api/v1/tickets/12/status` | 400 | 10 | 3/3 |
| 34 | IT Staff hoàn tất (RESOLVED) | `PATCH /api/v1/tickets/12/status` | 200 | 16 | 4/4 |
| 35 | Employee khác đóng ticket → 404 | `POST /api/v1/tickets/12/close` | 404 | 9 | 3/3 |
| 36 | Employee đóng ticket | `POST /api/v1/tickets/12/close` | 200 | 16 | 4/4 |
| 37 | Bình luận ticket đã đóng → 422 | `POST /api/v1/tickets/12/comments` | 422 | 11 | 3/3 |
| 38 | Employee đánh giá 5 sao | `POST /api/v1/tickets/12/feedback` | 201 | 15 | 3/3 |
| 39 | Đánh giá lần 2 → 422 | `POST /api/v1/tickets/12/feedback` | 422 | 10 | 3/3 |
| 40 | Lịch sử ticket đủ vòng đời | `GET /api/v1/tickets/12/history` | 200 | 12 | 4/4 |

## 03 Luồng P3: yêu cầu mở lại và duyệt

| # | Ca kiểm thử | Request | HTTP | ms | Assertions |
|---|---|---|---|---|---|
| 41 | Employee tạo ticket 2 | `POST /api/v1/tickets` | 201 | 21 | 3/3 |
| 42 | Phân công ticket 2 | `PUT /api/v1/tickets/13/assign` | 200 | 19 | 3/3 |
| 43 | Nhận xử lý ticket 2 | `PATCH /api/v1/tickets/13/status` | 200 | 15 | 3/3 |
| 44 | Hoàn tất ticket 2 | `PATCH /api/v1/tickets/13/status` | 200 | 15 | 3/3 |
| 45 | Lý do mở lại quá ngắn → 400 | `POST /api/v1/tickets/13/reopen` | 400 | 7 | 3/3 |
| 46 | Employee yêu cầu mở lại | `POST /api/v1/tickets/13/reopen` | 201 | 18 | 4/4 |
| 47 | Gửi lại khi đang chờ duyệt → 422 | `POST /api/v1/tickets/13/reopen` | 422 | 10 | 3/3 |
| 48 | Đóng ticket đang chờ duyệt → 422 | `POST /api/v1/tickets/13/close` | 422 | 10 | 3/3 |
| 49 | IT Staff xem yêu cầu mở lại → 403 | `GET /api/v1/reopen-requests` | 403 | 12 | 3/3 |
| 50 | IT Manager xem yêu cầu chờ duyệt | `GET /api/v1/reopen-requests?status=PENDING` | 200 | 16 | 4/4 |
| 51 | IT Manager duyệt mở lại | `POST /api/v1/reopen-requests/7/approve` | 200 | 28 | 4/4 |
| 52 | Ticket 2 ở trạng thái REOPENED | `GET /api/v1/tickets/13` | 200 | 12 | 4/4 |

## 04 Quản trị, SLA, dashboard

| # | Ca kiểm thử | Request | HTTP | ms | Assertions |
|---|---|---|---|---|---|
| 53 | Employee vào /admin/users → 403 | `GET /api/v1/admin/users` | 403 | 10 | 3/3 |
| 54 | IT Staff lấy danh sách IT Staff → 403 | `GET /api/v1/users/it-staff` | 403 | 9 | 3/3 |
| 55 | IT Manager xem cấu hình SLA | `GET /api/v1/admin/sla-configs` | 200 | 11 | 4/4 |
| 56 | IT Manager sửa SLA → 403 | `PUT /api/v1/admin/sla-configs/HIGH` | 403 | 8 | 3/3 |
| 57 | Admin đặt SLA = 0 giờ → 400 | `PUT /api/v1/admin/sla-configs/HIGH` | 400 | 9 | 3/3 |
| 58 | Admin xem mức SLA không tồn tại → 404 | `GET /api/v1/admin/sla-configs/URGENT` | 404 | 11 | 3/3 |
| 59 | Admin xem audit log | `GET /api/v1/admin/audit-logs` | 200 | 22 | 3/3 |
| 60 | Dashboard: Employee | `GET /api/v1/dashboard/stats` | 200 | 16 | 3/3 |
| 61 | Dashboard: IT Staff | `GET /api/v1/dashboard/stats` | 200 | 14 | 3/3 |
| 62 | Dashboard: IT Manager | `GET /api/v1/dashboard/stats` | 200 | 14 | 3/3 |
| 63 | Dashboard: Admin | `GET /api/v1/dashboard/stats` | 200 | 42 | 3/3 |

## 05 Đăng xuất

| # | Ca kiểm thử | Request | HTTP | ms | Assertions |
|---|---|---|---|---|---|
| 64 | employee2 đăng xuất | `POST /api/v1/auth/logout` | 200 | 14 | 3/3 |
| 65 | Token đã đăng xuất → 401 | `GET /api/v1/auth/me` | 401 | 4 | 3/3 |
