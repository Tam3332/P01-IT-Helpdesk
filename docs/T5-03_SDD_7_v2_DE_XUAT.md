# SDD §7 v2 — Audit / Monitoring Design (bản đề xuất tuần 5)

> **Phạm vi:** P01 — IT Helpdesk + Security Monitoring; task T5-03, T5-10. Tài liệu **để Hòa đối chiếu và sao chép có chọn lọc** vào Google Doc 03-SDD. Không tự sửa tài liệu nhóm. **Không ghi đã Approved** nếu nhóm chưa duyệt.
>
> **Căn cứ:** SRS §3.2 P2, HD11/12, BR-HD-03; SecRS IA-HD04/05, NFR-05; SDD §6–7 hiện tại; Sheet T5-03/05/06/10/12/13; source `src/monitoring/monitor.py`, `AuditLogger.java` trên nhánh Backend.

## 7.1. Kiến trúc và ranh giới tin cậy

Backend Spring Boot ghi **Security Audit Log** theo JSON Lines (mỗi dòng một object) vào `audit_events.jsonl` ở volume `audit_logs`. Log bảo mật tách riêng khỏi bảng nghiệp vụ `ticket_history` trong PostgreSQL. Container Monitoring Python chạy riêng, chỉ đọc `audit_logs:/audit:ro`, không có kết nối mạng, phân tích R1/R2 rồi ghi `alerts.jsonl` vào volume `monitoring_alerts:/alerts`. Monitoring **không** tự khóa tài khoản, cập nhật ticket hoặc sửa Audit Log. Trong Compose, `--follow` mặc định bắt đầu từ cuối file hiện tại; sự kiện phát sinh trước thời điểm khởi động không tự động tạo cảnh báo live.

Tính đến bằng chứng Nhi cung cấp 16/11/2026, container `p01-lab-v04-monitoring-1` chạy, chương trình khởi động và đọc được file Audit; **chưa có bằng chứng ghi thành công `alerts.jsonl` hoặc tạo cảnh báo R1/R2 live trên VM**. Bản v2 tại đây mới được kiểm thử **offline**; kết quả triển khai phải được ghi sau buổi lab.

## 7.2. Schema JSONL chuẩn

Mỗi event có các trường: `timestamp` (ISO-8601 UTC), `event_type`, `actor_id` (id số hoặc null), `role` (hoặc null), `target_type`, `target_id`, `action`, `outcome`, `src_ip`, `correlation_id` (UUID), `http_status` (hoặc null). Các trường bổ sung **tùy chọn**: `before`, `after` (object dữ liệu thay đổi); `reason` (mã lỗi ngắn với event thất bại, ví dụ `FILE_TOO_LARGE`).

- Chỉ đưa trường `before/after` khi có trạng thái trước/sau đáng ghi; không đặt lý do từ chối vào `after`.
- Không ghi mật khẩu, JWT, `Authorization` header, nội dung bình luận, nội dung file hoặc tên file nhạy cảm. Dữ liệu định danh trong log gốc chỉ dùng ở phạm vi hệ thống, cần che trước khi đưa vào repo.
- Event chạy từ scheduler không có request HTTP: `actor_id`, `role`, `src_ip`, `http_status` có thể null; `timestamp` và `correlation_id` phải được tạo theo logic AuditLogger.
- `target_type=TICKET` thì `target_id` dùng **mã ticket** (`HD-YYYY-XXXX`) để truy vết nhất quán; `target_type=ENDPOINT` chỉ dùng khi request bị từ chối trước khi controller lấy được ticket. File bị Nginx chặn trước Backend không thể có Backend Audit Log; kiểm chứng ở log gateway.
- Dùng `logAfterCommit(...)` cho event thành công gắn với thay đổi DB; không phát log thành công trước commit.

## 7.3. Event Catalog cần đối chiếu với build MVP

