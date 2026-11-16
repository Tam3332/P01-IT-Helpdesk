# Bằng chứng T5-13 — Audit upload/comment kèm lý do từ chối, before/after vòng đời và reopen (Lê Phú Cường)

Môi trường: máy dev (Windows), backend nhánh `cuong/be` commit `9465137` (đã push), PostgreSQL 18 Docker. Ngày chạy: 17/11/2026. Chưa chạy trên VM nhóm (lab đang chạy tag `v0.4-week4`; chờ PR vào `main`, sau đó Nhi cập nhật backend lên VM và chạy lại SEC-TC-HD-03).

Trường `reason` theo thống nhất với Hoà (17/11): trường riêng, tùy chọn, không đưa vào `before`/`after`; `FILE_UPLOAD_REJECTED` ghi mã lỗi; không ghi tên hay nội dung tệp. SDD §7 sẽ được Hoà bổ sung trường này.

Mã EV đặt theo TC tương ứng (EV-HD-xx ↔ TC-HD-xx). Quy ước mã evidence chung đang chờ Khánh chốt (T5-09).

| EV-ID | File | Ngày | Yêu cầu | Mô tả | Kết quả quan sát |
|---|---|---|---|---|---|
| EV-HD-10 | api/EV-HD-10_api_01_tao_ticket_HD-2026-0009.png | 2026-11-17 | HD03 | Employee tạo ticket test | 201, HD-2026-0009 (id 9) |
| EV-HD-10 | api/EV-HD-10_api_02_vong_doi_va_reopen.png | 2026-11-17 | HD06, HD07, HD08, HD10; IA-HD04 | Manager phân công staff → staff IN_PROGRESS → RESOLVED → employee bình luận → employee yêu cầu mở lại → manager duyệt | assign 200, in progress 200, resolved 200, comment 201, reopen 201 (request 4), approve 200 |
| EV-HD-04 | api/EV-HD-04_api_upload_tu_choi_va_gioi_han.png | 2026-11-17 | HD04, UC-03, IA-HD03 | Upload vào HD-2026-0009: fake.png (nội dung PHP), a.exe, tệp rỗng, big.pdf 10,5 MB; upload vào HD-2026-0005 (Closed); ok.png 6 lần | 4 ca đầu: 400; ticket Closed: 422; ok.png lần 1–5: 201, lần 6: 400 |
| EV-HD-04, EV-HD-10 | audit_vong_doi_upload_reason_20261117.jsonl | 2026-11-17 | IA-HD04; SDD §7 | Trích 18 dòng từ `src/backend/logs/audit_events.jsonl` ứng với các ca trên | Xem bảng dưới |

Đối chiếu từng dòng trong `audit_vong_doi_upload_reason_20261117.jsonl`:

| Event | before / after / reason |
|---|---|
| `TICKET_CREATED` | — |
| `TICKET_ASSIGNED` | before `assignee_id: null` → after `assignee_id: 2` |
| `TICKET_STATUS_CHANGED` ×2 | ASSIGNED → IN_PROGRESS; IN_PROGRESS → RESOLVED |
| `COMMENT_ADDED` | after `comment_id: 7, is_internal: false` (không ghi nội dung bình luận) |
| `REOPEN_REQUESTED` | before `status: RESOLVED` → after `status: REOPEN_PENDING` |
| `REOPEN_APPROVED` | before `status: REOPEN_PENDING` → after `status: REOPENED` |
| `FILE_UPLOAD_REJECTED` (fake.png, a.exe) | `reason: FILE_TYPE_NOT_ALLOWED` (sai magic bytes và sai đuôi cùng mã lỗi) |
| `FILE_UPLOAD_REJECTED` (tệp rỗng) | `reason: VALIDATION_FAILED` |
| `FILE_UPLOAD_REJECTED` (big.pdf 10,5 MB) | `reason: FILE_TOO_LARGE`; `target_type: ENDPOINT`, `target_id: POST /api/v1/tickets/9/attachments` (xem ghi chú) |
| `FILE_UPLOAD_REJECTED` (HD-2026-0005 Closed) | `reason: TICKET_CLOSED`, http_status 422 |
| `FILE_UPLOADED` ×5 | after `attachment_id, file_size, mime_type` (không ghi tên tệp) |
| `FILE_UPLOAD_REJECTED` (tệp thứ 6) | `reason: ATTACHMENT_LIMIT_EXCEEDED` |

Ghi chú:
- Tệp từ 10 MB đến 11 MB bị Spring chặn khi đọc multipart (`max-file-size: 10MB`), trước khi vào controller/service, nên chưa tra được mã ticket và chưa kiểm quyền; audit ghi theo đường dẫn request. Tệp lớn hơn 11 MB bị Nginx trả 413, không tới backend nên không có audit.
- Ảnh chụp PowerShell có một số ký tự lẫn (`\x0a…189feb17…`) do lỗi dán nhiều dòng vào terminal; không ảnh hưởng kết quả.
- Unit test: xem `evidence/T5-06/unit_test_78_pass.png` (tổng 78/78 gồm cả `AuditLoggerTest` với 2 ca mới về `reason`; phần đầu danh sách test nằm ngoài khung ảnh).
