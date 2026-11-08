# T3-07 — Evidence: FE Login + Ticket

**Người thực hiện:** Nguyễn Nhật Chính (SE196524) · **Nhánh:** `frontend`
**Commit liên quan:** `71941dd`, `fded96a`, `2421a18` (Staff không thấy ticket NEW), `8baaa67` (gộp `tri/be`)
**Liên kết:** HD01, HD03, HD05; NFR-03; SCR-01…04 · **Đầu ra:** FE login + SCR-02/03/04 tích hợp API; ảnh màn hình theo role

## 1. Môi trường chụp

- Ngày chụp: 08/11/2026 (khoảng 22:00–22:30). FE chạy `npm run dev` tại `http://localhost:5173`, Backend nhánh `tri/be` (`f8f8ef1`, 06/11) với dữ liệu demo.
- Trình duyệt Chrome ở cửa sổ ẩn danh. Tài khoản dùng: `employee2`, `itstaff2`, `itmanager2`, `sysadmin2` (@helpdesk.local).
- Ảnh có chứa token đã được **tô đen** (Session storage). Dải dấu trang của trình duyệt đã được cắt khỏi ảnh. Không có thay đổi nào khác lên nội dung ảnh.
- Thư mục ảnh: `evidence/T3-07/ui/<nhóm>/2026-11-08/`. Ảnh test ngày 04/11 của bên test nằm ở `evidence/T3-06/ui/` và được dẫn lại ở cột "Bổ sung" (không sao chép).

## 2. Yêu cầu → cài đặt → bằng chứng

| # | Yêu cầu | Cài đặt (`src/frontend/src/`) | Bằng chứng (08/11) | Bổ sung (04/11, `T3-06`) |
|---|---|---|---|---|
| 1 | SCR-01 Login, gọi `/auth/login` | `pages/auth/LoginPage.jsx`, `services/authService` | [trang đăng nhập](ui/A_General/2026-11-08/EV-HD-01_ui_A_trang_dang_nhap.png) · [sai mật khẩu](ui/A_General/2026-11-08/EV-HD-01_ui_A1_sai_mat_khau.png) | [sai email](../T3-06/ui/A_General/2026-11-04/EV-HD-01_ui_A1_sai_email.png) |
| 2 | Kiểm tra form: bỏ trống | `LoginPage.jsx` (báo lỗi tại từng ô, không gọi API) | [bỏ trống cả hai](ui/A_General/2026-11-08/EV-HD-01_ui_A2_bo_trong_ca_hai.png) | |
| 3 | Lưu token ở sessionStorage, không in ra console | `context/AuthContext.jsx`; `grep console.` chỉ có 1 dòng `console.error` ở `notificationService.js` (không in token) | [Console sạch sau đăng nhập](ui/A_General/2026-11-08/EV-HD-01_ui_A3_console_khong_in_token.png) · [Session storage có `hd_token` (đã che)](ui/A_General/2026-11-08/EV-HD-01_ui_A3_session_storage_token_da_che.png) · [Local storage trống](ui/A_General/2026-11-08/EV-HD-01_ui_A3_local_storage_trong.png) | |
| 4 | Tự logout khi nhận 401 | `services/apiClient.js` (`emitUnauthorized()` khi 401, trừ `/auth/login`); `AuthContext.jsx` | [về login kèm "Phiên đăng nhập đã hết hạn", Session storage đã xoá](ui/A_General/2026-11-08/EV-HD-01_ui_A8_het_phien_401.png) | [A8](../T3-06/ui/A_General/2026-11-04/EV-HD-01_ui_A8_het_phien.png) |
| 5 | Route guard theo role; 403 | `routes/ProtectedRoute.jsx`, `AppRoutes.jsx`, `ForbiddenPage` | [Employee mở `/admin/users`](ui/A_General/2026-11-08/EV-HD-05_ui_A6_403_employee_admin_users.png) · [Manager mở `/employee/tickets`](ui/A_General/2026-11-08/EV-HD-05_ui_A6_403_manager_employee_tickets.png) | |
| 6 | 404 | `NotFoundPage`, thông báo ở `TicketDetailPage` | [ticket 99999](ui/A_General/2026-11-08/EV-HD-05_ui_A7_ticket_khong_ton_tai.png) · [đường dẫn sai](ui/A_General/2026-11-08/EV-HD-05_ui_A7_trang_khong_ton_tai.png) · [Staff mở ticket không được giao](ui/C_Staff/2026-11-08/EV-HD-05_ui_C_staff_ticket_khong_duoc_giao_404.png) | |
| 7 | Menu/nút theo role; Admin không có nút nghiệp vụ | `config/nav.js`, `TicketDetailPage.jsx` | Employee: [Tạo ticket](ui/B_Employee/2026-11-08/EV-HD-03_ui_B_form_tao_ticket_trong.png) · Staff: [tổng quan](ui/C_Staff/2026-11-08/EV-HD-05_ui_C_tong_quan_staff_khong_new.png), [chi tiết có nút Bắt đầu xử lý](ui/C_Staff/2026-11-08/EV-HD-05_ui_C_chi_tiet_staff.png) · Manager: [chi tiết có nút Phân công](ui/D_Manager/2026-11-08/EV-HD-05_ui_D_chi_tiet_manager.png) · Admin: [danh sách chỉ đọc](ui/E_Admin/2026-11-08/EV-HD-05_ui_E_admin_tickets_chi_doc.png), [chi tiết chỉ đọc](ui/E_Admin/2026-11-08/EV-HD-05_ui_E_admin_chi_tiet_chi_doc.png) | |
| 8 | SCR-04 Tạo ticket + tệp đính kèm | `pages/employee/CreateTicketPage.jsx` | [chọn tệp, chưa gửi](ui/B_Employee/2026-11-08/EV-HD-03_ui_B_dinh_kem_truoc_gui.png) · [ticket HD-2026-0008 có tệp, lịch sử ghi "Đính kèm tệp tin"](ui/B_Employee/2026-11-08/EV-HD-03_ui_B_dinh_kem_sau_gui.png) | [thiếu trường bắt buộc (B2)](../T3-06/ui/B_Employee/2026-11-04/EV-HD-03_ui_B2_thieu_tieu_de_danh_muc.png), [tạo thành công](../T3-06/ui/B_Employee/2026-11-04/EV-HD-03_fe_tao_ticket_thanh_cong.png) |
| 9 | SCR-02 Danh sách (phân trang, lọc) | `pages/common/TicketsPage.jsx` | Employee: [danh sách](ui/B_Employee/2026-11-08/EV-HD-05_ui_B4_danh_sach_employee.png) · Staff: [Ticket được giao](ui/C_Staff/2026-11-08/EV-HD-05_ui_C_danh_sach_staff_khong_new.png) · Manager: [danh sách](ui/D_Manager/2026-11-08/EV-HD-05_ui_D_danh_sach_manager.png) | [tìm kiếm](../T3-06/ui/B_Employee/2026-11-04/EV-HD-05_ui_B5_tim_tra_sua.png), [lọc danh mục](../T3-06/ui/B_Employee/2026-11-04/EV-HD-05_ui_B6_loc_danh_muc.png) |
| 10 | SCR-03 Chi tiết | `pages/tickets/TicketDetailPage.jsx` | Employee [chi tiết](ui/B_Employee/2026-11-08/EV-HD-03_ui_B_dinh_kem_sau_gui.png) · Staff [chi tiết](ui/C_Staff/2026-11-08/EV-HD-05_ui_C_chi_tiet_staff.png) · Manager [chi tiết](ui/D_Manager/2026-11-08/EV-HD-05_ui_D_chi_tiet_manager.png) · Admin [chi tiết](ui/E_Admin/2026-11-08/EV-HD-05_ui_E_admin_chi_tiet_chi_doc.png) | |
| 11 | Staff chỉ thấy ticket được giao, không có hàng New (BE đổi ngày 05/11) | `StaffOverview.jsx`, `TicketsPage.jsx` (commit `2421a18`) | [tổng quan](ui/C_Staff/2026-11-08/EV-HD-05_ui_C_tong_quan_staff_khong_new.png) · [Ticket được giao: chỉ 2 ticket của Staff, không có New](ui/C_Staff/2026-11-08/EV-HD-05_ui_C_danh_sach_staff_khong_new.png) | |
| 12 | Thông báo lỗi thân thiện 401/403/404/422 | `apiClient.js` (`apiError`), các trang trên | 401, 403, 404: xem dòng 4–6 | |

