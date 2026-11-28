# Cập nhật hồ sơ retest và quyết định IA ngày 28 tháng 11 năm 2026

Nguyễn Trọng Khiêm · IA Lead. Mốc main đối chiếu 8cd144c.

- 02 v1.12 Draft: Risk Register §8, credential High/Open, Accepted risk riêng BUG009/lab v0.6-final, điều kiện security sign-off.
- 04 v2.7 Draft: nhận 106/106 assertions rc1/WAF none từ PR #61 theo đúng scope; cập nhật BUG008–012, High mở và Evidence Index. Giữ functional regression/156 tests do Trí ghi; không cộng số giữa các lượt.
- 08 v2.2 Draft: §5.4, R-01 một phần, quyết định và điều kiện còn chặn final.
- Báo cáo T6-01 và Defect Log T6-16 cập nhật PR #52/#57/#60/#64 đã merge, #61/#62/#63 còn mở (#63 a78a8c8 đã hoàn lại 6 hash seed về bản cũ, chưa merge); giữ nguồn Before và trạng thái các ca NOT RUN.
- WAF README ghi finding Gitleaks fixture là false positive riêng lẻ; chưa có kết luận toàn bộ scan PASS.

Quyết định: BUG008 còn biên giây/chưa Accepted; BUG009 Medium Accepted risk riêng lab với phạm vi/hạn tại 02 §8.2; BUG010 fix merged/chờ retest; BUG011 Open/chưa Accepted; BUG012 High/Open, chờ xử lý seed, rotate tài khoản VM và AFTER final.

Nguồn đầy đủ: [hồ sơ review](../evidence/T6-01/REVIEW_DECISIONS_20261128.md). Chỉ cập nhật hồ sơ repo; không thay source ứng dụng, hash seed, mật khẩu VM, config gateway, Workbook/Sheet/Drive; không merge PR thành viên khác hoặc tạo tag/deploy final.
