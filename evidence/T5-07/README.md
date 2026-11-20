# Evidence T5-07: lab v0.5-mvp

Ngày kiểm: 20/11/2026. Người thực hiện: Nguyễn Nhật Chính (SE196524). Kết quả lấy từ VM Ubuntu lab, không chỉnh sửa.
Ảnh chụp bằng trình duyệt ẩn danh, đã cắt bỏ thanh dấu trang cá nhân; thanh địa chỉ vẫn hiện `https://100.124.80.30:8443/`.
Không có mật khẩu, token hay nội dung `.env` trong các tệp này.

Mã nguồn: tag `v0.5-mvp` = commit `35c573d`. Script kiểm tra: `scripts/lab-check.sh` ở commit `a44208a`. Chi tiết dựng lab xem `DEPLOY_NOTES.md`, mục "Bản triển khai tuần 5 (T5-07)".

| Mục | Kết quả | Tệp |
|---|---|---|
| Kiểm tra tự động | Mục 1 đến 7 đạt, `KẾT QUẢ: đạt` | `lab-check_v05_8443.txt` |
| Employee, Tổng quan | 3 ticket, danh sách ticket gần đây | `ui/EV-HD-14_ui_14.03_employee_tong_quan.png` |
| Employee, Chi tiết ticket | Dòng thời gian trạng thái, hạn xử lý SLA, lịch sử xử lý, tệp đính kèm | `ui/employee_chi_tiet_ticket.png` |
| IT Staff, Tổng quan | 2 ticket được giao, huy hiệu SLA | `ui/EV-HD-14_ui_14.02_staff_tong_quan.png` |
| IT Staff, Giám sát SLA | Bình thường 2, sắp quá hạn 0, quá hạn 0 | `ui/staff_giam_sat_sla.png` |
| IT Manager, Tổng quan | Thống kê theo mức ưu tiên, tình trạng SLA (chế độ English) | `ui/EV-HD-14_ui_14.01_manager_tong_quan.png` |
| IT Manager, Giám sát SLA | 3 ticket đang mở, hạn xử lý còn lại | `ui/manager_giam_sat_sla.png` |
| IT Manager, Yêu cầu mở lại | 1 yêu cầu chờ duyệt (`HD-2026-0002`) | `ui/manager_yeu_cau_mo_lai.png` |
| Admin, Tổng quan | 6 người dùng, 5 danh mục, 4 mức ưu tiên, 7 trạng thái, 38 bản ghi nhật ký | `ui/EV-HD-14_ui_14.01_admin_tong_quan.png` |
| Admin, Người dùng | Danh sách 6 tài khoản, các nút gán vai trò, đặt lại mật khẩu, khóa | `ui/admin_nguoi_dung.png` |
| Admin, Cài đặt hệ thống | Danh mục, mức ưu tiên, trạng thái, cấu hình SLA | `ui/admin_cai_dat_he_thong.png` |
| Admin, Nhật ký hệ thống | Ghi đăng nhập thành công và thất bại, đổi trạng thái, gửi yêu cầu mở lại | `ui/admin_nhat_ky.png` |

Một số ảnh ở chế độ English còn chuỗi tiếng Việt; đã ghi trong `DEPLOY_NOTES.md`, mục "Hạn chế đã biết".
