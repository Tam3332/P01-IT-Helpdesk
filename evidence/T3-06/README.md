# Bằng chứng T3-06 — Auth (Lê Phú Cường)

Môi trường: máy dev cá nhân (Windows), backend nhánh `cuong/be` chạy `mvnw spring-boot:run`, PostgreSQL 18 trong Docker (cổng 5433), tài khoản demo từ `database/init_demo_users.sql`.
Lưu ý: chạy thử trên máy dev, **chưa chạy trên VM lab**; chưa phải kết quả chính thức của TC-HD-01/TC-HD-03 (test case viết sau). Token JWT trong ảnh đã được che.

## Cấu trúc thư mục

| Thư mục / file | Chứa gì |
|---|---|
| `api/` | Ảnh gọi API trực tiếp (Swagger, PowerShell `Invoke-RestMethod` / `curl.exe`): mã HTTP, JSON trả về |
| `db/` | Ảnh truy vấn PostgreSQL (`docker exec p01-db psql ...`) để kiểm tra dữ liệu được ghi đúng |
| `ui/` | Ảnh giao diện (FE `localhost:5173`): `EV-HD-xx_fe_*` (03–04/11), `EV-HD-xx_ui_<mã ca>_*` (theo `test_giao_dien.md`) |
| `test_giao_dien.md` | Checklist test giao diện theo role (mã ca A–E), cột "Đã phát hiện" (lỗi đã biết, ai xử lý) và "Kết quả" |

Quy ước tên: `EV-HD-<số FR>_<api|db|fe|ui>_<mô tả>.png`. Số FR theo SRS: 01 đăng nhập, 03 tạo ticket, 05 xem ticket, 06 phân công.

| EV-ID | File | Ngày | Task / test | Mô tả | Kết quả quan sát |
|---|---|---|---|---|---|
| EV-HD-01 | api/EV-HD-01_login_200_manager.png | 2026-11-03 | T3-06, TC-HD-01 (dự kiến), SRS UC-01 | POST /api/v1/auth/login, tài khoản manager đúng mật khẩu | 200, `success: true`, có accessToken (đã che), tokenType Bearer, expiresIn 900, user.role IT_MANAGER |
| EV-HD-01 | api/EV-HD-01_login_401_sai_mat_khau.png | 2026-11-03 | T3-06, TC-HD-01 (dự kiến), SRS UC-01 2a | Đăng nhập sai mật khẩu | 401, `error.code: INVALID_CREDENTIALS`, thông báo chung "Email hoặc mật khẩu không đúng" |
| EV-HD-01 | api/EV-HD-01_login_400_validation.png | 2026-11-03 | T3-06, TC-HD-01 (dự kiến) | Bỏ trống email và mật khẩu | 400, `error.code: VALIDATION_FAILED`, details liệt kê từng trường |
| EV-HD-03 | api/EV-HD-03_ticket_400_danh_muc_khong_hop_le.png | 2026-11-03 | T3-06, TC-HD-03 (dự kiến), SRS UC-02 5a | POST /api/v1/tickets với categoryId không tồn tại | 400, `error.code: CATEGORY_NOT_FOUND` |
| (không có EV-ID) | api/T3-06_categories_200_dinh_dang_moi.png | 2026-11-03 | T3-06, kiểm tra định dạng phản hồi SDD §4.2a | GET /api/v1/categories sau khi đổi ApiResponse | 200, `success: true`, 5 danh mục |

**Ghi chú về 5 ảnh trên (ngày 03/11):** chụp từ code chạy trên máy với bản sửa định dạng response chưa push (sau đó thay bằng bản của Trí trên `tri/be`). Định dạng lỗi hiện tại có thêm trường `message` ở ngoài cùng, nên ảnh 400/401 có thể cần chụp lại sau khi chốt SDD §4.2.

## Kiểm thử tích hợp Frontend + Backend (04/11/2026)

Môi trường: máy dev cá nhân (Windows). Backend nhánh `cuong/be` sau khi merge `tri/be` (commit `7490d75`), frontend `src/frontend` chạy `npm run dev` (Vite, cổng 5173, proxy `/api` → `localhost:8080`), PostgreSQL 18 Docker cổng 5433. Tài khoản demo `*@helpdesk.local`. Chưa chạy trên VM lab; chưa phải kết quả chính thức của TC-HD-01/03/05.

