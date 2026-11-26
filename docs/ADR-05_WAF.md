# ADR-05 — WAF tại gateway P01

Ngày cập nhật: 26/11/2026. Người phụ trách: Nguyễn Trọng Khiêm · SE192700.
Trạng thái: **Draft của phần hoàn thiện T6-15; chờ review và nghiệm thu runtime**.

## Căn cứ và quyết định

[PR #38](https://github.com/Tam3332/P01-IT-Helpdesk/pull/38) đã merge tại `38a5a29400feeb4ba164b85448e920e140a19a5b`. Cấu hình gốc dùng DetectionOnly, pin digest image, TLS 1.2/1.3, header bảo mật, rate limit, giới hạn 11m, volume audit riêng. Tag v0.6-rc1 `ed229a3e1362333416822b2866ae7597a15ce917` có trước WAF; không đổi nội dung tag để gọi đó là build WAF. Nguồn: [docker-compose.yml](https://github.com/Tam3332/P01-IT-Helpdesk/blob/38a5a29400feeb4ba164b85448e920e140a19a5b/docker-compose.yml), [nginx/nginx-waf.conf](https://github.com/Tam3332/P01-IT-Helpdesk/blob/38a5a29400feeb4ba164b85448e920e140a19a5b/nginx/nginx-waf.conf).

Bản hoàn thiện giữ cấu hình đã merge và ghi tường minh ngưỡng inbound 5/outbound 4 qua biến môi trường. Đây là **đề xuất lab PL1**, chưa thay chữ ký duyệt SecRS §7. Giá trị mặc định được đối chiếu với [biến chính thức của CRS Docker](https://github.com/coreruleset/modsecurity-crs-docker#readme); image pinned vẫn cần kiểm tra giá trị render thực tế.

Gateway trả JSON lỗi 403 thân thiện bằng handler nội bộ chỉ kết thúc phản hồi. Giữ `proxy_intercept_errors off` để lỗi từ upstream đi qua; không đồng nhất 403 RBAC của backend với WAF. `GATEWAY_FORBIDDEN` là mã giao diện, **không đủ chứng minh WAF đã chặn** nếu thiếu audit cùng request. Thiết kế dựa trên [Nginx error_page](https://nginx.org/en/docs/http/ngx_http_core_module.html#error_page) và [proxy_intercept_errors](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_intercept_errors); tương tác ModSecurity trên image thật chưa kiểm chứng.

## Triển khai và điều kiện đổi chế độ

1. Nhi review patch sau PR #38 và ngoại lệ control WAF của T6-08; Chính ghi SHA, digest, project/volumes, cấu hình và giờ triển khai. Không dùng thư mục week4 lịch sử làm nơi thử patch mới.
2. Bắt đầu DetectionOnly; kiểm P1, P3 approve/reject, upload gần giới hạn, nội dung có ký tự hợp lệ và JSON. Ghi cảnh báo nhầm theo request ID; chưa có evidence thì chưa tạo exclusion.
3. Chỉ đổi On theo scope, lịch, ngưỡng và người chạy được duyệt; nghiệm thu SEC-TC-HD-07 ở cả hai chế độ, regression luồng hợp lệ và UI 403. Hồ sơ này không cấp quyền chạy quét.
4. Nếu chưa xử lý được false positive trước mốc nhóm chốt, giữ DetectionOnly và ghi chấp nhận rủi ro trong 02 §8; không tuyên bố đã bật chặn.

## Audit và R3

Giữ RelevantOnly/Serial/JSON, parts AZ, volume `waf_audit_logs`, đường dẫn `/var/log/modsecurity/audit/waf-audit.jsonl`. AZ là lựa chọn giảm dữ liệu, **chưa chứng minh log runtime sạch hoặc đủ trường rule ID cho R3**. Không tăng parts lấy Authorization, Cookie, body đăng nhập để đủ parser. Hoà/Nhi phải chốt trường tối thiểu, mapping và mẫu đã che. `WAF_BLOCKED` là event chuẩn hóa cần được bên Monitoring xác nhận, không mặc định có trong log native. Xem `evidence/T6-15/AUDIT_HANDOFF.md`.

## Giới hạn và rollback

Đã có nginx -t trên Ubuntu cá nhân ở hai chế độ sau khi dùng TLS staging đúng UID; 857 rule nạp thành công. Lượt đầu FAIL do key root:root0600. Chưa có startup/HTTPS/audit privacy/tài nguyên/UI trên VM nhóm cho patch mới. Xem evidence/T6-15/EV-HD-28_WAF/ubuntu-personal-20261126/README.md và TLS_DEPLOYMENT_NOTE.md; không chuyển kết quả cá nhân thành nghiệm thu nhóm. Frontend hiện phát sự kiện forbidden với mọi 403 rồi đọc message; hành vi màn hình cần kiểm riêng ([src/frontend/src/services/apiClient.js](https://github.com/Tam3332/P01-IT-Helpdesk/blob/38a5a29400feeb4ba164b85448e920e140a19a5b/src/frontend/src/services/apiClient.js#L15-L22), [src/frontend/src/services/apiClient.js](https://github.com/Tam3332/P01-IT-Helpdesk/blob/38a5a29400feeb4ba164b85448e920e140a19a5b/src/frontend/src/services/apiClient.js#L37-L50)). Không đổi frontend ngoài phạm vi patch này.

Rollback do Chính/Nhi chọn theo project và volume triển khai thực tế: quay lại cấu hình gateway được lưu trước thay đổi hoặc giữ DetectionOnly theo quyết định rủi ro. Không `down -v`, không xóa volume hay đổi DB/tag để xử lý gateway. Đo CPU/RAM trên VM nhóm; số 64 MiB PoC cá nhân không thay số đo nhóm.

## Xác nhận còn chờ

Nhi review: Chưa có. Khánh xác nhận ngưỡng/lịch/scope: Chưa có. Chính deploy manifest: Chưa có. Hoà R3/schema: Chưa có. Trí đưa ADR vào SDD §9, WAF vào §8: Chưa có. SecRS §7, 04 §3 và EV-HD-28_WAF: chờ bằng chứng và người duyệt, chưa ghi Approved/Done.

## Bổ sung chuẩn bị bàn giao cho Chính ngày 26 tháng 11 năm 2026

Follow-up dựa main c61ccb2124a8e77529db815a21e72d00d2380cda. Cấu hình cho phép WAF_TLS_DIR để dùng bản sao TLS riêng đúng UID/GID của image, giữ key cũ cho rollback. Access log JSON chỉ gồm thời gian, request ID, source IP, method, status và upstream status; bỏ query, body, User-Agent/header. Gateway gắn X-Request-Id trên mọi phản hồi và trả JSON403 có cùng ID; chặn Actuator404 cùng API docs. Privacy vẫn phải rà cả error log và stdout/stderr của ModSecurity, vì audit AZ không bảo đảm các kênh này sạch.

scripts/lab-check.sh mặc định kiểm cổng container WAF8443; baseline cũ dùng GATEWAY_CONTAINER_PORT=443. scripts/waf/preflight.py đọc stack, phát hiện sai project/compose files, kiểm SHA sạch và chính sách; snapshot riêng pin image cũ, env, network/volume và config để Chính rollback. scripts/waf/smoke.py là 11 GET-only probe, không thay nghiệm thu P1/P3/upload/UI. Kết quả tooling mới được kiểm riêng trong PREDEPLOY_VERIFICATION.md; ảnh syntax cũ chỉ thuộc config cũ.

Hướng dẫn triển khai và rollback: evidence/T6-15/DEPLOY_CHINH.md. Bản này tiếp tục Draft/chờ Nhi review và VM nhóm; chưa ký SecRS Approved, chưa xác nhận R3 live. Nếu scanner User-Agent chưa đủ anomaly score để bị chặn, giữ kết quả thật và quyết định policy hẹp hoặc risk, không tự hạ threshold. Các bản tài liệu 02/03/04 cập nhật trạng thái chuẩn bị, giữ nguyên dữ kiện Before và các finding đang mở.
