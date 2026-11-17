# T5-03 — Monitoring v2: cài, chạy, kiểm thử, review

**Dành cho nhánh Hòa**, không tự thay đổi nhánh `main`/VM chung. Căn cứ repo `Tam3332/P01-IT-Helpdesk`, `main` tại thời điểm review 17/11/2026 là `26ed05d` (PR #22 và #27 đều đã merge).

## Các file code

- `src/monitoring/monitor.py`: mở rộng R1 cho lockout tuần 5 (tên `ACCOUNT_LOCKED` / `BRUTE_FORCE_DETECTED`) và giữ R2 tương thích.
- `src/monitoring/tests/test_week5_lockout.py`: 10 kiểm thử mới cho 401/429/lockout, phân nhóm, chống duplicate và R2.
- `src/monitoring/alert_view.py`: xuất bảng alert dạng HTML offline, thay tên actor/IP bằng HMAC pseudonym, chống HTML injection.
- `src/monitoring/tests/test_alert_view.py`: 3 test mới của dashboard.
- `src/monitoring/audit_schema_check.py` và `tests/test_audit_schema_check.py`: 8 test kiểm tra schema Audit.

- `src/monitoring/examples/audit.week5.synthetic.jsonl`: bộ log **giả lập** mô phỏng Backend lockout, IDOR và SLA.

## Kiểm thử trước khi tạo PR

Mở PowerShell tại `E:\Projects\P01-IT-Helpdesk`, chỉ khi đã có các file mới trên nhánh của Hòa:

```powershell
py -m unittest discover -s src/monitoring/tests -v
py src/monitoring/monitor.py --input src/monitoring/examples/audit.week5.synthetic.jsonl --output "$env:TEMP\p01_alerts_week5_synthetic.jsonl"
py src/monitoring/alert_view.py --input "$env:TEMP\p01_alerts_week5_synthetic.jsonl" --output "$env:TEMP\p01_alert_dashboard_demo.html" --source-kind synthetic
Start-Process "$env:TEMP\p01_alert_dashboard_demo.html"
git diff --check
```

**Kết quả đã chạy trong bản giao ngày 17/11:** 39/39 test PASS; bộ 10 log giả lập tạo 2 cảnh báo (R1=1, R2=1), HTML tạo thành công. Test offline không thay thế test VM.

## Kiểm tra schema Audit cho SEC-TC-HD-04

File `src/monitoring/audit_schema_check.py` chạy offline trên bản log JSONL được phép đọc, chỉ xuất tổng hợp có SHA-256, số sự kiện, số lỗi schema; **không xuất dòng log/actor/IP/email/ticket gốc**. Ví dụ PowerShell:

```powershell
py src/monitoring/audit_schema_check.py --input "D:\duongdan\audit_events.jsonl" --output "$env:TEMP\EV-HD-24_schema_summary.md" --mode before
```

Khi đã có log từ build MVP mới, chuyển `--mode after`, đối chiếu với thao tác và commit thực tế. File báo cáo đầu ra **không chứng minh alert live**. Bộ 81 event trước đây có 2 event Reopen thiếu before/after, 3 SLA có outcome BREACHED và thiếu false→true, đã lưu tổng hợp redacted ở `evidence/T5-03/EV-HD-24_schema_CHECK_BEFORE_existing_81events.md`.

## Rule R1 — lưu ý đồng bộ Backend

Backend T5-06 có chuỗi 4×`LOGIN_FAILED/401` rồi 1×`LOGIN_FAILED/429` và 1×event khóa login. R1 v2 **bỏ qua 429** và nhận 1 event `ACCOUNT_LOCKED` **hoặc** `BRUTE_FORCE_DETECTED` làm mốc thứ 5 nếu trước đó đã đọc đủ 4 lần 401; không tạo cảnh báo cho 429 lặp. Cần Cường xác nhận tên event cuối cùng trước khi đóng SDD §7 v2, và Trí/Nhi đưa build tương ứng lên VM.

`--follow` chỉ xử lý dòng ghi sau lúc watcher chạy. Nếu restart sidecar giữa chuỗi đăng nhập sai, R1 không có đủ bốn event 401 để chứng minh ngưỡng và sẽ không giả định đã thấy đủ 5. Vì vậy kiểm thử live phải tạo các event **sau** khi sidecar đã chạy, trên tài khoản lab có quyền thử nghiệm.

## Xuất màn giám sát (HTML tĩnh)

```powershell
py src/monitoring/alert_view.py --input "D:\duongdan\alerts.jsonl" --output "D:\duongdan\alerts_report.html" --source-kind lab
```

Chỉ chạy với bản sao `alerts.jsonl` đã được phép trích xuất. Không đưa log gốc, email, token, IP gốc hoặc source audit vào GitHub. Report có pseudonym actor/IP; ghi commit/tag, thời điểm và nguồn evidence riêng ngoài HTML.

## Quy trình test trên VM (chờ owner phối hợp)

- Có build main MVP mới, xác nhận commit/tag triển khai, `src/monitoring/monitor.py` v2 được mount đúng và sidecar đã **restart sau cập nhật code**.
- Xác minh write path `/alerts/alerts.jsonl` có thể ghi (không chỉ dòng startup). Không tự thay đổi Docker Compose hoặc VM dùng chung nếu chưa được Nhi/Chính đồng ý.
- Tạo riêng chuỗi login sai đã duyệt với tài khoản thử nghiệm, đối chiếu HTTP, log Backend, event khóa, alert R1 xuất hiện mới với thời gian/correlation phù hợp; không dùng dữ liệu nhạy cảm trong evidence.
- Tạo ca `AUTHZ_DENIED` theo ticket lab để kiểm tra R2; so với Audit/SLA event có `before/after` sau commit.
- Hoàn tất JWT expiry, Nginx 429, unlock sau 15 phút, event upload/comment/role/SLA. Ghi Actual/Pass-Fail/EV vào 04 §3; FAIL đưa vào 04 §4 Defect Log.

## Các giới hạn chưa tự nhận hoàn thành

Chưa có quyền vận hành VM, chưa thấy trực tiếp `alerts.jsonl` live, chưa có build MVP của Trí/Cường đang chạy trên VM. Chưa triển khai R3 vì log WAF chưa chốt. Retention 30 ngày và rotation vẫn là đề xuất cần nhóm phê duyệt. Không nâng trạng thái T5-03 hoặc T5-10 thành Done chỉ vì test offline đã PASS.
