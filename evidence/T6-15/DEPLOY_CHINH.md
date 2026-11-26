# Chính triển khai WAF T6 15

Gói hoàn thiện của Khiêm cho Nhi review và Chính triển khai gateway trên VM nhóm. Giữ project, database, uploads, audit backend và các container ứng dụng đang chạy. Lượt đầu dùng DetectionOnly; chuyển On sau lượt hợp lệ và review false positive.

## 1 Chuẩn bị và chụp trạng thái trước cập nhật

Nhi review diff, ADR-05 và ngưỡng PL1 inbound 5/outbound 4. Chính lấy **SHA đầy đủ của commit đã review**, không dùng tên nhánh làm định danh build và không gắn lại tag v0.6-rc1 cũ. PR #38 đã merge; phần này là follow-up.

Lệnh bên dưới chạy trong Bash Ubuntu bằng tài khoản có quyền Docker. Nếu dùng sudo, giữ cùng quyền và cùng biến môi trường cho toàn bộ lệnh; đừng trộn `sudo docker` với wrapper khác. Đọc project và danh sách compose file từ nhãn container đang chạy:

```bash
docker inspect p01-gateway --format '{{index .Config.Labels "com.docker.compose.project"}}'
docker inspect p01-gateway --format '{{index .Config.Labels "com.docker.compose.project.working_dir"}}'
docker inspect p01-gateway --format '{{index .Config.Labels "com.docker.compose.project.config_files"}}'
```

Điền đúng giá trị vừa đọc. `DC` phải liệt kê **mọi file compose/override của stack hiện hành theo đúng thứ tự**, kể cả monitoring nếu nhãn có ghi. Không đổi tên project hoặc bỏ override. Nếu thư mục live còn thay đổi chưa commit, dừng để giữ và đối chiếu thay đổi trước.

```bash
export PROJECT='ten-project-vua-doc'
export LIVE='/duong/dan/repo-dang-chay'
export REVIEWED_SHA='SHA-40-ky-tu-da-review'
export HANDOFF='/duong/dan/ban-checkout-goi-T6-15'
export PRIVATE="$HOME/p01-waf-private/$(date -u +%Y%m%dT%H%M%SZ)"
umask 077
mkdir -p "$PRIVATE"
cd "$LIVE"
git status --short
# Ví dụ stack chỉ có base; thêm -f <override> và --compose-file <override> nếu nhãn có.
DC=(docker compose --project-directory "$LIVE" -p "$PROJECT" -f "$LIVE/docker-compose.yml")
CF=(--compose-file "$LIVE/docker-compose.yml")
python3 "$HANDOFF/scripts/waf/preflight.py" snapshot --repo "$LIVE" --project "$PROJECT" "${CF[@]}" --output "$PRIVATE/before"
```

Snapshot chạy **trước khi checkout/sửa nguồn gateway**. Nó lưu image ID đang chạy, biến môi trường gateway, mạng/volume thực và bản sao file cấu hình bind vào `rollback.compose.json`. Snapshot có thể chứa secret của gateway, chỉ giữ riêng. Các thư mục bind như TLS được giữ tại đường dẫn gốc; bước tiếp theo dùng TLS riêng nên không đổi key cũ. Nếu gateway có bind directory cấu hình ngoài TLS, sao lưu riêng và xác nhận rollback trước khi cập nhật.

## 2 Cập nhật đúng bản và chuẩn bị TLS

Nguồn cần có sẵn trên remote sau review, hoặc áp patch trong checkout riêng rồi commit/review trước. Chính cập nhật checkout live sạch đến đúng commit, không dùng `reset --hard` để xóa thay đổi. Giữ `.env` hiện hành, không chép đè bằng `.env.example`.

