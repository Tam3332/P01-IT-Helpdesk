# Bằng chứng T5-06 — Lockout đăng nhập, giới hạn page size, HD15 trả 422 (Lê Phú Cường)

Môi trường: máy dev (Windows), backend nhánh `cuong/be` commit `aa572aa` (đã push, nằm trong `9465137`), PostgreSQL 18 Docker. Ngày chạy: 17/11/2026; bổ sung 18/11/2026 (commit `367adcb`, `2db6812` trên `cuong/be`). Chưa chạy trên VM nhóm (lab đang chạy tag `v0.4-week4`, chưa có các thay đổi dưới; chờ PR vào `main`).

Mã EV đặt theo TC tương ứng (EV-HD-xx ↔ TC-HD-xx). Quy ước mã evidence chung đang chờ Khánh chốt (T5-09); nếu đổi sẽ đổi tên file theo.

| EV-ID | File | Ngày | Yêu cầu | Mô tả | Kết quả quan sát |
|---|---|---|---|---|---|
| EV-HD-01 | api/EV-HD-01_api_lockout_5_lan_sai_429.png | 2026-11-17 | SRS UC-01 2b, IA-HD05; TC-HD-01.09 | `employee2` đăng nhập sai 5 lần liên tiếp, rồi nhập đúng mật khẩu khi đang khóa; lặp lại với email không tồn tại `khongco@helpdesk.local` | Lần 1–4: 401 `INVALID_CREDENTIALS`; lần 5: 429 `TOO_MANY_LOGIN_ATTEMPTS`; mật khẩu đúng khi đang khóa: 429. Email không tồn tại cho kết quả giống hệt (không lộ email nào có thật). Tài khoản khác (`employee`) vẫn đăng nhập 200 |
| EV-HD-01 | audit_lockout_20261117.jsonl | 2026-11-17 | IA-HD04, IA-HD05; SDD §7 | Trích 13 dòng từ `src/backend/logs/audit_events.jsonl` ứng với ca trên | Mỗi chuỗi: 4 dòng `LOGIN_FAILED` 401 → `LOGIN_FAILED` 429 + `BRUTE_FORCE_DETECTED` 429 cùng thời điểm; lần thử khi đang khóa ghi `LOGIN_FAILED` 429 (`actor_id` null). Email không tồn tại: `actor_id` null. Không có mật khẩu/token trong log |
| EV-HD-13 | api/EV-HD-13_api_page_size_toi_da_100_va_HD15_422.png | 2026-11-17 | HD13, UC-04 3b; T5-06 "giới hạn page size" | Manager gọi `GET /tickets?size=500`, `size=50`, `size=0` | `pageSize` = 100, 50, 20 (vượt 100 giảm về 100; < 1 về mặc định 20). Cùng ảnh: feedback ticket 7 (NEW) → 422 `TICKET_NOT_CLOSED` |
| EV-HD-15 | api/EV-HD-15_api_feedback_2_lan_201_422.png | 2026-11-17 | HD15 (SRS: 422) | Employee gửi đánh giá 2 lần cho HD-2026-0005 (Closed, chưa có đánh giá) | Lần 1: 201; lần 2: 422 `FEEDBACK_ALREADY_SUBMITTED` |
| EV-HD-01 | EV-HD-01_unit_lockout_78_pass.png | 2026-11-17 | T5-06 "Unit test: lockout" | `.\mvnw.cmd test` sau khi thêm `AuthServiceLockoutTest` (7 ca) | `Tests run: 78, Failures: 0, Errors: 0`, `BUILD SUCCESS`; `AuthServiceLockoutTest` 7/7 |
| EV-HD-13 | api/EV-HD-13_api_02_tim_loc_theo_role_va_khoang_ngay.png | 2026-11-18 | HD13, HD05, IA-HD02, IA-HD06; TC-HD-13.01–13.05, 13.07 | Tìm `HD-2026` theo 3 tài khoản; staff2 xem danh sách; Manager lọc `status=RESOLVED&categoryId=1`; tìm `0009` và `HD-2026-0009`; tìm `' OR 1=1 --`, `%`, `_`; lọc `createdFrom=createdTo=2026-11-17` (commit `367adcb`) | employee 9, employee2 0, manager 9; staff2 4 vé đều assignee 7; lọc kết hợp ra HD-2026-0009 RESOLVED, pageSize 20; cả hai cách tìm mã ra HD-2026-0009, employee2 ra 0; chuỗi SQL `200/0`, `%` `200/0`; khoảng ngày ra 3 vé tạo 17/11 giờ Việt Nam (createdAt UTC ngày 16/11 từ 17:00 trở đi) trên tổng 9 |
| EV-HD-13 | api/EV-HD-13_api_03_tim_ky_tu_gach_duoi.png | 2026-11-18 | IA-HD06; TC-HD-13.05 | Xem 2 vé trả về khi tìm `_` | Cả 2 vé có `_` thật trong mô tả (`AUTHZ_DENIED`, `STATUS_CHANGED`) → `_` không thành ký tự đại diện (nếu thành thì khớp cả 9 vé) |
| EV-HD-15 | api/EV-HD-15_api_02_nguoi_khac_admin_du_lieu_sai.png | 2026-11-18 | HD15, IA-HD01, IA-HD02; TC-HD-15.04–15.06 | employee2 gửi đánh giá vào HD-2026-0002 của employee; Admin gửi đánh giá; employee gửi `ratingStars` 0, 6 và `feedbackComment` 501 ký tự | 404 `TICKET_NOT_FOUND`; 403 `FORBIDDEN`; 3 ca đều 400 `VALIDATION_FAILED` |
| EV-HD-15 | audit_feedback_authz_20261118.jsonl | 2026-11-18 | IA-HD04; SDD §7 | Trích dòng `AUTHZ_DENIED` của 2 ca trên từ `src/backend/logs/audit_events.jsonl` | `AUTHZ_DENIED` action `CREATE_FEEDBACK`, target `HD-2026-0002`: employee2 404, Admin 403. Mỗi ca 2 dòng vì lệnh được chạy 2 lần |
| EV-HD-13 | EV-HD-13_unit_tim_kiem_theo_role_va_khoang_ngay_100_pass.png | 2026-11-17 | T5-06 "Unit test: lockout, search theo role" | `.\mvnw.cmd test` sau `367adcb`, merge `tri/be` (`eea8001`) và `2db6812` | `Tests run: 100, Failures: 0, Errors: 0`; `TicketDateRangeTest` 4/4, `TicketSearchScopeTest` 6/6 |

Ghi chú thiết kế lockout (theo quyết định nhóm 17/11):
- Bộ đếm lưu trong bộ nhớ backend theo cặp email + IP; restart backend thì mất bộ đếm.
- Lần sai thứ 5 trong 5 phút trả 429 và khóa 15 phút; hết 15 phút tự mở; đăng nhập đúng xóa bộ đếm.
- Tên event khi khóa: `BRUTE_FORCE_DETECTED` theo SDD §7 (sheet T5-06 ghi `ACCOUNT_LOCKED`, đang chờ Hoà chốt).
- Ảnh chụp PowerShell có một số ký tự lẫn (`\x0a…189feb17…`) do lỗi dán nhiều dòng vào terminal; không ảnh hưởng kết quả.
