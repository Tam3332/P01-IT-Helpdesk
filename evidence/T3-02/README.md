# Bằng chứng T3-02 — AuthZ testing (Nguyễn Tăng Minh Khánh)

Môi trường: lab riêng của Khánh (VM Ubuntu trên VMware Workstation, Docker Compose theo `docker-compose.yml` của repo, truy cập qua Nginx `https://localhost`). Backend nhánh `tri/be` commit `f8f8ef1` (06/11/2026). Tài khoản demo `*@helpdesk.local` từ `database/init_demo_users.sql`, thêm `employee2@helpdesk.local` (Employee B) bằng lệnh INSERT copy password_hash của `employee@`.
Lưu ý: chạy trên lab riêng, **chưa chạy trên VM lab chung** của nhóm. Script và ảnh không chứa token JWT; mật khẩu chỉ là mật khẩu demo công khai trong `DEPLOY_NOTES.md`.

Cách chạy lại: đứng trong thư mục repo trên VM lab, `export BASE=https://localhost` và các biến `PW_EMP1`, `PW_EMP2`, `PW_STAFF`, `PW_MGR`, `PW_ADMIN`, rồi `bash evidence/T3-02/sec_tc_hd_01_05.sh`. Mỗi lần chạy tạo thêm 1 ticket của Employee B.

| EV-ID | File | Ngày | Task / test | Mô tả | Kết quả quan sát |
|---|---|---|---|---|---|
| — | sec_tc_hd_01_05.sh | 2026-11-08 | T3-02, SEC-TC-HD-01, SEC-TC-HD-05 | Script curl + jq: đăng nhập 5 role, Employee B tạo ticket mục tiêu, chạy 19 ca và ghi PASS/FAIL | Không in token ra màn hình hay file |
| EV-HD-21, EV-HD-25 | result_20261108_0010.txt | 2026-11-08 | SEC-TC-HD-01, SEC-TC-HD-05 | Kết quả 19 ca (mã HTTP mong đợi và thực tế) | **19/19 PASS** |
| EV-HD-21 | EV-HD-21_idor_404.png | 2026-11-08 | SEC-TC-HD-01, IA-HD02 | Ca 01.1–01.9: Employee A và Staff chưa được giao truy cập ticket, history, comments, attachments, close của Employee B; ID không tồn tại | Chủ ticket 200; mọi ca trái quyền 404; ticket của B và ticket không tồn tại cùng trả 404 (không lộ sự tồn tại) |
| EV-HD-21 | audit_authz_denied.txt | 2026-11-08 | SEC-TC-HD-01, IA-HD04 | Các dòng `AUTHZ_DENIED` lấy từ `/app/logs/audit_events.jsonl` sau khi chạy script | 13 event, có actor, role, target, action, http_status; không có token hay mật khẩu |
| EV-HD-21 | EV-HD-21_audit_gap.png | 2026-11-08 | SEC-TC-HD-01 ca 01.7, SEC-TC-HD-05 ca 05.8, IA-HD04 | So dòng log cuối trước và sau khi gọi lại 2 ca | **Chưa đạt IA-HD04:** `POST /tickets/{id}/close` (404) và `PATCH /tickets/{id}/status` (403) trả đúng mã nhưng không sinh `AUTHZ_DENIED`. Đã ghi BUG mức Medium, owner Cường (04 §4) |
| EV-HD-25 | EV-HD-25_escalation_403.png | 2026-11-08 | SEC-TC-HD-05, IA-HD01 | Ca 05.1–05.9: Employee/Staff/Manager gọi API ngoài quyền role (assign, it-staff, admin/sla-configs, admin/categories, PATCH status) và gọi không token | Mọi ca thiếu role 403; không token 401 |

Kiểm tra thêm (không có ảnh, ghi trong SecRS §5 v1.3): Admin gọi `POST /api/v1/tickets` → 403, khớp chính sách chỉ Employee tạo ticket.

Còn mở: 4 điểm chờ chốt tại SecRS §3.4 PC-03 (Manager PATCH status, Admin tải tệp, GET feedback, POST feedback 403/404). Các nhánh nghi thiếu audit cùng nguyên nhân (createTicket bởi non-Employee, PATCH status bởi Staff không được giao, close bởi Staff/Admin) mới đọc từ code, chưa test.
