# CHANGELOG v1.1 — 29/10/2026

Trạng thái phát hành: bản chuẩn bị Review 1, chưa baseline/approved.

## 01 SRS
- Thống nhất Group2, 7 thành viên, v1.1/ngày 29-10-2026 và trạng thái Draft/Chờ Review 1; footer giữ trường số trang.
- SRS dùng §6 cho Use Case và §8 cho NFR. Sửa các tham chiếu NFR-09/10 không tồn tại sang Reliability/Audit hoặc Integrity theo nội dung; auditability → NFR-05, UI → NFR-03.
- Chuẩn hoá mã upload SEC-TC-HD-03.1…03.5 và hardening SEC-TC-HD-08.1…08.5 theo nhóm test tương ứng.
- Thêm liên kết hồ sơ; traceability HD01–14 để Chờ Review 1, HD15 giữ Should ngoài MVP; không ký thay GV/Lead.
- Giữ nội dung và hình nguồn; các xung đột nghiệp vụ chưa được nhóm chốt được quản lý tại PC-01…06, không tự đổi thành quyết định cuối.

## 02 SecRS
- Header/version đồng bộ; sửa PC-06 cho đúng vị trí SRS §6/§8 và ghi đã sửa tham chiếu trong bản gộp, chờ SE review.
- Bản gộp giữ 12 TH, 7 mã SR, 8 nhóm test, 12 dòng risk; DFD v0.2, mapping F/TB/STRIDE và các quyết định đang chờ.
- EV-HD-21…28 vẫn dự kiến, chưa chạy test hoặc chấm residual risk.

## 03 SDD
- Header/version/footer đồng bộ.
- Hoàn thiện mapping §8 theo thiết kế hiện có và baseline SecRS; ghi rõ chỗ khác biệt cần owner chốt.
- File: loại/kích thước/số lượng, kiểm quyền, quan hệ file–ticket, ngoài webroot, trạng thái Closed và các mã test.
- Log: P1 → F7 → D3 → F8 → P2; phân biệt ticket_history D1, Backend ghi/lock, Monitoring đọc/cảnh báo.
- TLS/DB/Docker được ghi là thiết kế cần chứng minh triển khai; không ghi đã cấu hình/Gitleaks sạch khi chưa có report.
- Giữ trách nhiệm deployment thuộc Nhi; điều chỉnh giữ tiêu đề/bảng ADR cùng trang để tránh mồ côi tiêu đề khi xuất PDF.

## 08 và Review 1
- Điền RACI 7 thành viên theo phân công (đề xuất), tracker 7 tuần, contribution evidence, 8 risk/issue và các mục review/scope acceptance.
- Tạo 7 slide chỉnh sửa được, kịch bản 4 trang và mẫu biên bản để ghi phản hồi thực tế.
- Chuẩn bị link bằng chứng và checklist trên Sheet/board; không tự chuyển Done khi chưa có xác nhận review.

## Kiểm tra bản xuất
- Word SRS: 37 trang; 08: 6 trang; kịch bản: 4 trang. Đã render bằng Word và kiểm tra bố cục.
- SecRS: bản xuất native 27 trang; SDD: bản xuất native 19 trang, kiểm tra phần chỉnh sửa.
- Google Slides: 7 slide; đã xuất PDF và kiểm tra đủ 7 trang sau import. Các bảng/diagram vẫn chỉnh sửa được.
- Helper render Slides yêu cầu đường dẫn Unix và công cụ Perl; máy Windows dùng một lần xuất PDF qua connector và Poppler để kiểm tra từng trang. LibreOffice không có sẵn, Word COM dùng để render DOCX.
- Các kiểm tra trên là kiểm tra tài liệu/bố cục, không phải kiểm thử an ninh của ứng dụng.