| Event | Trigger | Target | Dữ liệu/kết quả kỳ vọng |
|---|---|---|---|
| `LOGIN_SUCCESS` | Đăng nhập hợp lệ | USER | SUCCESS, 200, không ghi JWT |
| `LOGIN_FAILED` | Sai mật khẩu hoặc bị khóa | USER_EMAIL | 401 trong 4 lần đầu; 429 ở lần khóa/thời gian khóa; 403 với user bị Admin vô hiệu hóa |
| `ACCOUNT_LOCKED` | Backend bắt đầu khóa login 15 phút | USER_EMAIL | FAILURE, 429; tên theo **Sheet T5-03/06** |
| `BRUTE_FORCE_DETECTED` | Tên event khóa cũ còn ở SRS/SDD & nhánh Cường | USER_EMAIL | **Chấp nhận tương thích khi đọc**; trước khi chốt MVP cần đồng bộ một tên với Backend |
| `AUTHZ_DENIED` | Từ chối quyền | TICKET hoặc ENDPOINT | 403/404 theo Access Matrix; không coi 404 ticket không tồn tại là AUTHZ_DENIED |
| `TICKET_CREATED`, `TICKET_ASSIGNED`, `TICKET_STATUS_CHANGED`, `TICKET_CLOSED` | Tác vụ ticket | TICKET | `before/after` cho phân công/thay trạng thái; chỉ sau commit |
| `REOPEN_REQUESTED`, `REOPEN_APPROVED`, `REOPEN_REJECTED` | Quy trình P3 | TICKET | `before/after` theo thay đổi trạng thái liên quan; không log nội dung lý do cá nhân |
| `FILE_UPLOADED`, `FILE_UPLOAD_REJECTED`, `COMMENT_ADDED` | Upload/comment | TICKET | Event được ghi khi xử lý; từ chối có `reason` mã lỗi; không ghi nội dung file/comment |
| `ROLE_CHANGED` | Admin đổi vai trò | USER | `before/after` giá trị role cũ/mới; **cần sửa mã event nếu Backend còn dùng `CHANGE_ROLE`** |
| `SLA_BREACHED` | Worker đánh dấu lần đầu vi phạm SLA | TICKET | `before/after` cờ `is_sla_breached`, chỉ ghi 1 lần |
| `WAF_BLOCKED` | WAF chặn request (dự kiến tuần 5) | ENDPOINT | Nguồn **gateway riêng**; chưa thống nhất format/nguồn, chưa tích hợp Rule R3 |

> SRS/SDD ngày 17/11 còn gọi event lockout là `BRUTE_FORCE_DETECTED` trong khi Sheet T5-03/T5-06 dùng `ACCOUNT_LOCKED`. Để không chặn kiểm thử, detector v2 nhận cả hai nhưng nhóm **phải chốt đúng một tên phát ra từ Backend** trước review. Không tự coi tài liệu đã đồng bộ.

## 7.4. Detection Rules

**R1 — `R1_BRUTE_FORCE_LOGIN` (HIGH).** Cùng `actor_id` (hoặc SHA-256 email chuẩn hóa khi chưa xác định tài khoản) và `src_ip`: đếm 5 lần thất bại trong 300 giây. Với Backend cũ: 5 `LOGIN_FAILED/401` sinh một alert. Với Backend lockout tuần 5: 4 `LOGIN_FAILED/401` rồi `LOGIN_FAILED/429` và event `ACCOUNT_LOCKED` hoặc `BRUTE_FORCE_DETECTED` cho **lần khóa đầu tiên**; R1 coi event khóa này là tín hiệu thứ 5, chỉ khi đã thực sự thấy đủ 4 event 401 trong cùng cửa sổ. `LOGIN_FAILED/429` lặp lại trong 15 phút khóa không được cộng thêm hoặc tạo cảnh báo trùng. `LOGIN_FAILED/403` (Admin vô hiệu hóa tài khoản) không tính; `LOGIN_SUCCESS` cùng actor/IP xóa chuỗi. Monitoring **không tự khóa**.

Điều kiện kiểm chứng: mọi event phải có `src_ip` đúng client; nếu Backend nhận IP proxy thì cách nhóm R1 có thể sai. Khi sidecar vừa khởi động bằng `--follow` giữa chuỗi, không tự tuyên bố đã thấy đủ 5 lần. Lưu ý: đoạn v2 này xử lý logic đếm ở mức **offline/test**, chưa xác minh log format trên MVP của nhóm.

**R2 — `R2_UNAUTHORIZED_TICKET_ACCESS` (MEDIUM, ngưỡng 3 lần/300 giây còn phải xác nhận Approved).** Cùng actor có ≥3 event `AUTHZ_DENIED` liên quan ticket (`target_type=TICKET` hoặc `ENDPOINT` theo route `/api/v1/tickets...`) trong 300 giây. Bỏ qua lỗi 404 thông thường và các endpoint Admin không phải ticket. Không tự sinh alert cho event nghiệp vụ `SLA_BREACHED`.

**R3 — WAF (Optional).** Sheet T5-03 ghi "nếu kịp". Chưa triển khai vì chưa có schema/volume WAF được Khiêm chốt. Khi có dữ liệu WAF thực tế mới thống nhất `WAF_BLOCKED` và ngưỡng R3, không tự ghép vào R1/R2.

## 7.5. Event SLA_BREACHED — T5-05 Trí

