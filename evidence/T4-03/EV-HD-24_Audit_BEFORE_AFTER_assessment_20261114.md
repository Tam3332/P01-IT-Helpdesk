# EV-HD-24 — Đánh giá Audit Log BEFORE / AFTER (14/11/2026)

**Người kiểm thử:** Huỳnh Minh Hòa
**Task:** T4-03; SEC-TC-HD-04; IA-HD04/05
**Môi trường:** P01 IT Helpdesk + Security Monitoring, dữ liệu Audit Log nhóm cung cấp
**Phương pháp:** kiểm tra từng dòng JSONL, kiểm đếm loại sự kiện, đối chiếu trường schema và so sánh dữ liệu BEFORE/AFTER.
**Kết luận:** BEFORE có bằng chứng hợp lệ. AFTER **CHƯA ĐỦ CƠ SỞ XÁC MINH (PENDING)**, không ghi PASS.

## 1. Kết quả kiểm tra

| Tiêu chí | BEFORE: `audit_reopen_authz.txt` | AFTER được cung cấp: `audit_after_backend_new.jsonl` |
|---|---:|---:|
| Số dòng JSONL hợp lệ | 33/33 | 81/81 |
| AUTHZ_DENIED | 31 | 35 |
| LOGIN_SUCCESS | 0 | 28 |
| LOGIN_FAILED | 0 | 8 |
| TICKET_CREATED | 0 | 5 |
| SLA_BREACHED | 0 | 3 |
| REOPEN_REQUESTED | 1 | 1 |
| REOPEN_REJECTED | 1 | 1 |
| Có khóa `before` | 0/33 | 0/81 |
| Có khóa `after` | 0/33 | 0/81 |
| Thiếu các khóa schema cơ bản | 0 | 0 |

**Schema cơ bản đối chiếu:** `timestamp`, `event_type`, `actor_id`, `role`,
`target_type`, `target_id`, `action`, `outcome`, `src_ip`, `correlation_id`, `http_status`.

**Đối chiếu chéo:** Cả 33 dòng JSONL BEFORE xuất hiện trong tập 81 dòng AFTER.
Do đó, file AFTER là log tổng hợp nhiều ngày, không phải riêng log sinh ra bởi bản Backend mới.

## 2. Kết quả detector

- BEFORE: 33 sự kiện, 0 skipped; replay ghi nhận 3 cảnh báo `R2_UNAUTHORIZED_TICKET_ACCESS` theo bằng chứng có sẵn của T4-03.
- Với tập log AFTER 81 sự kiện, offline replay trước đó ghi nhận tổng 4 cảnh báo (3 R2 và 1 R1). Kết quả này chứng minh khả năng phát hiện với log được cung cấp, **không chứng minh event before/after đã được triển khai**.
- Không sao chép IP, email hay correlation ID vào báo cáo này.

## 3. Khoảng trống xác minh AFTER

1. Log AFTER hiện tại không chứa thuộc tính `before` và `after` (0/81). Tuy nhiên, **không phải mọi loại event đều bắt buộc có cả hai trường**; cần kiểm đúng các event cập nhật đối tượng theo SDD §7.
2. Chưa có bằng chứng xác nhận mã commit/tag Backend tại thời điểm phát sinh các sự kiện liên quan đến thay đổi trạng thái/phân công.
3. Cần Trí/Cường xác nhận việc phát event workflow/upload/comment và định dạng trước/sau; Nhi hỗ trợ xác nhận phiên bản đã triển khai và xuất log **phát sinh sau deploy**.
4. Nếu yêu cầu `before/after` được định nghĩa trong payload `details` hoặc trường khác thay vì top-level, phải thống nhất schema trong SDD rồi kiểm thử lại theo cấu trúc thực tế, không tự gán FAIL cho code.

## 4. Ca AFTER đề xuất khi bản sửa được triển khai

- Đăng nhập bằng tài khoản demo được phân quyền.
- Thực hiện một thao tác cập nhật trạng thái ticket hợp lệ, ghi nhận giá trị trước/sau.
- Thực hiện một thao tác phân công ticket hợp lệ, ghi nhận người phụ trách trước/sau.
- Đối chiếu event tương ứng theo timestamp UTC, actor, target, outcome, correlation ID và trường trước/sau.
- Kiểm tra không ghi password, JWT, token và nội dung tệp vào audit.
- Xuất đoạn evidence đã che thông tin nhạy cảm; đối chiếu Expected/Actual để kết luận PASS/FAIL.

## 5. Dấu vết nguồn

- SHA-256 `audit_reopen_authz.txt`: `1227f86ec1a5190f66d2c11facb6fe6b90431de2f86599e7ff0ee575645b72f3`
- SHA-256 `audit_after_backend_new.jsonl`: `50b3aee6224957ecb7b8d133416d1aca28e8cca1808e164a7217ca99be8ada48`

**Trạng thái cập nhật bảng phân công:** T4-03 — `In progress`.
**Giới hạn:** Đây là báo cáo đánh giá log được cung cấp, không phải kết quả kiểm thử trực tiếp bản Backend mới.
