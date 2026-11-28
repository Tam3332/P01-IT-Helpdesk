# BUG-HD-012 Credential cố định

**High · Open · không Accepted · cập nhật 28/11/2026.** Nguyễn Trọng Khiêm (IA Lead) duyệt mức High dựa trên evidence rc1 của Khánh; chưa gán điểm CVSS. AFTER final còn NOT RUN.

## Evidence và điều kiện

PR #61, commit 228f450, VM lab nhóm v0.6-rc1 ed229a3 ngày 27/11, WAF none:

- Admin reset không body trả 200, backend dùng mật khẩu cố định.
- Đăng nhập tài khoản thử bằng mật khẩu đó thành công.
- Chunk UsersPage trong assets tải không đăng nhập trả JavaScript 200 và chứa chuỗi; ảnh ghi hash của file, không đưa mật khẩu hoặc file JS vào repo.

Actor vẫn cần truy cập được mạng lab và tài khoản đang dùng mật khẩu đã biết. API reset chỉ dành cho Admin; evidence không chứng minh người không đăng nhập gọi được API reset.

## Bản sửa và phần chưa hoàn tất

| Phần | Tình trạng khi đối chiếu 28/11 |
|---|---|
| Backend reset | PR #57 merged e037a01: bỏ fallback, trống hoặc ngoài 8–100 ký tự bị từ chối |
| Frontend reset | PR #60 merged 419f945: bỏ ô mặc định và giá trị điền sẵn |
| Ví dụ/chú thích/unit test | PR #63 a78a8c8 còn Open; đã hoàn lại 6 hash seed về bản cũ, chưa có cập nhật dữ liệu VM được xác nhận |
| Tài khoản đã tồn tại | PR #63 ghi VM còn mật khẩu cũ; cần kiểm kê và rotation qua kênh riêng |
| AFTER final | Chưa có manifest deploy và retest đúng SHA; chưa Closed |

Sửa seed chỉ tác động khởi tạo DB mới, không thay password hash đã có trong volume. Cường xử lý cách cấp credential seed; Chính/Nhi phối hợp đổi mật khẩu các tài khoản bị ảnh hưởng, gồm tài khoản quyền cao. Không xóa volume, không viết lại Git history để bỏ ví dụ.

Khánh retest reset sai/hợp lệ, self-reset, token cũ, credential cũ sau rotation, buộc đổi mật khẩu/quyền API và bundle/chunk final. Kiểm asset cũ không còn phục vụ JS chứa credential; nếu SPA fallback trả HTML 200 phải phân biệt với việc còn JS cũ. Cache/history cũ không được coi đã thu hồi chỉ bằng thay source.

Nguồn và checklist đầy đủ: [quyết định IA 28/11](../T6-01/REVIEW_DECISIONS_20261128.md), [PR #61](https://github.com/Tam3332/P01-IT-Helpdesk/pull/61), [#57](https://github.com/Tam3332/P01-IT-Helpdesk/pull/57), [#60](https://github.com/Tam3332/P01-IT-Helpdesk/pull/60), [#63](https://github.com/Tam3332/P01-IT-Helpdesk/pull/63).

## Lịch sử

26/11: source xác nhận fallback, frontend và ví dụ seed; chưa có HTTP/VM evidence, mức Medium đề xuất. 27/11: Khánh bổ sung BEFORE thực tế. 28/11: cập nhật High/Open; merge BE/FE không thay kết quả AFTER. Thay lab-check chỉ giảm lộ credential trong công cụ, không tự sửa password đang dùng.