```bash
git fetch origin
git switch --detach "$REVIEWED_SHA"
test "$(git rev-parse HEAD)" = "$REVIEWED_SHA"
IMAGE='owasp/modsecurity-crs@sha256:c95914e1855c41791e6745c2a63864a218f99e04948b64d8b31cb7b29b1ba5b3'
docker pull "$IMAGE"
WAF_UID=$(docker run --rm --network none --entrypoint id "$IMAGE" -u)
WAF_GID=$(docker run --rm --network none --entrypoint id "$IMAGE" -g)
```

Tạo bản sao TLS trong thư mục **mới** ngoài Git. `OLD_TLS` là bind source của chứng chỉ đang chạy (đọc `docker inspect`), không đoán theo week4. Lệnh `sudo install` thực thi trên host Docker; UID/GID phải tương ứng bind mount của Docker đó. Nếu Docker rootless/user namespace, Nhi điều chỉnh mapping rồi kiểm khả năng đọc bằng container, không dùng chmod 644 cho key.

```bash
export OLD_TLS='/duong/dan/TLS-dang-chay'
export WAF_TLS_DIR="$HOME/p01-waf-tls-$(date -u +%Y%m%dT%H%M%SZ)"
test ! -e "$WAF_TLS_DIR"
sudo install -d -o "$WAF_UID" -g "$WAF_GID" -m 0700 "$WAF_TLS_DIR"
sudo install -o "$WAF_UID" -g "$WAF_GID" -m 0400 "$OLD_TLS/server.key" "$WAF_TLS_DIR/server.key"
sudo install -o "$WAF_UID" -g "$WAF_GID" -m 0444 "$OLD_TLS/server.crt" "$WAF_TLS_DIR/server.crt"
export WAF_RULE_ENGINE=DetectionOnly WAF_ANOMALY_INBOUND=5 WAF_ANOMALY_OUTBOUND=4
```

Ghi bốn biến WAF và đường dẫn TLS vào `.env` **cục bộ** để những lần Compose tiếp theo không trở về cấu hình cũ. Chỉnh đúng dòng, không thay các secret. Tránh `set -x`, không chia sẻ `docker compose config` đầy đủ, `docker inspect` đầy đủ hoặc `.env`.

## 3 Kiểm tra rồi thay riêng gateway

```bash
python3 scripts/waf/preflight.py check --repo "$LIVE" --project "$PROJECT" "${CF[@]}" --sha "$REVIEWED_SHA" --output "$PRIVATE/preflight"
"${DC[@]}" run --rm --no-deps -T nginx nginx -t >"$PRIVATE/nginx-t.private.txt" 2>&1
# Chỉ chạy tiếp nếu nginx-t exit 0. Không dùng --build, không down stack.
"${DC[@]}" up -d --no-deps --no-build --pull never nginx
"${DC[@]}" exec -T nginx nginx -t >"$PRIVATE/nginx-t-live.private.txt" 2>&1
python3 scripts/waf/preflight.py check --repo "$LIVE" --project "$PROJECT" "${CF[@]}" --sha "$REVIEWED_SHA" --output "$PRIVATE/after-detection"
"${DC[@]}" exec -T nginx sh -c 'grep -E "^[[:space:]]*(SecRuleEngine|SecAuditEngine|SecAuditLogParts|SecAuditLogFormat|SecAuditLogType) " /etc/modsecurity.d/modsecurity.conf' >"$PRIVATE/rendered-policy.private.txt"
```

Chạy từng bước và dừng ngay khi lệnh trả khác 0. Đường dẫn file render phụ thuộc image: nếu grep không tìm thấy, xem include của `setup.conf` riêng trên VM rồi ghi đúng file. Đối chiếu manifest trước/sau: ID/image/start time của postgres/backend/frontend phải giữ nguyên. Kiểm thêm giá trị rule engine, PL1 và anomaly 5/4 trong file render thực; biến môi trường riêng lẻ chưa chứng minh policy đã được nạp.

## 4 Kiểm DetectionOnly rồi On