| EV-ID | File | Ngày | Task / test | Mô tả | Kết quả quan sát |
|---|---|---|---|---|---|
| EV-HD-01 | ui/EV-HD-01_fe_login_sai_mat_khau.png | 2026-11-04 | T3-06, SRS UC-01 2a | Đăng nhập Employee với mật khẩu sai (mật khẩu thử, không phải mật khẩu thật) | Báo "Email hoặc mật khẩu không đúng.", ở lại trang login |
| EV-HD-01 | ui/EV-HD-01_fe_token_het_han.png | 2026-11-04 | T3-06, SDD §6.1 | Để phiên quá 15 phút (jwt.expiration-ms 900000) | Tự đăng xuất, báo "Phiên đăng nhập đã hết hạn" |
| EV-HD-03 | ui/EV-HD-03_fe_tao_ticket_thanh_cong.png | 2026-11-04 | T3-06, SRS HD03 / UC-02 | Employee tạo ticket (Yêu cầu CNTT khác, mức Cao) | Có HD-2026-0001, trạng thái Mới, chưa phân công |
| EV-HD-05 | ui/EV-HD-05_fe_chi_tiet_ticket_employee.png | 2026-11-04 | T3-06, SRS HD05, BR-HD-03 | Employee mở chi tiết ticket vừa tạo | Hiện đúng thông tin; hạn xử lý = tạo + 8 giờ (Cao). Bình luận/lịch sử/đính kèm báo lỗi do backend chưa có API (trả 500) |
| EV-HD-05 | ui/EV-HD-05_fe_danh_sach_employee.png | 2026-11-04 | T3-06, SRS HD05 | Danh sách ticket của Employee | Chỉ thấy ticket do mình tạo |
| EV-HD-05 | ui/EV-HD-05_fe_danh_sach_staff.png | 2026-11-04 | T3-06, SRS HD05 / UC-04 | IT Staff xem danh sách | Thấy ticket NEW (hàng đợi) dù chưa được giao |
| EV-HD-05 | ui/EV-HD-05_fe_tong_quan_manager.png | 2026-11-04 | T3-06, SRS HD05 | IT Manager xem tổng quan | Thấy toàn bộ ticket (1 đang mở, 1 chưa phân công) |
| EV-HD-05 | ui/EV-HD-05_fe_admin_khong_co_trang_ticket.png | 2026-11-04 | T3-06, SRS HD05 | Admin đăng nhập | **Chưa đạt:** frontend không có trang ticket cho Admin; SRS HD05 yêu cầu Admin xem toàn bộ ở chế độ chỉ đọc. Đã báo Chính |

Chưa kiểm được: Employee A xem ticket của Employee B (cần thêm 1 tài khoản Employee). Đã biết: F5 bị về trang login vì backend chưa có `GET /auth/me` (làm ở B7).

## Kiểm thử API `/auth/me`, `/auth/logout`, thu hồi token (B7, 04/11/2026)

Môi trường như phần trên, backend sau khi thêm B7 (chưa commit lúc chụp). Gọi API bằng PowerShell (`Invoke-RestMethod`, `curl.exe`); token lưu trong biến, không in ra màn hình.

| EV-ID | File | Ngày | Task / test | Mô tả | Kết quả quan sát |
|---|---|---|---|---|---|
| EV-HD-01 | api/EV-HD-01_api_login_luu_token.png | 2026-11-04 | T3-06, SRS UC-01 | Đăng nhập Employee, lưu token vào biến | Có token (chỉ in độ dài 284, không in nội dung) |
| EV-HD-01 | api/EV-HD-01_api_me_200_va_401.png | 2026-11-04 | T3-06, SDD §6.1–6.2 | `GET /auth/me` có token, rồi không có token | 200, đúng user, không trả password_hash; không token → 401 UNAUTHORIZED |
| EV-HD-01 | api/EV-HD-01_api_404_logout_token_thu_hoi_401.png | 2026-11-04 | T3-06, SDD §6.1, SecRS TH-08 | API chưa có (`/tickets/HD-2026-0001/comments`); `POST /auth/logout`; gọi lại `/auth/me` bằng token cũ | 404 RESOURCE_NOT_FOUND (không còn 500); logout 200; token cũ còn hạn nhưng bị từ chối 401 |
| EV-HD-01 | db/EV-HD-01_db_token_blacklist.png | 2026-11-04 | T3-06, ADR-02, IA-HD07 | Truy vấn bảng `token_blacklist` sau logout | 1 dòng: jti, user_id = 1, cột `token` trống (không lưu token gốc), expires_at = lúc login + 15 phút (giờ UTC) |

