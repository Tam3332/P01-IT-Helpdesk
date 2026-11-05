# Hòa — T3-03 / T3-10: kiểm tra khớp Backend và kết quả Monitoring v1

**Nguồn so sánh (06/11/2026):** repo `Tam3332/P01-IT-Helpdesk`, nhánh `tri/be` (`e9ed3fc`) và `cuong/be` (`279aade`, 06/11 chỉ thêm actuator health, AuditLogger không đổi), SRS HD01/UC-01/HD09, SecRS IA-HD04/05, SDD §7 và Test Plan SEC-TC-HD-02. **Mã nguồn nhánh `main` chưa chứa AuditLogger tại thời điểm so sánh.**

## Vấn đề đã sửa trong Monitoring (không cần đổi schema Backend)

1. **`login_ref` không có trong Backend:** Python v1 dùng `actor_id` cho tài khoản đã tồn tại, dùng hash của email trong `target_type=USER_EMAIL, target_id` cho email chưa có tài khoản. Cùng `actor_id + src_ip` trên `LOGIN_SUCCESS` đặt lại chuỗi đăng nhập sai. Không ghi email gốc trong file cảnh báo. `login_ref` v0 vẫn được chấp nhận cho log giả lập cũ.
2. **`actor_id` kiểu số:** Python chấp nhận `int` dương của Jackson, chuẩn hóa thành chuỗi cho R2 và alert. Không đếm `actor_id=null`, Boolean hay dữ liệu rỗng.
3. **Hai nguồn `AUTHZ_DENIED`:** `TicketService` ghi `target_type=TICKET`, 404 Anti-IDOR. `SecurityConfig` ghi `target_type=ENDPOINT`, 403; Python chỉ nhận ENDPOINT nếu đường dẫn là `/api/v1/tickets...`, tránh bỏ sót truy cập trái quyền liên quan ticket. Không đếm lỗi HTTP 404 thông thường hoặc endpoint Admin.
4. **Đăng nhập tài khoản bị khóa bởi Admin:** Backend phát `LOGIN_FAILED` với HTTP 403 dù mật khẩu đúng; rule R1 chỉ tính 401 sai thông tin, không coi 403 là một lần dò mật khẩu.
5. **Tên log/volume:** Backend mặc định `logs/audit_events.jsonl` (chạy trong `/app`), Docker Compose gốc đã gắn `audit_logs:/app/logs`, nên file log nằm ở `/app/logs/audit_events.jsonl`. File thêm `docker-compose.monitoring.yml` chỉ thêm service `monitoring`, **không sửa service backend**; Monitoring đọc volume ở chế độ read-only và ghi `alerts.jsonl` ở volume riêng. Nếu muốn khai báo rõ `AUDIT_LOG_FILE`, Chính thêm vào `docker-compose.yml` gốc. **Chỉ là cấu hình tích hợp đề xuất**, chưa chạy VM.

## Các quyết định KHÔNG thể tự xác nhận thay người phụ trách

- **IP sau reverse proxy:** Backend `AuditLogger` hiện lấy `request.getRemoteAddr()`. Khi chạy qua proxy, giá trị có thể là IP của Nginx. Đề xuất xử lý IP sau proxy đã gửi Cường (Backend) và Nhi (gateway), không đưa vào repo; Monitoring không sửa code Backend. Cần Cường/Nhi cấu hình trước khi kiểm thử thực tế.
- **Khóa 15 phút và 429:** `AuthService` đang ghi rõ để tuần 5; Monitoring không được tự thực hiện. SEC-TC-HD-02 phần này chưa thể PASS.
- **Ngưỡng R2 3 lần/5 phút, tool Python + JSONL, retention 30 ngày:** còn Proposed trong SecRS; Khiêm/Nhi/nhóm phê duyệt và cập nhật R-01 trước khi gọi Approved.
- **`before/after` cho sự kiện role, phân công/trạng thái:** không thuộc 4 audit event tuần 3; Cường/Trí cần triển khai khi tương ứng với các chức năng. Monitoring không tự bổ sung dữ liệu chưa tồn tại trong Backend.
- **Vận hành thực tế:** chưa có bằng chứng chạy trên VM Ubuntu, không tự điền `EV-HD-22`.

## Giả mã R1 — SDD §7 (tương thích code Backend)

```text
Với LOGIN_FAILED có http_status=401:
  nếu actor_id có: login_key = "actor:" + str(actor_id)
  nếu actor_id rỗng và target_type=USER_EMAIL:
       login_key = "email_sha256:" + SHA256(lowercase(trim(target_id)))
  key = (login_key, src_ip do Backend xuất)
  loại khỏi key những lần sai ngoài 5 phút
  thêm lần sai, khi số lần tăng từ 4 -> 5 tạo 1 R1 alert
Với LOGIN_SUCCESS: xóa chuỗi sai cho cùng (actor_id, src_ip)
Với LOGIN_FAILED/403 do Admin đã khóa: không cộng vào chuỗi dò mật khẩu
BRUTE_FORCE_DETECTED/HTTP 429, khóa tài khoản: do Backend xử lý khi triển khai; Monitoring không khóa
```

## Giả mã R2 — SDD §7 (ngưỡng Proposed)

```text
Với AUTHZ_DENIED do Backend phát, có actor_id hợp lệ:
   hợp lệ nếu target_type=TICKET, hoặc target_type=ENDPOINT và target_id trỏ tới /api/v1/tickets...
   không đếm endpoint Admin, 404 bình thường hay sự kiện không phải AUTHZ_DENIED
   key = str(actor_id)
   loại sự kiện ngoài 5 phút; khi số lần tăng 2 -> 3 tạo 1 R2 alert
```

## Kết quả kiểm thử offline v1 (không phải VM lab)

- `python -m unittest discover -s src/monitoring/tests -v`: **18/18 PASS** trên máy tạo gói.
- `audit.backend.sample.jsonl`: **12 sự kiện mẫu** theo cấu trúc Backend, **0 skipped, 2 alerts**.
- `audit.sample.jsonl` v0: dữ liệu mô phỏng cũ vẫn được hỗ trợ, không phá tương thích.
- Mã code cần review/commit trên nhánh Hòa; không tuyên bố đã có pull request hoặc đã merge.

## Việc gửi nhóm

1. Trí/Cường review R1 identity mapping, trường `actor_id` và hợp đồng AuditLogger; chốt xử lý `src_ip` cùng Nhi. Không yêu cầu Backend thêm `login_ref` chỉ để Python đọc được nữa.
2. Khánh review 403/404 và ENDPOINT ticket của R2.
3. Nhi/Chính review `docker-compose.monitoring.yml`, thiết lập proxy đáng tin, quyền đọc log/ghi alert và chạy VM.
4. Khiêm duyệt ngưỡng/tool/retention, cập nhật R-01, nhận PR; chuyển T3-03/T3-10 sang Review sau khi gửi được code/evidence; chỉ Done sau khi có bằng chứng phù hợp.

Không commit file `init_demo_users.sql`, token, mật khẩu, log thật hoặc `alerts.*.actual.jsonl`; giữ evidence offline phân biệt rõ với `EV-HD-22` chính thức.