Chính mở đúng HTTPS_PORT trong `.env` (host port khác cổng 8443 nội bộ). Dùng chứng chỉ CA tin cậy với `--ca-file`, hoặc `--insecure-lab` riêng lab tự ký. Script chỉ gửi 11 GET không đăng nhập, không ghi dữ liệu seed, không đi theo redirect.

```bash
export LAB_ORIGIN='https://dia-chi-VM:cong-HTTPS'
python3 scripts/waf/smoke.py --base-url "$LAB_ORIGIN" --mode DetectionOnly --sha "$REVIEWED_SHA" --run-id T615-DETECTION --insecure-lab --output "$PRIVATE/smoke-detection.json"
# lab-check dùng COMPOSE_PROJECT_NAME/COMPOSE_FILE để trùng đúng stack DC ở trên.
export COMPOSE_PROJECT_NAME="$PROJECT"
# Nếu có nhiều compose file, nối đường dẫn bằng dấu : trên Linux.
export COMPOSE_FILE="$LIVE/docker-compose.yml"
sh scripts/lab-check.sh CONG_HTTPS_CUA_VM >"$PRIVATE/lab-check.private.txt" 2>&1
```

Khánh/SE chạy P1/P3 approve/reject, upload gần 10 MB, nội dung tiếng Việt/ký tự/code hợp lệ và JSON theo [TEST_GUIDE.md](TEST_GUIDE.md). Điền Actual/EV vào checklist. Với luồng hợp lệ, giữ đúng status nghiệp vụ (200/201...), không chấp nhận WAF403. Exclusion chỉ thêm khi có rule/route/parameter/evidence/lý do và Nhi review; giữ bảng trống nếu chưa có ca nhầm đã xác minh.

Khi đã có kết quả và Nhi xác nhận chuyển chế độ, sửa `WAF_RULE_ENGINE=On` trong `.env` và export cùng giá trị. Giữ nguyên SHA và ngưỡng:

```bash
export WAF_RULE_ENGINE=On
"${DC[@]}" run --rm --no-deps -T nginx nginx -t >"$PRIVATE/nginx-t-on.private.txt" 2>&1
"${DC[@]}" up -d --no-deps --no-build --pull never nginx
python3 scripts/waf/preflight.py check --repo "$LIVE" --project "$PROJECT" "${CF[@]}" --sha "$REVIEWED_SHA" --output "$PRIVATE/after-on"
python3 scripts/waf/smoke.py --base-url "$LAB_ORIGIN" --mode On --sha "$REVIEWED_SHA" --run-id T615-ON --insecure-lab --output "$PRIVATE/smoke-on.json"
"${DC[@]}" exec -T nginx nginx -t >"$PRIVATE/nginx-t-live-on.private.txt" 2>&1
```

Chạy lại toàn bộ ca hợp lệ, lab-check, UI403, RBAC403, privacy và R3 ở On. CRS có thể chấm User-Agent scanner dưới ngưỡng chặn: nếu không 403, giữ FAIL và rule/score thực để Nhi quyết định policy hẹp hoặc ghi residual risk; không hạ threshold/tắt rule để làm đẹp kết quả.

## 5 Audit tài nguyên và evidence

Lưu raw riêng, theo dõi kích thước và quyền volume. Với WAF RelevantOnly, request hợp lệ có thể không có audit; phải có request chặn đã đối chiếu để kiểm coverage. Không kết luận privacy PASS từ file rỗng.

```bash
"${DC[@]}" exec -T nginx cat /var/log/modsecurity/audit/waf-audit.jsonl >"$PRIVATE/waf-audit.private.jsonl"
"${DC[@]}" exec -T nginx cat /var/log/nginx/access.log >"$PRIVATE/nginx-access.private.txt"
"${DC[@]}" logs --no-color nginx >"$PRIVATE/nginx-container.private.txt" 2>&1
"${DC[@]}" exec -T nginx cat /var/log/nginx/error.log >"$PRIVATE/nginx-error.private.txt"
docker stats --no-stream --format '{{.Name}} {{.CPUPerc}} {{.MemUsage}}' p01-gateway >"$PRIVATE/stats.txt"
```

