# Audit WAF → Hoà / rule R3

Nguồn cấu hình: [docker-compose.yml](https://github.com/Tam3332/P01-IT-Helpdesk/blob/38a5a29400feeb4ba164b85448e920e140a19a5b/docker-compose.yml), [nginx/nginx-waf.conf](https://github.com/Tam3332/P01-IT-Helpdesk/blob/38a5a29400feeb4ba164b85448e920e140a19a5b/nginx/nginx-waf.conf#L36-L40). Đây là hợp đồng cần xác nhận, chưa phải sample/parser đã chạy.

| Trường chuẩn hóa đề nghị | Căn cứ / điều kiện |
|---|---|
| event = WAF_BLOCKED | Chỉ tạo khi audit native chứng minh WAF can thiệp và có status tương ứng; HTTP 403 riêng lẻ không đủ |
| timestamp | Thời gian native có timezone; không tự giả thành thời gian hiện tại |
| request_id | Đối chiếu ID giao dịch native với `$request_id` gateway; cần sample thực |
| http_status, interrupted | Quan sát audit native của cùng request; thiếu trường thì đánh thiếu, không điền từ giả định |
| build, environment, waf_mode | Run manifest, không suy ra từ tên container hay branch |
| rule_id | Nếu AZ không chứa thì ghi unavailable; Hoà/Nhi chốt cách chẩn đoán đã che, không tự bật H/B/C để lấy secret |
| route/method tối thiểu | Chỉ giữ nếu đã được duyệt và làm sạch query/PII; không chuyển body/header nguyên bản |

Không đưa Authorization, Cookie, Set-Cookie, mật khẩu, JWT, body đăng nhập, secret vào repo. Cần kiểm cả WAF audit, access/error log và ảnh màn hình; AZ không bảo đảm các kênh khác sạch. Raw giữ riêng, bản share do Nhi/Hoà rà.

Mẫu cá nhân 19/11 có HTTP 200 và interrupted=false chỉ chứng minh audit smoke ở PoC. Không dùng làm WAF_BLOCKED/R3 hoặc evidence VM nhóm. Hiện không có sample chặn mới cho bản follow-up.

## Nguồn metadata bổ sung cho Hoà

Access log gateway mới ở `/var/log/nginx/access.log` là JSON Lines, volume `nginx_logs`, gồm `timestamp`, `request_id`, `src_ip`, `method`, `uri`, `http_status`, `upstream_status`. Header `X-Request-Id` và JSON403 body `requestId` dùng cùng ID gateway; `modsecurity_transaction_id` cũng dùng `$request_id`. Cần đối chiếu mẫu native thật để xác nhận ID trong audit AZ. Không bật H/B/C để tìm dữ liệu thiếu.

Đề nghị record chuẩn hóa: `event_type=WAF_BLOCKED`, timestamp UTC từ nguồn đã xác minh, correlation_id/request_id, src_ip đã được duyệt chia sẻ, http_status403, interrupted=true có căn cứ native, build SHA, environment, waf_mode; rule_id chỉ khi có nguồn. Những trường bắt buộc thiếu thì đánh `INSUFFICIENT_EVIDENCE`, không phát WAF_BLOCKED. `upstream_status` trống chỉ là tín hiệu gateway chưa proxy, chưa đủ chứng minh WAF can thiệp. File parser Monitoring hiện hành chưa có R3; Hoà phải review/tích hợp và chạy live trước khi đóng WAF-13. Giữ R1/R2 và volume audit backend hiện hành.

Raw audit và error/container log giữ ngoài Git, quyền riêng. `scripts/waf/privacy_check.py` chỉ dò pattern/canary, không tự che log hoặc chứng minh mọi dữ liệu riêng tư đã sạch. Tạo sample đã che từ lượt nhóm rồi ghi source hash, request ID và mapping cho Hoà; gói hiện không dựng sample block giả. Không có alert live thì R3 tiếp tục NOT RUN.

Đề xuất bổ sung sau rà soát: `uri` lấy từ `$uri`, không dùng `$request_uri` hoặc `$args`. Nó hỗ trợ tìm route, không thay bằng chứng native interruption/rule. Rà mã đối tượng và PII trong path trước khi chia sẻ. Gói bổ sung chưa được xuất bản.
