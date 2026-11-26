# Finding Before và kế hoạch xử lý

| BUG_ID | Severity | Owner | Status | Description | Evidence |
| --- | --- | --- | --- | --- | --- |
| BUG-HD-008 | Low (theo Defect Log hiện có) | Trí sửa; Khánh retest | BEFORE reproduced; fix already on main, AFTER still required | Password change200, old token GET auth/me200 instead of401 | Test_Cases.csv T616-AUTH-09; EV-HD-27_VA_before_ubuntu_20261126/http-results.json |
| BUG-HD-009 | Medium (ZAP; risk review pending) | Chính/Nhi; Cường FE | Open hardening finding | CSP allows style-src unsafe-inline; no XSS exploit demonstrated | ZAP_Triage.csv rule10055-6; EV-HD-27_VA_before_ubuntu_20261126/zap/baseline-anon.html |
| BUG-HD-010 | Medium (ZAP; risk review pending) | Cường/Chính; Nhi review | Open hardening finding | External Google Fonts CSS has no integrity; self-host/pin suggested, no CDN compromise demonstrated | ZAP_Triage.csv rule90003; EV-HD-27_VA_before_ubuntu_20261126/zap/baseline-anon.html |
| BUG-HD-011 | Low (ZAP) | Chính/Nhi; Trí backend | Open hardening finding | API returns two HSTS headers; one authoritative TLS-gateway policy required | ZAP_Triage.csv rule10035-3; EV-HD-27_VA_before_ubuntu_20261126/api-header-check.txt |

BUG-HD-009/010/011 đã được Khiêm duyệt xuất bản trong gói VA Before ngày 26/11, đối chiếu Defect Log gốc001–008; đã có trong tài liệu04 và evidence trên main. Severity/risk review, owner tiếp nhận và kết quả AFTER vẫn cần nhóm xác nhận. Không tự coi owner đã nhận hoặc nhóm đã chấp nhận residual risk. T6-02 phải retest trên build AFTER thực tế.

## Bổ sung ngày 26/11 sau review

BUG-HD-012: mật khẩu cố định trong luồng Admin reset, giao diện và tài liệu seed. Đọc mã xác nhận UserService.java dùng fallback cố định khi đầu vào trống; UsersPage.jsx có giá trị mặc định hiển thị trong source, tools/generate_seed_users.py và evidence/T3-06 còn ví dụ cũ. Chưa kiểm HTTP để xác nhận bundle được tải trước đăng nhập, chưa chứng minh khai thác trên VM. Medium đề xuất, chờ Trí/Cường triage và xử lý; Khánh retest. Liên quan IA-HD07/SEC-TC-HD-08.3 và quản lý tài khoản. Chưa fix, chưa Accepted và chưa có AFTER.

Chi tiết: [BUG-HD-012](BUG-HD-012_Fixed_Credential.md).