Dùng dữ liệu lab dùng một lần để gửi login có giá trị đánh dấu riêng, header Authorization/Cookie và nội dung đặc biệt. Ghi **các giá trị đánh dấu** vào `$PRIVATE/canaries.private.txt`, mỗi dòng ít nhất 8 ký tự. Không dùng tài khoản thật hoặc đổi secret đang chạy. Ghi lại request ID/giờ/case, rà đủ audit/access/error/container stdout/stderr. ModSecurity có thể ghi matched data vào error log dù audit parts AZ; nếu thấy leak, giữ FAIL và dừng xuất bản raw.

```bash
python3 scripts/waf/privacy_check.py --canary-file "$PRIVATE/canaries.private.txt" --input "$PRIVATE/waf-audit.private.jsonl" --input "$PRIVATE/nginx-access.private.txt" --input "$PRIVATE/nginx-error.private.txt" --input "$PRIVATE/nginx-container.private.txt" --output "$PRIVATE/privacy-check.json"
```

Audit hợp đồng và mapping Hoà ở [AUDIT_HANDOFF.md](AUDIT_HANDOFF.md). Không tạo WAF_BLOCKED chỉ từ HTTP403. Nếu AZ thiếu trạng thái/rule/intervention, ghi thiếu; Hoà/Nhi chốt nguồn chứng minh đã che trước khi đánh R3 PASS. Không bật body/header audit để đủ schema.

Ảnh thật và bản log đã che lưu `evidence/T6-15/EV-HD-28_WAF/<run-id>/`, kèm SHA/image ID, mode render, thời gian/múi giờ, người chạy, case và SHA256. Chụp đủ luồng hợp lệ/chặn/UI, không chụp JWT, mật khẩu hoặc key. CPU/RAM chụp trước deploy, lúc test và sau On; số PoC cá nhân không thay số nhóm.

## Rollback

Nếu On chặn nhầm nhưng gateway còn hoạt động, trả `WAF_RULE_ENGINE=DetectionOnly` trong `.env`, export cùng giá trị và `up -d --no-deps --no-build --pull never nginx`; chạy lại ca hợp lệ. Ghi quyết định rủi ro ở SecRS §8 nếu giữ DetectionOnly cho bản cuối.

Nếu cần khôi phục gateway trước thay đổi, snapshot tạo ở bước 1 pin **image ID cũ**, dùng lại đúng network/volume và bản sao config cũ:

```bash
# Trước đó đối chiếu đúng snapshot/project và xác nhận image ID cũ còn trong Docker.
docker compose -p "$PROJECT" -f "$PRIVATE/before/rollback.compose.json" config --quiet
docker compose -p "$PROJECT" -f "$PRIVATE/before/rollback.compose.json" up -d --no-deps --no-build --pull never nginx
# Kiểm HTTPS, login, luồng nghiệp vụ và container/volume ứng dụng giữ nguyên.
```

Không xóa image cũ/TLS gốc/snapshot trước nghiệm thu. Cấu hình rollback chỉ có gateway nên không dùng `--remove-orphans`. Sau rollback, giữ cấu hình live trỏ về snapshot cho đến khi Chính khôi phục nguồn/deploy theo bản đã review; đừng chạy Compose mới vô tình bật lại WAF. Lượt rollback vẫn cần chạy thực tế và ghi bằng chứng; snapshot đã chuẩn bị không tự chứng minh rollback PASS.

## Kết thúc bàn giao

Chính trả manifest trước/sau, nginx-t, mode render, smoke, lab-check, stats, log privacy đã rà và ảnh thật. Khánh trả 15 dòng Actual/EV; Hoà trả R3; Nhi ký review/ngưỡng; Trí gộp ADR và SDD. Khi đủ mới cập nhật T6-15 Done và Approved. Hiện trạng chuẩn bị là chờ review/deploy.
