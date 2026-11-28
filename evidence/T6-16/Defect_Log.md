# Finding Before và trạng thái đối chiếu ngày 28 tháng 11 năm 2026

Nguyễn Trọng Khiêm · IA Lead. Dữ liệu VA Before ngày 26/11 giữ nguyên trong CSV/log gốc, build v0.5-mvp 35c573d trên Ubuntu cá nhân. Bảng dưới cập nhật trạng thái xử lý sau đó, không đổi kết quả Before thành AFTER.

Mốc đối chiếu sau PR #66/#67: main `701c00c9d5b6103fb014b17dc4441c92ccf4e454`. Chỉ cập nhật hồ sơ; chưa có evidence deploy, rotation VM hoặc retest final mới được cung cấp trong lượt này.

| BUG_ID | Severity hiện hành | Owner | Status ngày 28/11 | Evidence / điều kiện còn lại |
|---|---|---|---|---|
| BUG-HD-008 | Low lịch sử; phần role/biên giây còn theo dõi | Trí/Cường sửa; Chính deploy; Khánh retest | Fix merged, chờ deploy/retest; vẫn Open, chưa Accepted/Closed | PR #66 merge 9c802eb, head d91a5dd: iatMs và so mốc mili-giây. Giữ BEFORE rc1 ở #61. AFTER cần e1 token cũ cùng giây 401, e2 token mới cùng giây 200; kiểm riêng token legacy không có iatMs |
| BUG-HD-009 | Medium | Chính cải thiện; Nhi review; Khánh retest | Accepted risk riêng lab v0.6-final; chưa Fixed | Khiêm duyệt 28/11, cần gắn SHA/header CSP. Hết hiệu lực khi đổi SHA/CSP/phạm vi, mở ngoài lab hoặc triển khai v0.6.1. Xem 02 §8.2 và hồ sơ quyết định |
| BUG-HD-010 | Medium | Cường/Chính; Nhi review | Fix merged, chờ retest lab | PR #64 e5de2e6, merge 8cd144c: self-host font, evidence dev. Chờ UI/Network/ZAP90003 trên lab final trước Closed |
| BUG-HD-011 | Low | Trí sửa backend; Chính deploy; Nhi/Khánh retest | Fix merged, chờ deploy/retest; vẫn Open, chưa Accepted/Closed | PR #66 merge 9c802eb: tắt HSTS backend, gateway là nguồn HSTS. Retest HTTPS raw headers đúng một HSTS, cả phản hồi hợp lệ và 401/403 |
| BUG-HD-012 | High, Khiêm duyệt 28/11 | Cường sửa seed/BE; Chính deploy và phối hợp Nhi rotation; Khánh retest | Seed fix merged; chờ rotation VM và AFTER; giữ High/Open, không Accepted | #57/#60 merged; #63 đã merge ccf803a sửa ví dụ, #67 merge 701c00c thay 6 hash bcrypt cost12. Seed chỉ áp dụng DB mới; chưa xác nhận đổi mật khẩu các tài khoản trên DB lab hiện có hoặc retest final |

Nguồn tổng hợp: [báo cáo T6-01 cập nhật sau merge](../T6-01/Before_After_Vulnerability_Report.md), [PR #66](https://github.com/Tam3332/P01-IT-Helpdesk/pull/66), [#67](https://github.com/Tam3332/P01-IT-Helpdesk/pull/67), [#61](https://github.com/Tam3332/P01-IT-Helpdesk/pull/61), [#63](https://github.com/Tam3332/P01-IT-Helpdesk/pull/63), [#64](https://github.com/Tam3332/P01-IT-Helpdesk/pull/64). [Quyết định IA 28/11](../T6-01/REVIEW_DECISIONS_20261128.md) giữ snapshot khi lập #65; trạng thái merge mới theo bảng trên.

## Điều kiện chờ sau merge

Chính build/deploy đúng bản có #66/#67 và gửi manifest SHA, image/digest, WAF mode, giờ chạy; phối hợp Nhi đổi mật khẩu các tài khoản bị ảnh hưởng trên volume VM hiện có, không dùng `down -v`. Khánh retest đúng build rồi bổ sung Actual/Status/evidence; không ghi PASS hoặc Closed từ việc code đã merge.

Đối với BUG008, code vẫn giữ luật cũ cho token thiếu iatMs. Kết quả token mới sau deploy không thay kiểm tra phiên legacy. Đối với BUG012, chỉ đổi seed không chứng minh đã thay credential tài khoản đang có; cả tài khoản demo và tài khoản từng bị reset trống cần được kiểm kê. Không ghi credential vào repo.

## Lịch sử và giới hạn

VA Before đã được duyệt xuất bản 26/11; việc xuất bản không phải chấp nhận residual risk. Giữ finding 009/010/011 theo nguồn ZAP và triage, không khẳng định XSS/CDN compromise. Quyết định Accepted risk mới chỉ áp dụng riêng BUG009 với giới hạn trên, không bao gồm 008/010/011/012 hoặc toàn hệ thống.

BUG012 ban đầu là Medium đề xuất theo đọc source ngày 26/11; evidence VM của Khánh ngày 27/11 là căn cứ nâng High. [Hồ sơ credential](BUG-HD-012_Fixed_Credential.md) giữ lịch sử review trước #67, cần đọc cùng trạng thái mới ở bảng trên. Không dùng kết quả rc1/WAF none để đánh PASS final/WAF. T6-01/T6-02 vẫn In progress; hai DOCX 02/04 để Khánh cập nhật riêng.