## Bổ sung: Assign Ticket (HD06/UC-06), `GET /users/it-staff`, phân trang (04/11/2026)

Task bổ sung do Trí giao ngày 03/11 (ngoài danh sách T3-06), làm theo SRS HD06/UC-06, BR-HD-03/05, SDD §3.4/§6.4. Backend trên máy dev, code chưa commit lúc chụp. Ticket test HD-2026-0001 (ban đầu NEW, HIGH); staff id = 2, employee id = 1, manager id = 3.

| EV-ID | File | Ngày | Task / test | Mô tả | Kết quả quan sát |
|---|---|---|---|---|---|
| EV-HD-05 | api/EV-HD-05_api_it_staff_va_phan_trang.png | 2026-11-04 | UC-06 bước 4; SDD §6.2; SRS UC-04 3b | `GET /users/it-staff` bằng Manager rồi Employee; `GET /tickets?page=-1&size=0` | 200 chỉ IT Staff đang hoạt động, không có password_hash; Employee → 403; tham số trang sai → dùng mặc định (page 0, size 20) |
| EV-HD-06 | api/EV-HD-06_api_assign_403_422_200.png | 2026-11-04 | UC-06 4a, 3a, bước 5; 3b (Trí chốt 04/11) | Employee gọi assign; Manager giao cho Employee; giao cho Staff kèm priority CRITICAL; giao lại đúng Staff cũ | 403; 422 INVALID_ASSIGNEE; 200 status ASSIGNED, SLA tính lại theo Critical (tạo + 15 phút / + 4 giờ), firstRespondedAt null (SRS P2); 422 ASSIGNEE_UNCHANGED |
| EV-HD-06 | db/EV-HD-06_db_assignment_history.png | 2026-11-04 | BR-HD-05; SDD §3.4; HD09 | Truy vấn `ticket_assignments`, `ticket_history` sau các ca trên | 1 dòng phân công is_current = true; 2 dòng lịch sử ASSIGN (người phụ trách, priority HIGH → CRITICAL) kèm IP; các ca bị từ chối không để lại dòng nào |

Chưa kiểm: điều phối lại sang Staff khác (cần thêm 1 tài khoản IT Staff); assign ticket REOPENED / Closed (chưa có API đổi trạng thái để tạo dữ liệu).

## Kiểm thử giao diện theo checklist (04/11/2026, chiều)

Theo `test_giao_dien.md`. Backend `cuong/be` sau commit `05cc318` + sửa cột Người xử lý, tìm kiếm/lọc (chưa commit lúc chụp). Ảnh B3 (giới hạn ký tự) chưa đưa vào vì cần chụp lại.

