# Finding Before và kế hoạch xử lý

| BUG_ID | Severity | Owner | Status | Description | Evidence |
| --- | --- | --- | --- | --- | --- |
| BUG-HD-008 | Low (theo Defect Log hiện có) | Trí sửa; Khánh retest | BEFORE reproduced; fix already on main, AFTER still required | Password change200, old token GET auth/me200 instead of401 | Test_Cases.csv T616-AUTH-09; EV-HD-27_VA_before_ubuntu_20261126/http-results.json |
| BUG-HD-009 | Medium (ZAP; risk review pending) | Chính/Nhi; Cường FE | Open hardening finding | CSP allows style-src unsafe-inline; no XSS exploit demonstrated | ZAP_Triage.csv rule10055-6; EV-HD-27_VA_before_ubuntu_20261126/zap/baseline-anon.html |
| BUG-HD-010 | Medium (ZAP; risk review pending) | Cường/Chính; Nhi review | Open hardening finding | External Google Fonts CSS has no integrity; self-host/pin suggested, no CDN compromise demonstrated | ZAP_Triage.csv rule90003; EV-HD-27_VA_before_ubuntu_20261126/zap/baseline-anon.html |
| BUG-HD-011 | Low (ZAP) | Chính/Nhi; Trí backend | Open hardening finding | API returns two HSTS headers; one authoritative TLS-gateway policy required | ZAP_Triage.csv rule10035-3; EV-HD-27_VA_before_ubuntu_20261126/api-header-check.txt |

BUG-HD-009/010/011 là mã đề xuất cho ba finding mới sau đối chiếu Defect Log trên main ngày26/11 (mã hiện có001–008), chờ người dùng duyệt trước khi publish. Không tự coi owner đã nhận hoặc nhóm đã chấp nhận residual risk. T6-02 phải retest trên build AFTER thực tế.
