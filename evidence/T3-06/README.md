# Bằng chứng T3-06 — Auth (Lê Phú Cường)

Môi trường: máy dev cá nhân (Windows), backend nhánh `cuong/be` chạy `mvnw spring-boot:run`, PostgreSQL 18 trong Docker (cổng 5433), tài khoản demo từ `database/init_demo_users.sql`.
Lưu ý: chạy thử trên máy dev, **chưa chạy trên VM lab**; chưa phải kết quả chính thức của TC-HD-01/TC-HD-03 (test case viết sau). Token JWT trong ảnh đã được che.

| EV-ID | File | Ngày | Task / test | Mô tả | Kết quả quan sát |
|---|---|---|---|---|---|
| EV-HD-01 | EV-HD-01_login_200_manager.png | 2026-11-03 | T3-06, TC-HD-01 (dự kiến), SRS UC-01 | POST /api/v1/auth/login, tài khoản manager đúng mật khẩu | 200, `success: true`, có accessToken (đã che), tokenType Bearer, expiresIn 900, user.role IT_MANAGER |
| EV-HD-01 | EV-HD-01_login_401_sai_mat_khau.png | 2026-11-03 | T3-06, TC-HD-01 (dự kiến), SRS UC-01 2a | Đăng nhập sai mật khẩu | 401, `error.code: INVALID_CREDENTIALS`, thông báo chung "Email hoặc mật khẩu không đúng" |
| EV-HD-01 | EV-HD-01_login_400_validation.png | 2026-11-03 | T3-06, TC-HD-01 (dự kiến) | Bỏ trống email và mật khẩu | 400, `error.code: VALIDATION_FAILED`, details liệt kê từng trường |
| EV-HD-03 | EV-HD-03_ticket_400_danh_muc_khong_hop_le.png | 2026-11-03 | T3-06, TC-HD-03 (dự kiến), SRS UC-02 5a | POST /api/v1/tickets với categoryId không tồn tại | 400, `error.code: CATEGORY_NOT_FOUND` |
| (không có EV-ID) | T3-06_categories_200_dinh_dang_moi.png | 2026-11-03 | T3-06, kiểm tra định dạng phản hồi SDD §4.2a | GET /api/v1/categories sau khi đổi ApiResponse | 200, `success: true`, 5 danh mục |

**Ghi chú về 5 ảnh trên (ngày 03/11):** chụp từ code chạy trên máy với bản sửa định dạng response chưa push (sau đó thay bằng bản của Trí trên `tri/be`). Định dạng lỗi hiện tại có thêm trường `message` ở ngoài cùng, nên ảnh 400/401 có thể cần chụp lại sau khi chốt SDD §4.2.

## Kiểm thử tích hợp Frontend + Backend (04/11/2026)

Môi trường: máy dev cá nhân (Windows). Backend nhánh `cuong/be` sau khi merge `tri/be` (commit `7490d75`), frontend `src/frontend` chạy `npm run dev` (Vite, cổng 5173, proxy `/api` → `localhost:8080`), PostgreSQL 18 Docker cổng 5433. Tài khoản demo `*@helpdesk.local`. Chưa chạy trên VM lab; chưa phải kết quả chính thức của TC-HD-01/03/05.

