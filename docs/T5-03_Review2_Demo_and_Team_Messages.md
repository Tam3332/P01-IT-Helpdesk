# Hướng dẫn đồng bộ nhóm tuần 5 — gửi theo đúng đầu mối

**Lưu ý:** Các tin nhắn dưới đây **chưa được gửi**. Đây là mẫu copy; không chỉnh sửa trực tiếp Sheet/Docs/Repo chung trong lần chuẩn bị này.

## Nhắn Trí — event SLA_BREACHED (T5-05)

Trí ơi, đối chiếu SRS P2/BR-HD-03, Sheet T5-05 và AuditLogger thì event dùng `SLA_BREACHED`. Khi worker lần đầu cập nhật `is_sla_breached` false → true, nhờ ông ghi `target_type=TICKET`, `target_id` là mã `HD-YYYY-XXXX`, `action=MARK_SLA_BREACHED` (đề xuất), `outcome=SUCCESS`, `before={"is_sla_breached":false}`, `after={"is_sla_breached":true}`. Vì worker chạy nền nên `actor_id/role/src_ip/http_status=null`, `timestamp` UTC và correlation UUID. Dùng `logAfterCommit` để tránh log trước khi DB commit. Chỉ phát một lần, không phát lặp khi worker quét; Reopen giữ cờ cũ nếu đã vi phạm. Khi xong báo commit/tag trên main để tôi đối chiếu EV-HD-24 và SDD §7 v2 nhé.

## Nhắn Cường — lockout event + upload/comment (T5-06/T5-13)

Cường ơi, Sheet T5-03/T5-06 đang dùng `ACCOUNT_LOCKED` nhưng SRS/SDD và Backend nhánh ông còn `BRUTE_FORCE_DETECTED`; nhờ ông chốt một tên event cuối cùng để tôi cập nhật SDD §7. Monitoring v2 đang nhận cả hai tên, vì lần sai thứ 5 Backend trả `LOGIN_FAILED/429`, rule R1 sẽ lấy event khóa làm mốc thứ 5 sau 4 lần 401, tránh bỏ sót/không báo trùng. Nhờ ông bảo đảm event khóa chỉ phát lúc bắt đầu khóa, log có IP, actor/email đúng schema. Phần `FILE_UPLOAD_REJECTED` dùng trường `reason` mã lỗi riêng như đã thống nhất; `ROLE_CHANGED` cần đồng bộ thay cho `CHANGE_ROLE`. Khi build sẵn sàng báo commit/PR để tôi test AFTER trên VM.

## Chỉ nhắn Nhi/Chính **một lần sau khi build MVP sẵn sàng**

Nhi/Chính ơi, phần sidecar up/read log ngày 16/11 đã có evidence rồi nên không cần kiểm lại. Khi Trí/Cường đưa Backend mới lên main và VM chạy đúng tag, nhờ hai bạn hỗ trợ **một lượt**: xác nhận commit/tag Backend + Monitoring đang deploy, xác nhận `alerts.jsonl` thực sự ghi được; sau khi tôi hoàn thành các thao tác kiểm thử lab R1/R2 và Audit AFTER, giúp xuất bản log `alerts.jsonl` cùng đoạn Audit tương ứng **đã che** (không có email/IP thật/token/secrets), có timestamp. Tôi sẽ tự tổng hợp vào EV-HD-22/24, tránh phải xin evidence nhiều lần. Việc restart sidecar để nạp Monitoring v2 chỉ thực hiện sau khi bên vận hành duyệt.

## Kịch bản demo bảo mật tích hợp — phần Hòa

1. Trình bày sơ đồ `Backend -> audit_events.jsonl -> Monitoring Python -> alerts.jsonl` và ranh giới đọc/ghi (không có API sửa log).
2. Trình bày kết quả Audit Login/IDOR đã test từ trước, phân biệt bản BEFORE và bản AFTER dự kiến. Trên môi trường lab được phép, chạy case login sai/lockout và đối chiếu `LOGIN_FAILED/401`, mốc `429`, `ACCOUNT_LOCKED`/alias và R1. Tránh chạy lại không kiểm soát làm ảnh hưởng tài khoản dùng chung.
3. Dùng tài khoản/ticket test đã được nhóm cấp phép để tạo 3 từ chối quyền liên quan ticket, so với R2 (không tính 404 thường).
4. Mở bảng HTML alert local từ `alerts.jsonl` đã lấy/che. **Nếu chỉ có fixture synthetic**, nói rõ *DEMO giả lập*; không gọi là alert live của VM.
5. Đối chiếu `SLA_BREACHED` từ worker Trí và `FILE_UPLOAD_REJECTED(reason)` từ Cường trong Audit Log mới, kiểm tra `before/after` và log không lộ password/JWT/file content.
6. Kết thúc bằng kết quả PASS/FAIL/NOT RUN từng TC, đúng tag/commit và BUG còn mở. Hòa phụ trách phần alert; Khiêm điều phối kịch bản tổng nhóm.

## Điền Sheet phân công (tự điền, không tự sửa từ bản này)

- **Tuần 4 — T4-03**: `In progress`; EV-HD-24 BEFORE có, AFTER chờ deploy Backend mới; PR #22 merged.
- **Tuần 4 — T4-10**: `Review`; EV-HD-22 offline PASS và sidecar up/read có; PR #22 và PR #27 merged; live alert/token expiry/429 còn thiếu.
- **Tuần 5 — T5-03**: `In progress` khi code/test offline xong, ghi 39/39 PASS, fixture 10 events→R1/R2, màn HTML DEMO; PR/commit mới chỉ điền sau khi thực sự tạo.
- **Tuần 5 — T5-10**: `In progress`; trước đây Sheet ghi PR #22 Draft nhưng thực tế **Merged**; PR #27 cũng **Merged**. Phần AFTER phụ thuộc build VM mới, R-01 chỉ đóng khi nhóm duyệt.

> Tất cả kết quả backend/VM ghi là `NOT RUN` cho đến khi có evidence thật từ đúng build; không sửa số liệu lịch sử để nhìn giống như PASS.
