# EV-HD-22/24 — Kết quả xác minh offline T5-03 (17/11/2026)

**Owner:** Huỳnh Minh Hòa — IA. **Trạng thái:** OFFLINE PASS / VM AFTER PENDING. **Nguồn code:** Monitoring v2 trong gói tuần 5 (chưa tích hợp `main` của nhóm). **Không dùng log gốc chứa thông tin định danh làm evidence GitHub.**

## 1. Kiểm tra tự động

- Lệnh: `python -m unittest discover -s src/monitoring/tests -q`.
- Kết quả thực chạy trên bộ file mới: **39 tests PASS**, 0 failures/errors.
- Bổ sung mới 21 tests tuần 5 (10 trường hợp lockout/đồng bộ R1, 3 trường hợp export dashboard, 8 trường hợp kiểm tra Audit schema).
- Bộ dữ liệu giả lập `audit.week5.synthetic.jsonl`: **10 sự kiện, 0 lỗi, 2 alert** gồm `R1_BRUTE_FORCE_LOGIN` = 1, `R2_UNAUTHORIZED_TICKET_ACCESS` = 1.
- Bộ dữ liệu giả lập gồm 4 `LOGIN_FAILED/401`, 1 `LOGIN_FAILED/429`, 1 `ACCOUNT_LOCKED/429`, 3 `AUTHZ_DENIED/404` ticket và 1 `SLA_BREACHED` — event SLA không tạo R1/R2.
- HTML demo: `EV-HD-22_24_alert_dashboard_DEMO.html`, có nhãn **DEMO — DỮ LIỆU GIẢ LẬP**, actor/IP đã mã hóa HMAC theo từng lần xuất.

## 2. Replay lại dữ liệu Audit thật đã có từ tuần 4 (không kèm nguồn gốc)

| Nguồn (file nhận trước đây) | SHA-256 | Số event hợp lệ | Cảnh báo offline | Trường before / after |
|---|---|---:|---|---|
| `audit_login_failed_r1.jsonl` | `1762f7fd39d80893dce434f8704cfe8d2c9ffa7ac70d94c6610a3ba060a8318a` | 8 | 1 R1 | 0 / 0 |
| `audit_reopen_authz.txt` | `1227f86ec1a5190f66d2c11facb6fe6b90431de2f86599e7ff0ee575645b72f3` | 33 | 3 R2 | 0 / 0 |
| `audit_after_backend_new.jsonl` | `50b3aee6224957ecb7b8d133416d1aca28e8cca1808e164a7217ca99be8ada48` | 81 | 4 alert (1 R1, 3 R2) | 0 / 0 |

**Ghi nhận đặc biệt:** File 81 event có 3 `SLA_BREACHED` và các event reopen nhưng **không có `before` hay `after`**. Vì vậy chưa thể dùng file này xác nhận bản Audit AFTER của SEC-TC-HD-04 đạt yêu cầu. Không suy diễn rằng các event `SLA_BREACHED` trùng ticket nếu chưa đối chiếu target.

Kết quả ở bảng được tái chạy bằng `src/monitoring/monitor.py` v2 và chỉ hiển thị số liệu tổng hợp, không đưa bản log gốc vào gói nộp.

## 3. Audit schema checker — kiểm chứng thêm trên log 81 event

Đã chạy `audit_schema_check.py` với `--mode before` trên bản log 81 sự kiện. Kết quả **39/39 unit tests PASS** và xuất báo cáo chỉ gồm thống kê tại `EV-HD-24_schema_CHECK_BEFORE_existing_81events.md`.

- `before_after_missing_for_change`: **2** (nhóm Reopen đã thực hiện thành công nhưng thiếu cặp field).
- `sla_nonstandard_outcome`: **3** (`SLA_BREACHED` trong log cũ dùng `outcome=BREACHED`, khác schema `SUCCESS` đang đề xuất).
- `sla_transition_invalid`: **3** (`SLA_BREACHED` không có before false/after true).
- 78 `correlation_id` dạng hex 32 ký tự và 3 dạng UUID có dấu nối: cả hai dạng đã được checker chấp nhận; không báo lỗi giả vì định dạng khác nhau.

Đây là **phát hiện trên log cũ**, không đồng nghĩa Backend mới có lỗi giống vậy. Phải kiểm thử lại đúng commit/tag `v0.5-mvp` sau khi nhóm triển khai.

## 4. Sidecar VM — bằng chứng Nhi cung cấp 16/11/2026

- Container `p01-lab-v04-monitoring-1`: `Up 5 minutes` tại thời điểm kiểm tra.
- Console: `[monitor] watch /audit/audit_events.jsonl; append alerts to /alerts/alerts.jsonl`.
- Audit readable: `True`; first line readable: `True`; file size `37524` bytes.

**Đã kiểm chứng theo evidence nhận:** container khởi động, chương trình được cấu hình theo dõi và có thể đọc Audit Log. **Chưa có bằng chứng** quyền ghi `/alerts`, file `alerts.jsonl` live hoặc alert R1/R2 do VM phát; 3 file gốc đã có trong PR #27. Không yêu cầu Nhi thực hiện lại những bước đã xác minh.

## 5. Kiểm thử AFTER bắt buộc trên build MVP

| Ca | Expected | Actual tại 17/11 | Status |
|---|---|---|---|
| SEC-TC-HD-02: 5 lần sai trong 5 phút, khóa 15 phút | 401×4, 429 khi khóa, `ACCOUNT_LOCKED` hoặc alias thống nhất, R1 xuất hiện | Chưa chạy trên build MVP mới | NOT RUN |
| SEC-TC-HD-02: đăng nhập đúng khi đang khóa | HTTP 429 | Chưa chạy | NOT RUN |
| SEC-TC-HD-02: hết hạn khóa | Đăng nhập hợp lệ được phép lại | Chưa chạy | NOT RUN |
| SEC-TC-HD-02: Nginx rate limit | HTTP 429 theo cấu hình gateway (tách account lockout) | Chưa xác minh riêng | NOT RUN |
| SEC-TC-HD-02: JWT hết hạn tự nhiên | Token hết hạn 401 | Chưa chạy | NOT RUN |
| SEC-TC-HD-02: logout | Token logout bị từ chối 401 | 10 HTTP/JWT PASS đã có evidence 14/11, gồm logout | PASS (week 4) |
| SEC-TC-HD-04: phân công/status/reopen | Event đúng, before/after và trace được vào transaction | Backend mới chưa triển khai lên VM | NOT RUN |
| SEC-TC-HD-04: upload/comment/role/SLA | Đúng event, optional reason, before/after khi thay đổi | Chưa test trên MVP | NOT RUN |
| Monitoring live | Alert R1/R2 mới sinh thực tế vào `alerts.jsonl` | Chưa được cung cấp cặp Audit/Alert live | NOT RUN |

**Chỉ điền `PASS` cho test đã có dữ liệu Actual và evidence thực tế.** Chạy thử trên nhánh phát triển không thay thế kết quả build `main` hoặc VM của nhóm. Trong test phải ghi commit/tag, thời gian, HTTP/JSONL đã che, kết quả và BUG-ID nếu fail.
