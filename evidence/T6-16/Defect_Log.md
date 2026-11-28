# Finding Before và trạng thái đối chiếu ngày 28 tháng 11 năm 2026

Nguyễn Trọng Khiêm · IA Lead. Dữ liệu VA Before ngày 26/11 giữ nguyên trong CSV/log gốc, build v0.5-mvp 35c573d trên Ubuntu cá nhân. Bảng dưới cập nhật trạng thái xử lý sau đó, không đổi kết quả Before thành AFTER.

| BUG_ID | Severity hiện hành | Owner | Status ngày 28/11 | Evidence / điều kiện còn lại |
|---|---|---|---|---|
| BUG-HD-008 | Low lịch sử; phần role/biên giây còn theo dõi | Trí/Cường; Khánh retest | Open phần biên cùng giây, chưa Accepted | PR #61 rc1 ed229a3/WAF none: a/b/c/d/g PASS; e cùng giây 200; f chỉ unit test. Sửa và retest trước final, kiểm cả token mới cùng giây |
| BUG-HD-009 | Medium | Chính cải thiện; Nhi review; Khánh retest | Accepted risk riêng lab v0.6-final; chưa Fixed | Khiêm duyệt 28/11, cần gắn SHA/header CSP. Hết hiệu lực khi đổi SHA/CSP/phạm vi, mở ngoài lab hoặc triển khai v0.6.1. Xem 02 §8.2 và hồ sơ quyết định |
| BUG-HD-010 | Medium | Cường/Chính; Nhi review | Fix merged, chờ retest lab | PR #64 e5de2e6, merge 8cd144c: self-host font, evidence dev. Chờ UI/Network/ZAP90003 trên lab final trước Closed |
| BUG-HD-011 | Low | Chính/Nhi; Trí backend | Open, chưa Accepted | HSTS còn lặp trên rc1 theo PR #61. Chọn một nơi phát header và kiểm HTTPS raw headers, cả phản hồi lỗi |
| BUG-HD-012 | High, Khiêm duyệt 28/11 | Cường/Chính; Nhi quản lý lab; Khánh retest | Open, không Accepted; AFTER final NOT RUN | BEFORE rc1 đã chứng minh reset trống, login và chunk không cần đăng nhập. #57/#60 merged; #63 Open a78a8c8 đã hoàn lại 6 hash seed về bản cũ, chưa xác nhận rotation VM; còn rotation tài khoản VM và retest final |

Nguồn tổng hợp: [quyết định review 28/11](../T6-01/REVIEW_DECISIONS_20261128.md), [PR #61](https://github.com/Tam3332/P01-IT-Helpdesk/pull/61), [#63](https://github.com/Tam3332/P01-IT-Helpdesk/pull/63), [#64](https://github.com/Tam3332/P01-IT-Helpdesk/pull/64).

## Lịch sử và giới hạn

VA Before đã được duyệt xuất bản 26/11; việc xuất bản không phải chấp nhận residual risk. Giữ finding 009/010/011 theo nguồn ZAP và triage, không khẳng định XSS/CDN compromise. Quyết định Accepted risk mới chỉ áp dụng riêng BUG009 với giới hạn trên, không bao gồm 008/010/011/012 hoặc toàn hệ thống.

BUG012 ban đầu là Medium đề xuất theo đọc source ngày 26/11; evidence VM của Khánh ngày 27/11 là căn cứ nâng High. Xem [hồ sơ credential](BUG-HD-012_Fixed_Credential.md). Không dùng kết quả rc1/WAF none để đánh PASS final/WAF.
