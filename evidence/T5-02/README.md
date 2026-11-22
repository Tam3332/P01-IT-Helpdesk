# Evidence T5-02 – SEC-TC-HD-07 (XSS / SQLi) và retest AuthZ

Người thực hiện: Nguyễn Tăng Minh Khánh (SE193903). Ngày: 22/11/2026.

| Mục | Giá trị |
|---|---|
| Build | tag `v0.5-mvp` (code giống main `e6e8170` trở đi) |
| Môi trường | VM lab nhóm `https://<IP-VM>:8443` qua Tailscale |
| WAF | Không có (gateway `nginx:1.27-alpine`). Lượt `WAF_MODE=detection/on` chờ WAF lên lab (T5-15) |
| Khung giờ test | 17:54–18:35 (giờ VN) = 10:54–11:35 UTC |
| Tài khoản | Tài khoản demo của seed; mật khẩu nhập lúc chạy, không lưu trong file |

## 1. SEC-TC-HD-07 phần API – `sec_tc_hd_07_api.sh`

Kết quả: `EV-HD-27-XSS_api_none_20261122_1754.txt` – **28/28 PASS**. Dữ liệu nền đủ: A_total=4, Staff_total=1, Global_total=5, assign=200.

| Ca | Nội dung | Kết quả |
|---|---|---|
| 07.3 | SQLi qua `search` (4 payload), `pg_sleep(5)`, `size=100000`, `page=-1`, `sort`, `status`, `createdFrom`, `categoryId` | PASS: không lỗi 500, không lộ ticket, phản hồi 0,08 s, `size` giới hạn 100, `createdFrom`/`categoryId` sai → 400 |
| 07.4 | IDOR qua search: Employee A và IT Staff tìm ticket của Employee B theo chuỗi đánh dấu và mã ticket | PASS: A, Staff = 0; Manager = 1 (đối chứng) |
| 07.5 | Leo quyền dọc: `/admin/users` (GET, PATCH role), `/admin/audit-logs` | PASS: 403 cho Employee/Staff/Manager; Admin 200 (đối chứng) |
| 07.5h/i | `/dashboard/stats` theo role | PASS: Employee đếm 4/5 (ticket của mình), IT Staff đếm 1/5 (vé được giao). Code lọc theo role; openapi ghi "chỉ IT_MANAGER" → chờ chốt Access Matrix v1.2 |

## 2. SEC-TC-HD-07 phần giao diện – stored/reflected XSS

Ticket test `HD-2026-0006` (Employee tạo, Manager xem). Payload vô hại `alert('P01')`.

| Ảnh | Nội dung | Kết quả |
|---|---|---|
| `EV-HD-27-XSS_07.1_01_tao_ticket_payload.png` | Tiêu đề `<img src=x onerror=…>`, mô tả `<script>…</script>` | Lưu được |
| `EV-HD-27-XSS_07.1_02_employee_comment.png` | Bình luận `"><svg onload=…>` | Hiển thị dạng chữ |
| `EV-HD-27-XSS_07.1_03_manager_thong_bao.png` | Chuông thông báo của Manager | Không thực thi |
| `EV-HD-27-XSS_07.1_04_manager_danh_sach.png` | Danh sách ticket | Không thực thi |
| `EV-HD-27-XSS_07.1_05_manager_chi_tiet.png` | Chi tiết: tiêu đề, mô tả, bình luận | Hiển thị dạng chữ, không có alert |
| `EV-HD-27-XSS_07.1_06_devtools_escaped.png` | Firefox Inspector: `<h1>&lt;img src=x onerror=alert('P01')&gt;</h1>` | Đã escape |
| `EV-HD-27-XSS_07.1_07_devtools_comment_escaped.png` | `<span>"&gt;&lt;svg onload=alert('P01')&gt;</span>` | Đã escape |
| `EV-HD-27-XSS_07.2_01_tim_kiem_payload.png` | Gõ payload vào ô tìm kiếm; kết quả có highlight | Không thực thi |

Rà mã frontend (main): không dùng `dangerouslySetInnerHTML`, `innerHTML`, `insertAdjacentHTML`, `eval`; nội dung người dùng đi qua escape mặc định của React.

## 3. Audit AUTHZ_DENIED – `EV-HD-27-XSS_audit_authz_denied_lab.txt`

Nhi xuất từ volume audit của lab trong khung 10:54–11:36 UTC; `src_ip` đã che. **9 dòng**, khớp 1-1 với 9 ca bị chặn:

| Endpoint (target_type ENDPOINT) | Số dòng | Ca |
|---|---|---|
| `GET /api/v1/admin/users` | 3 | 07.5a/b/c |
| `PATCH /api/v1/admin/users/{id}/role` | 2 | 07.5e/f |
| `GET /api/v1/admin/audit-logs` | 1 | 07.5g |
| `POST /api/v1/tickets` | 3 | BUG-HD-007 |

Ca 07.3 (400) và 07.4 (lọc theo phạm vi, 0 kết quả) không sinh audit – đúng thiết kế.

## 4. Retest lỗi

| Bug | Môi trường | Kết quả | File |
|---|---|---|---|
| BUG-HD-007 – role khác tạo ticket | Lab nhóm, v0.5-mvp | **PASS**: Staff, Manager, Admin `POST /tickets` → 403, có 3 dòng AUTHZ_DENIED | `BUG-HD-007_retest_lab_20261122_1834.txt`, file audit ở mục 3 |
| BUG-HD-008 – token cũ sau đổi mật khẩu | Lab local (Docker Desktop, code = v0.5-mvp) | **Còn lỗi**: đổi mật khẩu 200; token cũ gọi `/auth/me` vẫn 200; mật khẩu cũ đăng nhập 401 | `EV-HD-01_api_BUG-HD-008_retest_local.png` |

BUG-HD-008 chạy trên lab local để không đổi mật khẩu tài khoản demo dùng chung trên VM nhóm (giới hạn 3 lần/ngày).

## Ghi chú
- IP thật của VM đã thay bằng `<IP-VM>` trong file kết quả; ảnh đã cắt thanh địa chỉ (theo PR #35).
- Kết quả hai lượt chạy thử trên lab local (00:14, 00:23 ngày 22/11) không commit.

## 5. Rate limit (Nginx)

| Zone | Cấu hình | Kiểm tra | Kết quả |
|---|---|---|---|
| `login` | 10r/m, burst 5 | Đã kiểm ở T4-11 (Nhi), T4-07 (Chính) | — |
| `api` | 20r/s, burst 40 | 100 request `GET /api/v1/categories` song song | **PASS**: 74 × 200, 26 × 429 (`EV-HD-28-RL_api_429_lab_*.txt`) |
| Upload | Dùng chung zone `api`, chưa có giới hạn riêng | — | **Đề xuất** gửi Nhi/Chính: zone `upload` 10r/m, burst 5 cho `POST /api/v1/tickets/{id}/attachments` |
