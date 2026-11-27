# EV-HD-28_WAF — kiểm tra Ubuntu cá nhân ngày 26/11/2026

Đây là bằng chứng mới từ Ubuntu cá nhân, **không phải nghiệm thu VM nhóm**. Khiêm xác thực SSH/sudo trực tiếp để thực hiện lượt kiểm tra. Bản raw log/JSON và PNG được giữ nguyên, có hash trong CAPTURE_MANIFEST.json. Ảnh PNG lấy trực tiếp từ framebuffer VM bằng VirtualBox screenshotpng; không dựng terminal, không chỉnh ảnh.

| Lượt | Giờ Việt Nam (UTC+7) | Actual | Evidence |
|---|---|---|---|
| 38 kiểm tra source/YAML/contract/patch | 10:40:38 | 38/38 PASS trên Linux, Python 3.12.3, Git 2.43.0 | [Ảnh](01_38-kiem-tra-Ubuntu.png), [JSON](logs/38-checks-ubuntu.json), [log](logs/static-session.log) |
| nginx-t trước chỉnh môi trường test | 10:43:12–10:43:18 | DetectionOnly FAIL và On FAIL: nginx không đọc được private key root:root 0600 | [Ảnh FAIL](02_nginx-t_before_key_permission.png), [manifest](logs/nginx-before/nginx-syntax-results.json), [log DetectionOnly](logs/nginx-before/nginx-DetectionOnly.log#L63), [log On](logs/nginx-before/nginx-On.log#L63) |
| nginx-t retest với bản sao TLS riêng | 10:45:59–10:46:05 | DetectionOnly PASS và On PASS, exit 0; ModSecurity nạp 857 rule local | [Ảnh PASS](03_nginx-t_after_secure_tls.png), [manifest](logs/nginx-after/nginx-syntax-results.json), [log DetectionOnly](logs/nginx-after/nginx-DetectionOnly.log#L63), [log On](logs/nginx-after/nginx-On.log#L63) |

## Điều kiện thực tế

- Ubuntu cá nhân: Linux 6.8.0-149-generic. Root làm việc riêng `/home/khiem/P01-T6-kiem-tra-Qg6ffzJW/P01-kiem-tra-Ubuntu`.
- Cấu hình follow-up trên main `38a5a29400feeb4ba164b85448e920e140a19a5b`. nginx-waf.conf SHA256 `b99e9a409f71762a432804af4d4d3d1875bd784c913f2a2d40569a61c0b33b33`. Compose SHA256 `904a79491435d63b8c3cd2305652fc0f066698b82018e373b2f37dd9bea09dde`. Chưa phải commit follow-up đã merge.
- Image `owasp/modsecurity-crs@sha256:c95914e1855c41791e6745c2a63864a218f99e04948b64d8b31cb7b29b1ba5b3`; image default user nginx, UID/GID đã xác định 101:101. PL1, inbound5/outbound4 trong lượt syntax; chưa là SecRS Approved.
- Container tạm `--rm`, `--network none`, không publish cổng, không dùng volume ứng dụng. Lượt này không gửi HTTP và không khởi động ZAP.
- Nguồn TLS là thư mục SSL của worktree week4. Key gốc root:root 0600 không đọc được bởi nginx. Retest sao chép TLS vào thư mục riêng 0700, key0400 thuộc UID101, bind read-only; sau đó xóa bản sao. Manifest xác nhận key gốc giữ nguyên metadata và không còn TLS tạm; xem [postcheck](logs/ubuntu-postcheck.txt).
- [Inventory trước](logs/ubuntu-worktree-inventory.txt) và [sau](logs/ubuntu-postcheck.txt) ghi cùng trạng thái dirty/detached của các repo đang có; không đổi tuần4 hay nhánh T5-15. Worktree T5-16 hiện ở đúng baseline 35c573d, chỉ dùng xác nhận mã nguồn.

## Kết luận áp dụng

Đã xác minh parser/module/rules với cấu hình follow-up và điều kiện TLS staging cụ thể. **Chưa** có server phục vụ request, WAF blocked403, UI403, P1/P3/upload regression, audit privacy/R3, tài nguyên hay triển khai VM nhóm. Hai lần syntax không thay các ca nhóm trong checklist.csv. T6-15 vẫn In progress.

Chính/Nhi cần chuẩn bị quyền đọc TLS đúng UID của image trên VM nhóm trước deploy. Không đổi gateway sang root hoặc chmod private key thành world-readable để né lỗi. Xem [TLS_DEPLOYMENT_NOTE.md](../../TLS_DEPLOYMENT_NOTE.md).
