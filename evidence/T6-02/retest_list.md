# T6-02 – Danh sách ca retest after
Nguồn: 04 §4 Defect Log, Access Matrix v1.2 (f519d84).
Build retest: v0.6-rc1 (ed229a3) → chạy lại ca có fix trên v0.6-final.

| BUG-ID | Mô tả ngắn | SEC-TC | Commit/PR fix | Có trong build | Người retest | Kết quả after |
|---|---|---|---|---|---|---|
| BUG-HD-008 | Thu hồi phiên khi đổi mật khẩu / đổi role / khóa tài khoản (credentials_updated_at, iat) | SEC-TC-HD-02, 05 | 590ec58 (tri/be) → PR #44, merge 96a2eb1 | v0.6-rc1 | Khánh | |
| BUG-HD-003 | Audit thiếu TICKET_ASSIGNED / STATUS_CHANGED / ROLE_CHANGED | SEC-TC-HD-04 | PR #40, fix f1b4abe, merge 9f273bb | v0.6-rc1 | Hoà (T6-03) – tham khảo | |

Ghi chú: các BUG còn lại trong 04 §4 (001, 002, 004, 005, 007) đã Closed; BUG-HD-006 thuộc functional (Trí/Chính). BUG mới từ VA v1 (T6-16) sẽ bổ sung khi Khiêm cập nhật 04 §4.

## Ca con BUG-HD-008
a) Đổi mật khẩu → token cũ 401
b) Admin đổi role X → token cũ của X 401, có ROLE_CHANGED before/after
c) Khóa/vô hiệu hóa X → token cũ của X 401
d) Phiên của Admin vẫn hoạt động sau khi đổi role người khác
e) Biên cùng giây (ghi hành vi thực tế)
f) Token thiếu iat → 401 (đối chiếu unit test PR #47 nếu không ký được token trên VM)

## Ca chạy lại toàn bộ (không gắn BUG)
- SEC-TC-HD-01: IDOR ticket / attachment / search
- SEC-TC-HD-05: leo thang dọc theo các ô Deny của Access Matrix v1.2
- SEC-TC-HD-06: 8 ca workflow bypass / mass assignment (T4-02)
- SEC-TC-HD-07: XSS/SQLi, chạy 2 chế độ WAF (DetectionOnly / chặn)

## Ca bổ sung SEC-TC-HD-06 (P3 / reopen, theo code ReopenController)
- Reopen ticket CLOSED → 422, không đổi trạng thái
- Reopen ticket RESOLVED quá 168h → 422 REOPEN_WINDOW_EXPIRED
- Gửi reopen lần 2 khi đang REOPEN_PENDING → 422 REOPEN_ALREADY_PENDING
- Employee B reopen ticket của A → 403 + AUTHZ_DENIED
- IT Staff / Employee tự gọi API approve → 403 + AUTHZ_DENIED
- Lý do < 20 ký tự → 422