| EV-ID | File | Ngày | Task / test | Mô tả | Kết quả quan sát |
|---|---|---|---|---|---|
| EV-HD-01 | EV-HD-01_fe_login_sai_mat_khau.png | 2026-11-04 | T3-06, SRS UC-01 2a | Đăng nhập Employee với mật khẩu sai (mật khẩu thử, không phải mật khẩu thật) | Báo "Email hoặc mật khẩu không đúng.", ở lại trang login |
| EV-HD-01 | EV-HD-01_fe_token_het_han.png | 2026-11-04 | T3-06, SDD §6.1 | Để phiên quá 15 phút (jwt.expiration-ms 900000) | Tự đăng xuất, báo "Phiên đăng nhập đã hết hạn" |
| EV-HD-03 | EV-HD-03_fe_tao_ticket_thanh_cong.png | 2026-11-04 | T3-06, SRS HD03 / UC-02 | Employee tạo ticket (Yêu cầu CNTT khác, mức Cao) | Có HD-2026-0001, trạng thái Mới, chưa phân công |
| EV-HD-05 | EV-HD-05_fe_chi_tiet_ticket_employee.png | 2026-11-04 | T3-06, SRS HD05, BR-HD-03 | Employee mở chi tiết ticket vừa tạo | Hiện đúng thông tin; hạn xử lý = tạo + 8 giờ (Cao). Bình luận/lịch sử/đính kèm báo lỗi do backend chưa có API (trả 500) |
| EV-HD-05 | EV-HD-05_fe_danh_sach_employee.png | 2026-11-04 | T3-06, SRS HD05 | Danh sách ticket của Employee | Chỉ thấy ticket do mình tạo |
| EV-HD-05 | EV-HD-05_fe_danh_sach_staff.png | 2026-11-04 | T3-06, SRS HD05 / UC-04 | IT Staff xem danh sách | Thấy ticket NEW (hàng đợi) dù chưa được giao |
| EV-HD-05 | EV-HD-05_fe_tong_quan_manager.png | 2026-11-04 | T3-06, SRS HD05 | IT Manager xem tổng quan | Thấy toàn bộ ticket (1 đang mở, 1 chưa phân công) |
| EV-HD-05 | EV-HD-05_fe_admin_khong_co_trang_ticket.png | 2026-11-04 | T3-06, SRS HD05 | Admin đăng nhập | **Chưa đạt:** frontend không có trang ticket cho Admin; SRS HD05 yêu cầu Admin xem toàn bộ ở chế độ chỉ đọc. Đã báo Chính |

Chưa kiểm được: Employee A xem ticket của Employee B (cần thêm 1 tài khoản Employee). Đã biết: F5 bị về trang login vì backend chưa có `GET /auth/me` (làm ở B7).

## Kiểm thử API `/auth/me`, `/auth/logout`, thu hồi token (B7, 04/11/2026)

Môi trường như phần trên, backend sau khi thêm B7 (chưa commit lúc chụp). Gọi API bằng PowerShell (`Invoke-RestMethod`, `curl.exe`); token lưu trong biến, không in ra màn hình.

| EV-ID | File | Ngày | Task / test | Mô tả | Kết quả quan sát |
|---|---|---|---|---|---|
| EV-HD-01 | EV-HD-01_api_login_luu_token.png | 2026-11-04 | T3-06, SRS UC-01 | Đăng nhập Employee, lưu token vào biến | Có token (chỉ in độ dài 284, không in nội dung) |
| EV-HD-01 | EV-HD-01_api_me_200_va_401.png | 2026-11-04 | T3-06, SDD §6.1–6.2 | `GET /auth/me` có token, rồi không có token | 200, đúng user, không trả password_hash; không token → 401 UNAUTHORIZED |
| EV-HD-01 | EV-HD-01_api_404_logout_token_thu_hoi_401.png | 2026-11-04 | T3-06, SDD §6.1, SecRS TH-08 | API chưa có (`/tickets/HD-2026-0001/comments`); `POST /auth/logout`; gọi lại `/auth/me` bằng token cũ | 404 RESOURCE_NOT_FOUND (không còn 500); logout 200; token cũ còn hạn nhưng bị từ chối 401 |
| EV-HD-01 | EV-HD-01_db_token_blacklist.png | 2026-11-04 | T3-06, ADR-02, IA-HD07 | Truy vấn bảng `token_blacklist` sau logout | 1 dòng: jti, user_id = 1, cột `token` trống (không lưu token gốc), expires_at = lúc login + 15 phút (giờ UTC) |
