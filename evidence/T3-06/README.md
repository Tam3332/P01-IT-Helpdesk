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