## 3. Chưa kiểm hoặc chưa có bằng chứng

- **Lỗi 422 qua giao diện:** form tạo ticket chặn trước ở FE (`maxLength` 200 / 2000, bắt buộc trường), nên Backend không nhận được dữ liệu sai để trả 422. `apiError()` đã xử lý `message` và `details` của Backend nhưng **chưa kích hoạt thực tế**.
- **Checklist A5** (đăng xuất rồi bấm Back) **chưa có ảnh**. **Bỏ trống mật khẩu riêng** (có email) **chưa chụp**; mới có ảnh bỏ trống cả hai ô.
- **B3** (gõ đủ 200 ký tự, bộ đếm `200/200`) chưa có ảnh; chỉ có ảnh thiếu trường (B2) ngày 04/11.
- **Trạng thái đang tải** (NFR-03) chưa có ảnh.
- **Tạo ticket thành công:** chỉ có ảnh 04/11 do bên test chụp; ngày 08/11 chỉ thấy ticket mới `HD-2026-0008` ở trang chi tiết.
- Giao diện còn tồn tại: dòng "Mức ảnh hưởng" hiện mã `MEDIUM`; trang lỗi ticket (99999, Staff) ghi "Không tải được dữ liệu" thay vì "Không tìm thấy ticket".
- Mới thử bằng Chrome, màn hình rộng. Chưa test responsive, trình duyệt khác, thao tác bằng bàn phím.
- Cột SLA: Backend chưa cập nhật `isSlaBreached` cho ticket đang mở quá hạn (đã báo nhóm Backend). Đây là việc của Backend, không thuộc phạm vi T3-07.

## 4. Cách chạy lại

`cd src/frontend && npm ci && npm run dev` (Backend và DB chạy bằng Docker Compose theo `DEPLOY_NOTES.md`), mở `http://localhost:5173`. Ẩn nút ở FE chỉ là UX; chặn quyền thực sự nằm ở Backend (IA-HD02).
