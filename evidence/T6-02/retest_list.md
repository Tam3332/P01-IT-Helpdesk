# T6-02 – Danh sách ca retest after
Nguồn: 04 §4 Defect Log, Access Matrix v1.2 (f519d84).
Build retest: v0.6-rc1 (ed229a3) → chạy lại ca có fix trên v0.6-final.

**Lượt 1:** 27/11/2026, 21:00–22:35 (UTC+7) · Khánh (SE193903) · VM lab nhóm `helpdesk-security-lab` qua Tailscale (`https://<IP-VM>:8443`) · build v0.6-rc1 (ed229a3) · **WAF: không có** (gateway `nginx:1.27-alpine`, PR #52 chưa deploy) · máy test: Git Bash trên Windows.

| BUG-ID | Mô tả ngắn | SEC-TC | Commit/PR fix | Có trong build | Người retest | Kết quả after |
|---|---|---|---|---|---|---|
| BUG-HD-008 | Thu hồi phiên khi đổi mật khẩu / đổi role / khóa tài khoản (credentials_updated_at, iat) | SEC-TC-HD-02, 05 | 590ec58 (tri/be) → PR #44, merge 96a2eb1 | v0.6-rc1 | Khánh | **PASS** trên rc1 (27/11): a/b/c/d/g đạt; e ghi nhận rủi ro biên giây; f đối chiếu unit test. EV: `screenshots/EV-HD-21_BUG-HD-008_AFTER_rc1_20261127.png` |
| BUG-HD-012 | Mật khẩu cố định trong luồng Admin reset (backend + frontend + seed) | SEC-TC-HD-08.3 | #57 (backend, Cường) + #60 (frontend, Chính), merge 27/11 | **Chưa** (chỉ có trên main, chờ v0.6-final) | Khánh | **BEFORE xác nhận lỗi** trên rc1 (27/11). AFTER chờ v0.6-final. EV: `screenshots/EV-HD-28_BUG-HD-012_BEFORE_rc1_20261127.png` |
| BUG-HD-003 | Audit thiếu TICKET_ASSIGNED / STATUS_CHANGED / ROLE_CHANGED | SEC-TC-HD-04 | PR #40, fix f1b4abe, merge 9f273bb | v0.6-rc1 | Hoà (T6-03) – tham khảo | Xem T6-10 (EV-HD-24, Hoà) |

Ghi chú: các BUG còn lại trong 04 §4 (001, 002, 004, 005, 007) đã Closed; BUG-HD-006 thuộc functional (Trí/Chính). BUG-HD-009/010/011 (ZAP, T6-16) vẫn Open trên rc1, xem `ZAP_after_triage.md`.

## Ca con BUG-HD-008 (kết quả lượt 1, tài khoản test riêng X, không dùng tài khoản demo)
| Ca | Mô tả | Expected | Actual | Kết quả |
|---|---|---|---|---|
| a | Đổi mật khẩu → token cũ | 401 | 401 (21:37:15) | PASS |
| b | Admin đổi role X → token cũ của X (login và đổi role cách ≥2 giây) | 401 | 401 (21:51:03) | PASS |
| c | Khóa/vô hiệu hóa X → token cũ của X | 401 | 401 (21:46:26) | PASS |
| d | Phiên của Admin sau khi đổi role người khác | 200 | 200 (21:46:01) | PASS |
| e | Biên cùng giây (ghi hành vi thực tế) | ghi nhận | Khác giây → 401 (21:51:24); cùng giây → 200 (21:46:00) | Ghi nhận – rủi ro còn lại |
| f | Token thiếu iat → 401 | 401 | Không ký được token trên VM | Đối chiếu unit test `JwtAuthenticationFilter` |
| g | Admin reset-password → token cũ (bổ sung) | 401 | 401 (21:46:47) | PASS |

- Lượt 21:37:54–21:43:09 **không hợp lệ**: token Admin đã hết hạn (`/auth/me` = 401) nên lệnh Admin không thực thi. Đã ghi chú trong log và chạy lại.
- **Rủi ro còn lại (ca e):** bộ lọc so `iat < credentials_updated_at` cắt về giây, nên token cấp trong cùng giây với lúc thu hồi vẫn hợp lệ tới khi hết hạn (tối đa 15 phút). Đề xuất Khiêm quyết Accepted hoặc đổi sang so sánh `<=`.
- Audit `ROLE_CHANGED`: đối chiếu `evidence/T6-10/audit/EV-HD-24_AFTER_ROLE_CHANGED_20261126.png` (Hoà).

## BUG-HD-012 BEFORE (rc1, tài khoản test X)
| Ca | Mô tả | Actual | Ý nghĩa |
|---|---|---|---|
| 012.1 | Admin `POST /admin/users/{id}/reset-password` không gửi body | 200 | Backend tự đặt mật khẩu cố định |
| 012.2 | Đăng nhập X bằng mật khẩu cố định | có token (21:08:17) | Khai thác được |
| 012.3 | JS bundle tải ngay ở `/login` chứa chuỗi | 0 file | – |
| 012.3b | Chunk tĩnh trong `/assets/` (tải không cần đăng nhập) chứa chuỗi | 1 file (`UsersPage-m1T3_b_N.js`), no-auth GET 200, `Cache-Control: public, immutable, max-age=31536000` | Lộ cho người chưa đăng nhập |

- Lượt 012.2 lúc 21:04:48 không hợp lệ (biến rỗng).
- Không lưu mật khẩu hay file JS vào repo; chỉ ghi tên file, mã HTTP, SHA-256 (`ed19463963bf…aa2d186`), số lần xuất hiện.
- **AFTER trên v0.6-final cần đạt:** reset trống/7 ký tự/101 ký tự → 400; Admin tự reset → 403; đăng nhập bằng chuỗi cũ không được; quét bundle + chunk = 0; gọi lại `/assets/UsersPage-m1T3_b_N.js` → 404.

## Ca chạy lại toàn bộ (không gắn BUG) – lượt 1, WAF=none
| SEC-TC | Nội dung | Kết quả | Log / EV |
|---|---|---|---|
| SEC-TC-HD-01 | IDOR ticket / attachment / search | 9/9 PASS | `evidence/T3-02/result_20261127_2153.txt` · `screenshots/EV-HD-21_SEC-TC-01-05_rc1_20261127.png` |
| SEC-TC-HD-05 | Leo thang dọc theo các ô Deny của Access Matrix v1.2 | 9/9 PASS | như trên |
| SEC-TC-HD-06 | Workflow bypass / mass assignment / P3 | 60/60 PASS | `evidence/T4-02/result_20261127_2153.txt` · `screenshots/EV-HD-26_SEC-TC-06_rc1_20261127_1.png`, `_2.png` |
| SEC-TC-HD-07 | XSS/SQLi/IDOR qua search, leo quyền endpoint mới (API) | 28/28 PASS (WAF=none) | `evidence/T5-02/EV-HD-27-XSS_api_none_20261127_2155.txt` · `screenshots/EV-HD-27_SEC-TC-07_none_rc1_20261127.png` |

- 06.4.8 lượt đầu `actual=000`: `mktemp` trả đường dẫn `/tmp` mà curl Windows không đọc được (lỗi môi trường). Chạy lại bằng file cục bộ → 422 PASS.
- SEC-TC-HD-07 chế độ DetectionOnly / On: **NOT RUN**, VM chưa có WAF (xem `evidence/T6-15/checklist.csv`).
- 07.5h/i: `/dashboard/stats` trả 200 cho Employee/IT Staff (đã lọc theo role), khác `openapi.yaml` (chỉ IT_MANAGER). Không phải lỗ hổng; cần Trí/Khiêm chốt Expected và cập nhật tài liệu.

## Ca bổ sung SEC-TC-HD-06 (P3 / reopen, theo code ReopenController)
| Ca | Expected (theo hành vi rc1) | Kết quả | Ca trong script |
|---|---|---|---|
| Reopen ticket CLOSED | 422, không đổi trạng thái | PASS | 06.4.6, 06.4.9 |
| Reopen ticket RESOLVED quá 168h | 422 REOPEN_WINDOW_EXPIRED | **NOT RUN** – không có ticket RESOLVED quá 168h trong seed, không dựng dữ liệu giả | – |
| Gửi reopen lần 2 khi đang REOPEN_PENDING | 422 REOPEN_ALREADY_PENDING | PASS | 06.8.7 |
| Employee B reopen ticket của A | **404** (trước ghi 403; API trả 404 để không lộ ticket tồn tại) | PASS | 06.3.6 |
| IT Staff / Employee tự gọi API approve | 403 + AUTHZ_DENIED | PASS | 06.8.3, 06.8.5 |
| Lý do < 20 ký tự | **400** (trước ghi 422; API trả lỗi validation 400) | PASS | 06.3.7 |
