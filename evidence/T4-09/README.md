# Bằng chứng T4-09 — Retest audit gap, PC-03, Access Matrix v1.1 (Nguyễn Tăng Minh Khánh)

Build: tag `v0.4-week4` = commit `a378fdb`. Môi trường: VM lab của nhóm `https://100.124.80.30:8443` (Tailscale), ngày 12/11/2026.

| EV-ID | File | Kết quả |
|---|---|---|
| EV-HD-21 (after) | ../T3-02/result_retest_lab_20261112.txt, ../T3-02/EV-HD-21_retest_lab.png | SEC-TC-HD-01/05: 18/18 PASS |
| EV-HD-21 (after) | ../T4-02/audit_events_lab_20261112.txt (dòng 6 và 15) | Có AUTHZ_DENIED `CLOSE_TICKET` 404 (ca 01.7) và `UPDATE_STATUS` 403 (ca 05.8) → BUG audit gap tuần 3 đã sửa |
| — | pc03_bosung.sh, pc03_bosung_lab_20261112.txt | PC-03 (2) Admin tải tệp → 403; (3) GET feedback đúng role; (4) Employee không phải chủ gửi feedback → 404: **11/11 PASS** |
| — | ../../docs/P01_Access_Matrix.xlsx | Access Matrix v1.1: thêm change-password, priorities/statuses, sla-configs/{priority}; mã lỗi theo SRS (không dùng 409); sheet "Đối chiếu v0.4-week4" |

BUG mới (Low, ghi 04 §4): POST /tickets bởi người không phải Employee trả 403 nhưng không ghi AUTHZ_DENIED (owner Cường); đổi mật khẩu không thu hồi token cũ (owner Trí).
