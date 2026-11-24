# Evidence T6-12: Kiểm thử E2E luồng P3 (Reopen Ticket) trên VM nhóm

- **Ngày thực hiện:** 24/11/2026
- **Người thực hiện:** Phạm Nguyễn Minh Trí (SE192434) & Nguyễn Nhật Chính (SE196524)
- **Môi trường:** VM Ubuntu lab nhóm (`v0.5-mvp` qua mạng Tailscale `100.124.80.30:8443`)
- **Mã liên quan:** BUG-HD-004 (High), FR HD10, UC-08, TC-HD-10.06..10.08
- **Kết quả:** Đạt PASS 100%, đóng triệt để `BUG-HD-004`

---

## Danh mục ảnh minh chứng (Evidence Index)

| STT | Tên file ảnh | Mô tả chi tiết | Vai trò / Người dùng |
| :---: | :--- | :--- | :--- |
| **01** | `EV-HD-10_p3_01_ticket_cho_duyet_mo_lai_manager.png` | Chi tiết ticket `HD-2026-0005` hiển thị trạng thái `Chờ duyệt mở lại` (`REOPEN_PENDING`), nút *Xem xét yêu cầu mở lại* của Manager | IT Manager (`manager@helpdesk.local`) |
| **02** | `EV-HD-10_p3_02_manager_xem_xet_yeu_cau_mo_lai.png` | Giao diện Manager xem xét yêu cầu mở lại: hiển thị lý do mở lại *"chưa được giải quyết triệt để"*, giải pháp trước đó và lịch sử xử lý đầy đủ | IT Manager (`manager@helpdesk.local`) |
| **03** | `EV-HD-10_p3_03_manager_phe_duyet_thanh_cong.png` | Manager phê duyệt thành công yêu cầu mở lại: banner *(i) Yêu cầu này đã được xử lý: đã duyệt*, ghi chú duyệt *"cần kiểm tra lại và giải quyết triệt để"* | IT Manager (`manager@helpdesk.local`) |
| **04** | `EV-HD-10_p3_04_staff_ticket_chuyen_trang_thai_mo_lai_reopened.png` | Giao diện IT Staff: Ticket `HD-2026-0005` chuyển sang trạng thái `Đã mở lại 1 lần`, huy hiệu `Mở lại` (`REOPENED`), kích hoạt nút *Bắt đầu xử lý* cho chu kỳ SLA mới | IT Staff (`staff2@helpdesk.local`) |
| **05** | `EV-HD-10_p3_05_employee_tong_quan_thong_bao.jpg` | Giao diện Tổng quan của Employee: Danh sách ticket cập nhật và thông báo chuông hệ thống | Nhân viên (`employee@helpdesk.local`) |