Theo SRS P2: worker kiểm tra ticket Active mỗi 5 phút; `Normal` dưới 80%, `Near Breach` từ 80% đến 100%, `Breached` khi **vượt 100%**. `SLA_BREACHED` chỉ phát khi **lần đầu** đổi `is_sla_breached` từ false sang true, theo `logAfterCommit(...)`. Lần quét tiếp theo không phát lại vì cờ đã true. Khi Reopen được duyệt, hệ thống tính hạn xử lý mới theo SRS BR-HD-03 nhưng **giữ nguyên cờ vi phạm cũ** nếu đã có.

```json
{
  "timestamp":"2026-11-17T02:33:00Z",
  "event_type":"SLA_BREACHED",
  "actor_id":null,
  "role":null,
  "target_type":"TICKET",
  "target_id":"HD-2026-0009",
  "action":"MARK_SLA_BREACHED",
  "outcome":"SUCCESS",
  "before":{"is_sla_breached":false},
  "after":{"is_sla_breached":true},
  "src_ip":null,
  "correlation_id":"00000000-0000-4000-8000-000000000001",
  "http_status":null
}
```

Mẫu trên **hoàn toàn giả lập**; `MARK_SLA_BREACHED` là tên action **đề xuất** để Trí xác nhận, không phải action đã kiểm chứng trên VM.

## 7.6. Màn giám sát và quyền truy cập

Tool `src/monitoring/alert_view.py` đọc **file alert đã có sẵn**, xuất HTML tĩnh (không kết nối mạng, không gọi API) để hiển thị bảng thời gian/rule/severity/actor/IP/số lần/trạng thái. Actor và IP được thay mã HMAC ngẫu nhiên của mỗi lần xuất (không hiển thị danh tính gốc). Output HTML mặc định ghi **DEMO — DỮ LIỆU GIẢ LẬP**; khi đọc file lấy từ VM phải chọn `--source-kind lab` và tự ghi rõ nguồn, thời điểm, commit/tag; riêng HTML không chứng minh pipeline live.

Nếu sau này nối vào FE `AuditLogsPage` của Chính, Backend phải cung cấp API chỉ đọc có RBAC, không được trả file log/alert gốc cho người không có quyền. Không coi trang FE hiện có là đã tích hợp thực tế nếu chưa chứng minh.

## 7.7. Lưu trữ, xoay vòng và rủi ro

- **Đề xuất** lưu Audit Log/Alert Log 30 ngày ở lab, xoay vòng hằng ngày; cần nhóm duyệt và Nhi xác nhận cấu hình Docker volume/logrotate, quyền truy cập, backup và nguồn dữ liệu. **Hiện chưa có bằng chứng retention/rotation thực thi**.
- R-01: phương án **JSONL + Python** đã có source trên main, sidecar đã chạy/đọc được log từ VM theo evidence 16/11. Có thể xin đóng **rủi ro lựa chọn công nghệ** sau khi owner chấp thuận; lưu issue riêng cho bước ghi alert live và retention. Không đánh dấu pipeline live là Done khi chưa có bằng chứng.
- Monitoring vẫn là prototype lab: trạng thái đếm trong RAM, không có checkpoint bền; restart hoặc rotation có thể bỏ sót hoặc lặp cảnh báo. Đưa điểm này vào Risk/Defect Log, không ghi đạt production-hardening.

## 7.8. Traceability và điều kiện nghiệm thu

| Yêu cầu | Test | Evidence | Trạng thái 17/11 |
|---|---|---|---|
| IA-HD04, NFR-05 | SEC-TC-HD-04, Audit Before/After | EV-HD-24 | BEFORE có; AFTER trên MVP mới chưa chạy |
| IA-HD05, HD01 | SEC-TC-HD-02 AuthN/lockout | EV-HD-22 | HTTP/JWT 10 PASS và R1 offline PASS (14/11); lockout/429/token expiry chưa đủ |
| IA-HD05 | Monitoring sidecar | EV-HD-22 | Container chạy/đọc log từ Nhi (16/11); live alerts.jsonl **chưa xác minh** |
| HD11, HD12, BR-HD-03 | TC-HD-11/12, SEC-TC-HD-04 | EV-HD-11/12, EV-HD-24 | Đợi Trí phát event `SLA_BREACHED` trên build mới |
| IA-HD06/07 (Optional) | R3 WAF | EV-HD-28 phần WAF | Chờ Khiêm xác nhận schema/nguồn |

**Ngày tạo bản đề xuất:** 17/11/2026. Người rà soát: Huỳnh Minh Hòa (IA). Tài liệu **chưa thay thế** SDD chính thức cho đến khi nhóm đồng bộ.
