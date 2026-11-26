# VA v1 Before trên v0 5 mvp

Nguyễn Trọng Khiêm · 26/11/2026 · 35c573d20d4244985e617e8a056f880faaadf1db

Bản Before có một lỗi phiên đã biết BUG-HD-008 và hai nhóm thiếu sót hardening Medium mới. Bảng này phản ánh kết quả trên Ubuntu cá nhân; không gọi đây là VM nhóm hoặc Kali.

| VA_ID | Status | Actual | Severity_BUG | Owner | Cases |
| --- | --- | --- | --- | --- | --- |
| VA-AUTH-01 | PASS | GET tickets không token401 | — | Khánh | T616-AUTH-01 |
| VA-AUTH-02 | PASS | Role/sub sửa giữ chữ ký, alg none, chữ ký sai, expired đều401; fixture expiry còn hạn200 | — | Trí/Khánh | T616-AUTH-02..06 |
| VA-AUTH-03 | PASS | Logout200 rồi token cũ401 | — | Trí/Khánh | T616-AUTH-07 |
| VA-AUTH-04 | PASS | Email không tồn tại/sai mật khẩu đều401, cùng INVALID_CREDENTIALS/message | — | Trí/Khánh | T616-AUTH-08 |
| VA-AUTH-05 | CONFIRMED BEFORE | Đổi mật khẩu200 nhưng token cũ vẫn200; giữ BUG hiện có, không mở trùng | Low · BUG-HD-008 | Trí; Khánh retest | T616-AUTH-09 |
| VA-API-01 | PASS IN TESTED SCOPE | 4 role; 2 chủ ticket/2 assignee; ngoài scope404; Admin users200, role khác403; DTO không hash | — | Cường/Khánh | T616-API-01 |
| VA-API-02 | PASS | Page size1000000 nhận effective size100 | — | Cường | T616-API-02 |
| VA-API-03 | PASS | PUT auth/me token hợp lệ405 | — | Trí | T616-API-03 |
| VA-API-04 | PASS | Origin lạ không có ACAO; GET200, OPTIONS401 | — | Nhi/Trí | T616-API-04 |
| VA-API-05 | PASS | DB Before dừng ngắn; login500 dùng thông báo chung, không SQL/stack; DB hồi phục200 | — | Trí/Chính | T616-API-05 |
| VA-EDGE-01 | PASS IN TESTED SCOPE | Swagger/OpenAPI404; actuator health trả HTML SPA, không operational JSON | — | Chính/Nhi | T616-EDGE-01..03 |
| VA-ZAP-01 | COMPLETE WITH OPEN FINDINGS | 1 Baseline +4 role API, HTML/JSON/context thật, GET-only và SQLi rule hữu hạn; triage đầy đủ | 2 nhóm Medium; BUG-HD-009/010 | Khiêm; Khánh nhận | T616-ZAP-01..02 |
| VA-CSP-01 | TRUE CONFIGURATION WEAKNESS | style-src unsafe-inline trong CSP; chưa chứng minh XSS | Medium · BUG-HD-009 | Chính/Nhi; Cường | ZAP rule10055-6 |
| VA-SRI-01 | TRUE HARDENING GAP | CSS Google Fonts ngoài origin thiếu integrity; khuyến nghị self-host font/CSS đã pin | Medium · BUG-HD-010 | Cường/Chính; Nhi | ZAP rule90003 |
| VA-HEADER-01 | TRUE CONFIGURATION WEAKNESS | API trả nhiều header Strict-Transport-Security; cần một nguồn policy HSTS ở gateway | Low · BUG-HD-011 | Chính/Nhi; Trí | ZAP rule10035-3 |

Evidence từng ca: Test_Cases.csv. Alert từng run: ZAP_Triage.csv. Kết quả không thay nghiệm thu AFTER hoặc security sign-off.
