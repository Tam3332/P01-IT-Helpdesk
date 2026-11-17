# SEC-TC-HD-04 — Audit schema check (tổng hợp đã che)

**Chế độ:** BEFORE — chưa phải evidence live VM
**SHA-256 log đầu vào:** `50b3aee6224957ecb7b8d133416d1aca28e8cca1808e164a7217ca99be8ada48`
**Sự kiện JSON hợp lệ:** 81

## Loại event và số lượng

| Event type | Số lượng |
|---|---:|
| `AUTHZ_DENIED` | 35 |
| `LOGIN_FAILED` | 8 |
| `LOGIN_SUCCESS` | 28 |
| `REOPEN_REJECTED` | 1 |
| `REOPEN_REQUESTED` | 1 |
| `SLA_BREACHED` | 3 |
| `TICKET_CREATED` | 5 |

## Dấu hiệu cần xem xét

| Loại vấn đề | Số sự kiện |
|---|---:|
| `before_after_missing_for_change` | 2 |
| `sla_nonstandard_outcome` | 3 |
| `sla_transition_invalid` | 3 |

## Giới hạn

- Báo cáo không chứa email, actor ID, IP gốc, mã ticket, correlation ID cụ thể, nội dung bình luận/tệp hoặc dòng log gốc.
- PASS schema **không** đồng nghĩa event đã sinh đúng từ thao tác thực tế, sidecar đã nhận/ghi alert hoặc build VM đã đạt.
- Cần đối chiếu thao tác ↔ ticket_history ↔ Audit JSONL ↔ kết quả alert trên đúng commit/tag VM.
- Các sự kiện tạo mới có thể chỉ có `after`; event đổi trạng thái yêu cầu `before` + `after`.
- Log gateway bị chặn trước Backend sẽ không có dòng Backend audit tương ứng.
