# Quyết định review bảo mật ngày 28 tháng 11 năm 2026

Nguyễn Trọng Khiêm · IA Lead · P01 Group2 · **T6-01/T6-02/T6-08: In progress**.

Hồ sơ này cập nhật quyết định IA và kết quả đã được cung cấp từ tối 27/11 đến 28/11. Không có lượt kiểm thử VM hoặc quét mới trong lần cập nhật hồ sơ này. Chưa chốt security sign-off, tag final hay nghiệm thu WAF.

## 1. Mốc đối chiếu và nguồn

| Nguồn | SHA / trạng thái khi đối chiếu | Phạm vi đã biết |
|---|---|---|
| Main | `8cd144c50c6fbe1afe5f362295c2a57dbb72044b` | Mốc nguồn để cập nhật hồ sơ; không phải xác nhận build đang chạy trên VM |
| [PR #61](https://github.com/Tam3332/P01-IT-Helpdesk/pull/61) | Open, `228f450609b383183063a1be954f3c4442c14508` | Khánh retest trên VM nhóm ngày 27/11, rc1 `ed229a3`, gateway Nginx thuần, WAF none |
| [PR #52](https://github.com/Tam3332/P01-IT-Helpdesk/pull/52) | Merged `11df1cf`; head trước merge `77046f2` | Follow-up WAF đã vào main; chưa có evidence triển khai WAF nhóm được cung cấp |
| [PR #57](https://github.com/Tam3332/P01-IT-Helpdesk/pull/57), [#60](https://github.com/Tam3332/P01-IT-Helpdesk/pull/60) | Merged `e037a01`, `419f945` | Bỏ fallback mật khẩu reset ở backend và giá trị mặc định ở frontend |
| [PR #63](https://github.com/Tam3332/P01-IT-Helpdesk/pull/63) | Open, `a78a8c8a9058a06e18b8f5bb2374d46137d3ffc1` | Head a78a8c8 đã hoàn lại 6 hash seed về bản cũ; chưa merge/rotation VM |
| [PR #64](https://github.com/Tam3332/P01-IT-Helpdesk/pull/64) | Merged `8cd144c`; fix `e5de2e6` | Self-host font; có build/Network dev, chờ ZAP AFTER lab |
| [PR #62](https://github.com/Tam3332/P01-IT-Helpdesk/pull/62) | Open | i18n và câu chữ reopen; không tính là đã vào final |

Giữ tag `v0.6-rc1 = ed229a3e1362333416822b2866ae7597a15ce917` đúng lịch sử. Chưa có manifest SHA/image/digest/mode của bản final được triển khai trong nguồn đối chiếu này.

## 2. Kết quả nhận từ PR 61

| Nhóm kiểm tra | Actual trên rc1 / WAF none | Giới hạn |
|---|---|---|
| SEC-TC-HD-01 | 9/9 mã HTTP đạt | Không suy ra mọi trường audit đã được kiểm |
| SEC-TC-HD-05 | 9/9 mã HTTP đạt | Biên role/token của BUG-HD-008 vẫn còn mở |
| SEC-TC-HD-06 | 60/60 assertions sau chạy lại | Lượt đầu 59 PASS/1 lỗi môi trường; ca 06.4.8 chạy lại 422 PASS. Reopen RESOLVED quá 168 giờ chưa chạy |
| SEC-TC-HD-07 | 28/28 API đạt | Không gộp thành PASS XSS UI v0.6 hoặc WAF DetectionOnly/On |
| ZAP | Báo cáo Khánh: 5 lượt, FAIL-NEW = 0, triage 40018 false positive | Before/After khác môi trường/phiên bản công cụ. Report thô đầy đủ chưa được đối chiếu trong review này |

**106/106 là tổng assertions đã chạy (9+9+60+28), không phải toàn bộ bộ kiểm thử bảo mật.** Dashboard 07.5h/i vẫn cần Trí/Khánh chốt Expected với Access Matrix và API contract. Giữ T6-02 **In progress**.

Nguồn cố định theo commit PR:

- [Danh sách retest và kết quả BUG008/012](https://github.com/Tam3332/P01-IT-Helpdesk/blob/228f450609b383183063a1be954f3c4442c14508/evidence/T6-02/retest_list.md).
- [Triage ZAP của Khánh](https://github.com/Tam3332/P01-IT-Helpdesk/blob/228f450609b383183063a1be954f3c4442c14508/evidence/T6-02/ZAP_after_triage.md).
- [Log SEC01/05](https://github.com/Tam3332/P01-IT-Helpdesk/blob/228f450609b383183063a1be954f3c4442c14508/evidence/T3-02/result_20261127_2153.txt), [log SEC06 và ca chạy lại](https://github.com/Tam3332/P01-IT-Helpdesk/blob/228f450609b383183063a1be954f3c4442c14508/evidence/T4-02/result_20261127_2153.txt), [log SEC07 API](https://github.com/Tam3332/P01-IT-Helpdesk/blob/228f450609b383183063a1be954f3c4442c14508/evidence/T5-02/EV-HD-27-XSS_api_none_20261127_2155.txt).

## 3. Quyết định BUG 008 đến 012

### BUG-HD-008

**Open phần biên cùng giây, chưa Accepted.** Các ca a/b/c/d/g giữ PASS theo evidence; PR #61 tại 228f450 đã ghi ca e cùng giây 200 là FAIL về thiết kế, bug Open/chưa Accepted; khác giây 401. Ca f chỉ đối chiếu unit test thiếu `iat`, chưa HTTP trên VM. Giữ mức Low của finding lịch sử; tác động role/biên giây còn phải theo dõi, không tự đổi thành Closed.

Filter hiện so `iat < credentials_updated_at` sau khi cắt mốc về giây; quyền được lấy từ role trong token. Token cấp cùng giây có thể giữ quyền cũ tới khi hết hạn. Trí/Cường sửa trước final, Khánh retest cả token cũ, phiên Admin khi đổi user khác, thiếu `iat` và các route cập nhật user. Retest e1: token cấp trước thu hồi trong cùng giây phải 401; e2: token cấp mới sau thu hồi trong cùng giây phải 200. Chỉ đổi dấu thành `<=` không phân biệt được thứ tự này. Trí/Cường chọn giải pháp phân biệt phiên, cập nhật unit test và Khánh chạy lại cả e1/e2; không ghi đã có bản sửa.

### BUG-HD-009

**Accepted risk bởi Nguyễn Trọng Khiêm (IA Lead), ngày 28/11/2026, chỉ cho lab hiện tại của v0.6-final.** Giữ **Medium**, không Fixed/false positive; đây là weakness cấu hình CSP, chưa có bằng chứng XSS khai thác từ finding này.

Chính đề nghị hoãn thay policy vì giao diện còn nhiều inline style và có nguy cơ vỡ UI; số lượng khoảng 137 là thông tin Chính cung cấp, chưa được kiểm đếm độc lập trong review này. Không dùng việc chưa chứng minh XSS để khẳng định không có rủi ro.

- **Owner:** Chính cải thiện; Nhi review cấu hình; Khánh retest ở v0.6.1.
- **Điều kiện trước phát hành:** bổ sung SHA final, header CSP thực tế và nguồn evidence vào manifest. Hiện các thông tin runtime này còn chờ.
- **Giới hạn và hết hiệu lực:** chỉ vòng đời lab v0.6-final đang được chốt; hết hiệu lực khi đổi SHA/CSP/phạm vi, mở ra ngoài lab hoặc triển khai v0.6.1. Muốn tiếp tục phải review và duyệt lại. Ngày lịch của v0.6.1 chưa được nhóm chốt.
- **Hướng cải thiện:** khảo sát `style-src 'self'` cùng `style-src-attr 'unsafe-inline'`, kiểm style element/library, font và browser hỗ trợ, rồi test UI/ZAP. Đây mới là đề xuất; `style-src-attr 'unsafe-inline'` vẫn cho phép style attribute inline, chưa loại bỏ hoàn toàn điểm yếu. [Tài liệu MDN](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Content-Security-Policy/style-src-attr).

Quyết định IA này được ghi ở 02 §8 và 04 §4; không thay chữ ký security sign-off của GVHD hoặc tự phê duyệt nghiệm thu toàn hệ thống.

### BUG-HD-010

**Fix merged, chờ AFTER lab, chưa Closed.** PR #64 tự host font, đã có build và Network dev (10 font local, 0 request Google). Sau deploy final cần xác nhận UI tiếng Việt, không gọi Google và ZAP rule 90003 không còn. Không đổi evidence Open trên rc1 thành PASS final.

### BUG-HD-011

**Low, Open, chưa Accepted.** Chính/Nhi phối hợp Trí chọn một nguồn phát HSTS, sau đó Khánh/Nhi retest raw header HTTPS thực tế, gồm phản hồi hợp lệ và 401/403. Chưa có PR sửa hoặc quyết định chấp nhận rủi ro trong nguồn đối chiếu này; không tự suy từ quyết định BUG-HD-009.

### BUG-HD-012

**High, Open, không Accepted.** Khiêm duyệt nâng mức từ Medium đề xuất dựa trên evidence rc1 của PR #61: reset body trống trả 200; đăng nhập tài khoản thử bằng mật khẩu cố định thành công; chunk `UsersPage-*.js` tải không đăng nhập trả JavaScript 200 và chứa chuỗi.

Điều kiện khai thác: actor truy cập được mạng lab và tài khoản đang dùng mật khẩu đã biết. API reset vẫn chỉ dành cho Admin; chưa chứng minh người không đăng nhập gọi được API reset. Không công bố lại giá trị mật khẩu/token hoặc chunk chứa chuỗi trong finding mới. Chưa gán điểm CVSS.

PR #57/#60 đã sửa BE/FE trên main. PR #63: d8f5641 sửa ví dụ/chú thích; 78b090f từng thay 6 hash seed, nhưng head mới a78a8c8 đã hoàn lại chúng. Đối chiếu file SQL cho thấy 6 hash hiện giống bản đầu PR; PR còn Open/chưa merge, tài khoản VM chưa xác nhận rotation. Vì vậy chưa đủ đóng BUG-HD-012:

1. Cường/Trí chốt cách xử lý credential seed: head a78a8c8 hiện giữ 6 hash cũ, không coi bước sửa ví dụ là đổi credential. Kiểm lại đúng SHA PR trước merge; credential mới giữ kênh riêng. Seed cho DB mới và rotation DB đang có là hai bước riêng.
2. Chính/Nhi kiểm kê và thay mật khẩu các tài khoản bị ảnh hưởng trên **volume VM hiện có**, qua kênh riêng; phối hợp Trí/Cường cho các tài khoản quyền cao. Sửa seed không tự thay password hash đã lưu. Không chạy `down -v` để làm việc này.
3. Khánh retest trên SHA final: reset trống/7/101 ký tự bị từ chối không đổi dữ liệu; reset hợp lệ 8–100 ký tự hoạt động; Admin tự reset bị chặn; token cũ bị thu hồi theo bản sửa BUG008.
4. Sau rotation, credential đã biết không còn đăng nhập được vào tài khoản bị ảnh hưởng. Kiểm việc đổi mật khẩu bắt buộc và quyền API; redirect frontend riêng lẻ không đủ chứng minh hạn chế ở backend.
5. Bundle/chunk final không chứa chuỗi; asset cũ không còn được phục vụ dưới dạng JS chứa credential. 404 là kết quả mong muốn, nhưng phải phân biệt SPA fallback trả HTML 200. Cache/history cũ không được xem là đã thu hồi chỉ vì xóa source; rotation mới loại bỏ tác dụng credential cũ.

Không viết lại Git history chỉ để bỏ ví dụ; giữ evidence lịch sử đúng nguồn và che dữ liệu xác thực mới.

## 4. Điều kiện chốt final

008/012 không có ngoại lệ được duyệt trong lượt này. Cần bản sửa/rotation và retest đúng build trước khi đề nghị chốt final. Nếu nhóm muốn phát hành khi còn mở, phải trình riêng phạm vi, owner, hạn hết hiệu lực và biện pháp giảm thiểu; không ghi tự động là Accepted.

Những điều kiện khác vẫn phải theo dõi: manifest deploy, WAF DetectionOnly/On hoặc fallback được duyệt, các ca NOT RUN, retention/rotation log, monitoring v0.6, 04 §3/§4/§6 và xác nhận phạm vi/biên bản/GV. Chưa coi việc merge code hoặc ghi quyết định IA là đủ gắn tag/hoàn tất nghiệm thu.

PR #55 có audit JSONL v0.6 và ảnh R1/R2 v0.5; `REOPEN_APPROVED` v0.5 chỉ còn nguồn tham chiếu, chưa truy xuất lại JSONL gốc. Giữ R-01 mở phần monitoring v0.6, retention và evidence còn thiếu; không gộp hai build thành một kết quả toàn bộ.

## 5. Finding Gitleaks trong fixture WAF

Đối chiếu [commit 2ca3143, test_tools.py dòng 127–132](https://github.com/Tam3332/P01-IT-Helpdesk/blob/2ca3143a8134ff5e370ea898432c87322eb9e5fc/scripts/waf/tests/test_tools.py#L127): dữ liệu giả lập được đưa vào `PrivacyTests.test_markers_are_reported_without_values` để kiểm privacy scanner nhận diện marker và không in giá trị ra kết quả. Các lần xuất hiện của sentinel trong commit này chỉ ở file test. Test tương ứng đã chạy lại PASS khi review ngày 27/11.

**Chỉ finding ở fixture này được phân loại false positive.** Không thêm allowlist toàn bộ thư mục test, không coi đây là credential lab thật, không kết luận Gitleaks toàn repo PASS. Nhi bổ sung bản scan đầy đủ và fingerprint finding đã phân loại vào EV-HD-28.

Nhi báo lượt scan 174 commits; ref/range thực tế chưa có báo cáo gốc để đối chiếu. `2ca3143` không phải ancestor của tag rc1 `ed229a3`; cần ghi rõ scan có bao gồm main/nhánh khác hay không, tránh gọi toàn bộ là history rc1.

## 6. Phạm vi cập nhật hồ sơ

02 v1.12 Draft, 04 v2.7 Draft, 08 v2.2 Draft cập nhật quyết định và evidence tham chiếu; giữ kết quả functional/regression của Trí và dữ liệu Before theo build gốc. Workbook/Sheet không có trạng thái Done mới được xác nhận từ các trao đổi này. Drive/Sheet chưa được chỉnh trong lượt cập nhật repo.

Nguồn PR #61 còn ở nhánh mở nên các link cố định dùng đúng SHA; chưa nhập hoặc sửa file evidence gốc của Khánh, tránh đè nội dung PR đang review. Không tạo tag final, merge PR của thành viên khác hoặc deploy trong lượt cập nhật hồ sơ.
