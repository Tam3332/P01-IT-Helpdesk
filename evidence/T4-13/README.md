# Bằng chứng T4-13 — Sửa / retest BUG tuần 3 (Lê Phú Cường)

Môi trường: máy dev (Windows), backend nhánh `cuong/be` sau merge `tri/be` 97d7307, PostgreSQL 18 Docker cổng 5433. Chưa chạy trên VM nhóm (lab đang chạy tag `v0.4-week4`, chưa có các bản sửa dưới).

| EV-ID | File | Ngày | Bug | Mô tả | Kết quả quan sát |
|---|---|---|---|---|---|
| EV-HD-01 | api/EV-HD-01_api_BUG-HD-003_audit_before_after.png | 2026-11-14 | BUG-HD-003 (sửa: Trí, bf36e8c) | Tạo HD-2026-0005, giao staff, giao lại staff2, IN_PROGRESS, RESOLVED, Employee đóng; xem `audit_events.jsonl` | TICKET_ASSIGNED (ASSIGN null→2, REASSIGN 2→7), TICKET_STATUS_CHANGED ×2, TICKET_CLOSED, đều có `before`/`after`. Còn lệch: đổi role ghi `CHANGE_ROLE` (SDD §7: `ROLE_CHANGED`) |
| EV-HD-03 | api/EV-HD-03_api_BUG-HD-007_tao_ticket_role_khac_403_authz_denied.png | 2026-11-14 | BUG-HD-007 (sửa: Cường, SecurityConfig) | Manager, Staff, Admin `POST /tickets`; Employee làm đối chứng | 403 FORBIDDEN ×3, 3 dòng AUTHZ_DENIED (target ENDPOINT `POST /api/v1/tickets`); Employee 201 + TICKET_CREATED |
