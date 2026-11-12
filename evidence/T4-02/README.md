# Bằng chứng T4-02 — SEC-TC-HD-06 workflow bypass + mass assignment (Nguyễn Tăng Minh Khánh)

Build: tag `v0.4-week4` = commit `a378fdb`. Môi trường: VM lab của nhóm `https://100.124.80.30:8443` (truy cập qua Tailscale), ngày 12/11/2026.
Script không in token; mật khẩu nhập qua biến môi trường (`read -s`).
Chạy lại: `export BASE=... PW_EMP1=... PW_EMP2=... PW_STAFF=... PW_STAFF2=... PW_MGR=... PW_ADMIN=...` rồi `bash evidence/T4-02/sec_tc_hd_06.sh`.

| EV-ID | File | Kết quả |
|---|---|---|
| — | sec_tc_hd_06.sh | Script curl + jq, 60 ca; expected theo TicketStatusTransitionValidator, BR-HD-01/02/04/05 |
| EV-HD-26 | result_lab_20261112.txt, EV-HD-26_lab_01.png, EV-HD-26_lab_02.png | **60/60 PASS** |
| EV-HD-26 | audit_events_lab_20261112.txt | 33 event (Nhi trích từ /app/logs/audit_events.jsonl trên VM lab) |

| Nhóm ca | BR / SR | Kết quả |
|---|---|---|
| 06.1 Nhảy trạng thái (NEW→RESOLVED/CLOSED/IN_PROGRESS, ASSIGNED→RESOLVED, IN_PROGRESS→CLOSED) | State machine, HD07 | 422, trạng thái không đổi |
| 06.2 RESOLVED khi giải pháp rỗng / < 20 ký tự / toàn khoảng trắng | BR-HD-01 | 400 |
| 06.3 Staff/Admin/Employee khác đóng hoặc mở lại | BR-HD-02, IA-HD02 | 403 / 404 + AUTHZ_DENIED |
| 06.4 Comment, upload, đổi trạng thái, assign, reopen trên ticket CLOSED | BR-HD-04 | 422 |
| 06.5 Employee/Staff assign; gán Manager/Employee; điều phối lại | BR-HD-05, IA-HD01 | 403 / 422; chỉ còn 1 người phụ trách |
| 06.6 Mass assignment (status, priority, assigneeId, requesterId, role, deadline, isInternal) | IA-HD01/02 | Trường thừa bị bỏ qua |
| 06.7 Staff không được giao / Employee đổi trạng thái | IA-HD02 | 404 / 403 |
| 06.8 Employee tự duyệt; Staff, Admin duyệt reopen; gửi trùng khi đang chờ | BR-HD-02, IA-HD01 | 403 / 422 |

Audit: 33 event, gồm 31 AUTHZ_DENIED khớp 1-1 với 31 ca bị chặn quyền của 2 script (SEC-TC-HD-01/05 và 06), 1 REOPEN_REQUESTED, 1 REOPEN_REJECTED. Không có REOPEN_APPROVED vì script chỉ thử duyệt bằng role không có quyền (403); luồng duyệt thành công có ở EV-HD-10 (T4-05). Ca 400/422 là lỗi nghiệp vụ, không sinh audit (SDD §7). `src_ip` là IP Tailscale thật của máy test. Không có token/mật khẩu trong file.

Kết luận: SEC-TC-HD-06 PASS trên `v0.4-week4`. Không phát hiện BUG mới.