| EV-ID | File | Ca | Kết quả quan sát |
|---|---|---|---|
| EV-HD-01 | ui/EV-HD-01_ui_A1_sai_mat_khau.png, ui/EV-HD-01_ui_A1_sai_email.png | A1 | Sai mật khẩu và email không tồn tại cùng 1 thông báo chung |
| EV-HD-01 | ui/EV-HD-01_ui_A8_het_phien.png | A8 | Quá 15 phút tự đăng xuất, báo hết phiên |
| EV-HD-05 | ui/EV-HD-05_ui_A6_403_manager_tickets.png, ui/EV-HD-05_ui_A6_403_admin_users.png | A6 | Employee gõ URL trang Manager/Admin → trang 403 |
| EV-HD-05 | ui/EV-HD-05_ui_A7_404_trang_khong_ton_tai.png, ui/EV-HD-05_ui_A7_ticket_khong_ton_tai.png | A7 | URL sai → 404; ticket không có → "Không tìm thấy ticket hoặc bạn không có quyền xem" |
| EV-HD-03 | ui/EV-HD-03_ui_B2_*.png (3 ảnh) | B2 | Bỏ trống tiêu đề/danh mục/mô tả (kể cả mô tả chỉ có xuống dòng) → báo lỗi tại ô, không tạo |
| EV-HD-05 | ui/EV-HD-05_ui_B4_danh_sach_employee.png | B1, B4 | HD-2026-0002 tạo thành công (Mới, Nghiêm trọng); đủ cột, cột Người xử lý đúng |
| EV-HD-05 | ui/EV-HD-05_ui_B5_*.png (3 ảnh), ui/EV-HD-05_ui_B6_loc_danh_muc.png | B5, B6 | Tìm "Cà phê" / "trà sữa" ra đúng ticket, từ khóa không có → trống; lọc danh mục đúng |
| EV-HD-05 | ui/EV-HD-05_ui_C1_C2_tong_quan_staff.png | C1, C2 | Staff thấy ticket được giao + Mới; thẻ "Ticket được giao" = 2 (**lỗi FE**: đếm cả ticket Mới) |
| EV-HD-05 | ui/EV-HD-05_ui_C3_staff_xem_ticket_moi.png | C3, B1 | Staff xem ticket Mới không có nút thao tác; **lỗi FE**: vẫn hiện ô bình luận. Hạn xử lý = tạo + 4 giờ (Critical) |
| EV-HD-05 | ui/EV-HD-05_ui_C4_staff_ticket_duoc_giao.png | C4 | Ticket được giao có nút "Bắt đầu xử lý", không có nút Phân công |
| EV-HD-05 | ui/EV-HD-05_ui_D1_tong_quan_manager.png | D1 | Manager thấy toàn bộ ticket (2 đang mở, 1 chưa phân công) |
| EV-HD-06 | ui/EV-HD-06_ui_D2_dropdown_it_staff.png | D2 | Hộp thoại phân công chỉ liệt kê IT Staff đang hoạt động |
| EV-HD-06 | ui/EV-HD-06_ui_D3_sau_phan_cong.png | D3 | HD-2026-0002: Đã phân công, ưu tiên Cao, hạn xử lý = tạo + 8 giờ, toast thành công |
| EV-HD-06 | ui/EV-HD-06_ui_D4_giao_lai_nguoi_cu.png | D4 | Giao lại đúng người cũ, không đổi ưu tiên → FE chặn, báo "Ticket đang giao cho người này…" |
| EV-HD-05 | ui/EV-HD-05_ui_D7_staff_sau_phan_cong.png | D7 | Staff thấy ticket vừa được giao, cột Người xử lý đúng |
| EV-UC-10 | ui/EV-UC-10_ui_E1_tao_danh_muc.png | E1 | Admin tạo danh mục TESTUI, toast thành công |
| EV-UC-10 | ui/EV-UC-10_ui_E1_employee_thay_danh_muc_moi.png | E1 | Employee thấy danh mục mới trong dropdown |
| EV-UC-10 | ui/EV-UC-10_ui_E2_trung_ma.png | E2 | Tạo trùng mã → "Mã danh mục đã tồn tại" |
| EV-UC-10 | ui/EV-UC-10_ui_E3_tat_danh_muc.png | E3 | Tắt TESTUI → trạng thái Tạm ẩn |
| EV-UC-10 | ui/EV-UC-10_ui_E3_employee_khong_con_danh_muc.png | E3 | Employee không còn thấy danh mục đã tắt |
| EV-UC-10 | ui/EV-UC-10_ui_E5_sla_bang_0.png | E5 | SLA = 0 → "Hạn xử lý phải lớn hơn 0." |
| EV-UC-10 | ui/EV-UC-10_ui_E5_sla_am.png | E5 | SLA = -1 → bị chặn |
| EV-UC-10 | ui/EV-UC-10_ui_E6_admin_tong_quan_khong_co_menu_ticket.png | E6 | Menu Admin không có trang ticket chỉ đọc (lỗi FE) |
