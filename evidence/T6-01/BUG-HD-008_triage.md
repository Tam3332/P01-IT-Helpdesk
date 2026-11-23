# BUG-HD-008 Thu hồi phiên sau thay đổi thông tin xác thực

Ngày triage: 23/11/2026. T6-01, IA-HD01, SEC-TC-HD-02 và SEC-TC-HD-05.

Giữ mã BUG-HD-008 và bổ sung phạm vi đổi vai trò, khóa hoặc vô hiệu hóa tài khoản theo phương án Trí và Khánh đã trao đổi, được Khiêm xác nhận. Trạng thái: **Mở, đã chốt phương án, chờ PR fix và retest**. Không đánh Closed hoặc PASS khi chưa có bằng chứng AFTER trên build triển khai.

## Bằng chứng hiện có

- Khánh retest ngày 22/11 trên lab local, code v0.5-mvp: đổi mật khẩu trả 200, token cũ gọi /auth/me vẫn trả 200, mật khẩu cũ đăng nhập trả 401. Lỗi còn. Lab local được dùng để tránh đổi mật khẩu tài khoản demo dùng chung. Nguồn: [kết quả T5-02](https://github.com/Tam3332/P01-IT-Helpdesk/blob/afbe94921ad1cb1bb31977777ef09133cef0ec19/evidence/T5-02/README.md#L54-L61), [ảnh gốc](https://github.com/Tam3332/P01-IT-Helpdesk/blob/afbe94921ad1cb1bb31977777ef09133cef0ec19/evidence/T5-02/EV-HD-01_api_BUG-HD-008_retest_local.png).
- Rà mã tại main afbe94921ad1cb1bb31977777ef09133cef0ec19: [JwtAuthenticationFilter.java, dòng 46–63](https://github.com/Tam3332/P01-IT-Helpdesk/blob/afbe94921ad1cb1bb31977777ef09133cef0ec19/src/backend/src/main/java/com/ojt/p01/security/JwtAuthenticationFilter.java#L46-L63) lấy username/role từ claims và kiểm tra JTI blacklist. Đoạn lọc này chưa đối chiếu mốc thay đổi thông tin xác thực hoặc trạng thái user trong DB.
- [AuthService.java, dòng 267–309](https://github.com/Tam3332/P01-IT-Helpdesk/blob/afbe94921ad1cb1bb31977777ef09133cef0ec19/src/backend/src/main/java/com/ojt/p01/service/AuthService.java#L267-L309) cập nhật mật khẩu và ghi PASSWORD_CHANGED; chưa cập nhật mốc thu hồi phiên.
- [UserService.java, dòng 124–164](https://github.com/Tam3332/P01-IT-Helpdesk/blob/afbe94921ad1cb1bb31977777ef09133cef0ec19/src/backend/src/main/java/com/ojt/p01/service/UserService.java#L124-L164) xử lý đổi role/trạng thái và giữ audit. [Đường cập nhật user chung, dòng 209–255](https://github.com/Tam3332/P01-IT-Helpdesk/blob/afbe94921ad1cb1bb31977777ef09133cef0ec19/src/backend/src/main/java/com/ojt/p01/service/UserService.java#L209-L255) cũng cần được người sửa đối chiếu để tránh bỏ sót endpoint.

Hai trường hợp đổi role và vô hiệu hóa tài khoản mới được bổ sung vào phạm vi xử lý. Rà mã là căn cứ cần kiểm tra tiếp; chưa có kết quả HTTP thực tế của hai trường hợp này. Các yêu cầu kỹ thuật dưới đây là phương án chờ triển khai, không phải xác nhận đã có trên main.

## Phạm vi và phương án xử lý

Tên dùng trong Defect Log: **BUG-HD-008 / IA-HD01 – Thu hồi phiên xác thực khi đổi mật khẩu, đổi vai trò hoặc khóa tài khoản**.

| Trường hợp | Yêu cầu xử lý | Phạm vi bằng chứng |
|---|---|---|
| User đổi mật khẩu | Cập nhật mốc credentials_updated_at của user đổi mật khẩu; từ chối token phiên cũ | Lỗi đã được xác nhận trên lab local |
| Admin đổi role của user khác | Cập nhật mốc của user bị đổi role; giữ ROLE_CHANGED với before/after | Chờ triển khai và xác minh |
| Admin khóa/vô hiệu hóa user | Cập nhật mốc của user bị tác động; chặn token cũ và tài khoản đang bị vô hiệu hóa | Chờ triển khai và xác minh |

- Mốc phải gắn với user bị thay đổi. Không cập nhật nhầm mốc của Admin thực hiện thao tác trên user khác.
- Phương án Trí/Khánh: lưu credentials_updated_at trên users, đối chiếu iat trong JwtAuthenticationFilter. Token thiếu iat phải bị từ chối 401.
- Đề xuất so sánh iat với credentials_updated_at.truncatedTo(SECONDS) cần được kiểm tra ở biên cùng giây. Với phép so sánh nhỏ hơn, hai giá trị bằng nhau chưa chứng minh token cũ đã bị thu hồi; người sửa cần chốt cách xử lý và có test tái hiện.
- Cần kiểm tra cả hai đường cập nhật role/trạng thái hiện có trong UserService. Xác nhận cách khởi tạo mốc cho user cũ và hành vi sau khi mở lại tài khoản.
- Khóa/vô hiệu hóa ở đây là thao tác quản trị tài khoản. Không coi kết quả lockout do đăng nhập sai là bằng chứng thay thế cho ca này.
- Giữ cơ chế logout/JTI blacklist, thời hạn token và audit hiện có. Không ghi JWT, mật khẩu hoặc secret vào evidence.

## Tiêu chí retest

Các ca sau **chưa chạy trong lượt triage này**.

| ID | Ca kiểm tra dự kiến | Kết quả cần xác minh |
|---|---|---|
| 008-PW | Lấy token trước khi đổi mật khẩu, đổi mật khẩu thành công, dùng lại token cũ | Token cũ bị từ chối 401; đăng nhập bằng mật khẩu mới hoạt động, mật khẩu cũ bị từ chối |
| 008-ROLE | Admin đổi role của user khác, dùng lại token cũ của user đó; kiểm cả hạ và nâng quyền | Token cũ trả 401; token mới phản ánh role mới và đúng phạm vi quyền |
| 008-ACTIVE | Admin vô hiệu hóa user đang có token; sau đó kiểm tình huống mở lại | User bị vô hiệu hóa không tiếp tục dùng token; token cũ không tự phục hồi khi mở lại |
| 008-TARGET | So sánh user đích, Admin thực hiện thao tác và một user không liên quan | Mốc thay đổi đúng user đích; phiên không bị tác động vẫn hoạt động trong quyền |
| 008-IAT | Token hợp lệ về chữ ký nhưng thiếu iat; kiểm iat không hợp lệ theo contract | Thiếu iat trả 401; trường hợp không hợp lệ không tạo Authentication |
| 008-TIME | Token được phát hành trước, cùng giây và sau mốc thay đổi | Không chấp nhận token cũ do mất độ chính xác thời gian; token mới hợp lệ dùng được theo cách xử lý đã chốt |
| 008-ROUTES | Chạy qua từng đường cập nhật role/trạng thái thực tế | Không có endpoint bỏ sót cập nhật mốc |
| 008-REG | Kiểm logout, hết hạn, audit ROLE_CHANGED và xử lý 401 trên client | Logout/hết hạn vẫn chặn; ROLE_CHANGED có actor Admin, target user đích và before/after; client xử lý phiên hết hiệu lực |

## Phân công và điều kiện đóng

- Trí phụ trách bản sửa; Khánh phụ trách retest; Khiêm ghi triage/Defect Log. Chính xác nhận commit/tag và thời gian deploy VM nhóm trước lượt AFTER.
- **Quyết định: Fix trong v0.6-rc1** (merge `main` trước ngày 25/11/2026). Bản sửa kỹ thuật đã hoàn tất tại commit `590ec58` trên nhánh `tri/be` (kèm PR fix có mã `BUG-HD-008` mở vào `main`), 113/113 unit tests pass.
- Severity gốc của lỗi đổi mật khẩu là Low. Phần role/khóa chờ xác minh tác động và thống nhất severity; không mặc định coi cả phạm vi mở rộng là Low.
- PR sửa cần ghi rõ BUG-HD-008, phạm vi xử lý và test tái hiện. Việc ghi BUG-ID không thay thế review và kiểm thử.
- Chỉ đóng khi có PR/commit sửa, build/tag VM đã triển khai, ngày/người retest và evidence kết quả cho từng phạm vi. Phần chưa kiểm hoặc còn lỗi tiếp tục ghi Open/Partial; Accepted phải có lý do và người duyệt.
- Không thay baseline BEFORE v0.5-mvp (35c573d) bằng commit rà mã hiện tại. Báo cáo AFTER dùng đúng commit/tag đã triển khai.

Theo dõi trong [04 §4](https://docs.google.com/document/d/1wscWfGVqg3JJutzH7SuciQb5Z9rbhr-aqeeFmcirY9U/edit) và [Sheet Tuần 6, T6-01](https://docs.google.com/spreadsheets/d/18NX_o1GiopEFlLNYtmjXm7pl4dlEY1cn7u21AKC8k7U/edit#gid=923885192&range=K2:R2).

