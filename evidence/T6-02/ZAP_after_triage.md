# T6-02 – ZAP after (VA retest)

27/11/2026, 22:10–22:31 (UTC+7) · Khánh (SE193903) · **Draft – chờ Khiêm review**

| Mục | Giá trị |
|---|---|
| Target | VM lab nhóm, `https://<IP-VM>:8443` (Tailscale) |
| Build | v0.6-rc1 (`ed229a3`) |
| WAF | Không có (gateway `nginx:1.27-alpine`) |
| Công cụ | `ghcr.io/zaproxy/zaproxy:stable` qua Docker Desktop (Windows) |
| Lượt quét | `zap-baseline.py` (không đăng nhập) + `zap-api-scan.py` với `openapi-readonly.yaml` cho 4 role (employee, staff, manager, admin), `-T 12` mỗi role |
| So sánh với | VA Before T6-16 (`evidence/T6-16/ZAP_Triage.csv`, Ubuntu cá nhân, build `35c573d`) |

Lưu ý: bản Before chạy trên môi trường và phiên bản ZAP khác, nên rule mới xuất hiện không có nghĩa là lỗ hổng mới.

## Tổng hợp theo lượt
| Lượt | FAIL-NEW | WARN-NEW | PASS |
|---|---|---|---|
| baseline (anon) | 0 | 8 | 60 |
| api-employee | 0 | 5 | 115 |
| api-staff | 0 | 5 | 115 |
| api-manager | 0 | 5 | 115 |
| api-admin | 0 | 5 | 115 |

Mỗi lượt API chạy khoảng 2 phút, ngắn hơn thời hạn JWT 15 phút, nên token còn hiệu lực suốt lượt quét.

## Phân loại
| Rule | Tên | Before | After | Phân loại | BUG / ghi chú |
|---|---|---|---|---|---|
| 10055 | CSP: style-src unsafe-inline | có | có | True configuration weakness | BUG-HD-009 – Open |
| 90003 | Sub Resource Integrity Attribute Missing | có | có | True hardening gap | BUG-HD-010 – Open |
| 10035 | Strict-Transport-Security Multiple Header Entries | có | có (4 role) | True configuration weakness | BUG-HD-011 – Open |
| 40018 | SQL Injection | không | có (4 role) | **False positive (đã xác minh)** | Xem mục dưới |
| 100001 | Unexpected Content-Type was returned | không | có | False positive | ZAP thử đường dẫn metadata cloud (`/opc/v2/instance/`, `/openstack/latest/meta_data.json`); gateway trả `index.html` (SPA fallback). Không có endpoint metadata |
| 10096 | Timestamp Disclosure – Unix | không | có | False positive / Informational | Giá trị epoch trong JSON của `/dashboard/sla-monitor`, `/notifications` (thời gian nghiệp vụ) |
| 90004 | Cross-Origin-Resource-Policy / Cross-Origin-Embedder-Policy Missing | không | có | Hardening gap – Low | Đề xuất thêm header ở gateway; chờ Khiêm/Nhi duyệt |
| 10063 | Permissions Policy Header Not Set | không | có | Hardening gap – Low | Như trên |
| 10049 | Storable and Cacheable Content | không | có | Informational | Tài nguyên tĩnh được cache theo thiết kế |
| 10015 | Re-examine Cache-control Directives | có | có | Informational | Giữ phân loại Before |
| 10109 | Modern Web Application | có | có | Informational | Giữ phân loại Before |
| 10027 | Information Disclosure – Suspicious Comments | có | có | False positive (không phải secret) | Giữ phân loại Before |

**Kết luận:** không có lỗ hổng mới mức Medium trở lên. Ba lỗi cấu hình cũ (BUG-HD-009/010/011) vẫn còn trên rc1.

## Xác minh rule 40018 (SQL Injection)
ZAP báo kiểu boolean-based trên các tham số `result`, `search` (`/admin/audit-logs`); `status`, `priority`, `search`, `categoryId`, `assigneeId`, `sort`, `createdTo` (`/tickets`); `page`, `size`, `limit` (biểu thức `22-2`, `0/2`, `30/2`); `breachedOnly`.

**Đối chiếu code rc1 (`ed229a3`):**
- `/admin/audit-logs`: `AuditLogService` đọc `audit_events.jsonl` và lọc bằng Java, không dùng SQL.
- `/tickets`: `TicketService` dựng truy vấn bằng JPA `Specification` (`cb.like`, `cb.equal`, có escape), giá trị là tham số bind. Không có `createNativeQuery`, `JdbcTemplate` hay ghép chuỗi SQL.
- `page`, `size`, `limit` khai báo kiểu `int`; giá trị như `22-2` bị Spring từ chối (400).

**Kiểm tra tay (22:31, `zap-40018-manual.txt`):**

| Nhóm | Gốc | `' AND '1'='1 -- ` | `' AND '1'='2 -- ` | Chuỗi ngẫu nhiên |
|---|---|---|---|---|
| audit `result` (Admin) | ok → 145 | 737 | 737 | 737 |
| tickets `status` (Employee) | NEW → 1 | 0 | 0 | 0 |
| tickets `search` (Employee) | ZAP → 0 | 0 | 0 | 0 |

Payload đúng và payload sai cho kết quả giống hệt nhau và giống chuỗi ngẫu nhiên, nên điều kiện SQL không ảnh hưởng truy vấn. ZAP báo vì kết quả khác giá trị gốc (giá trị lạ bị bỏ qua hoặc không khớp). **Kết luận: false positive.**

**Đề xuất (Informational):** `/admin/audit-logs` bỏ qua bộ lọc khi `result` không hợp lệ (trả toàn bộ 737 bản ghi). Nên trả 400 cho giá trị ngoài `ok|fail`.

## Evidence
- `screenshots/EV-HD-27_ZAP_after_rc1_20261127.png` (tổng hợp lượt quét + kiểm tra tay 40018).
- Report HTML/JSON thô giữ ngoài repo (có thể chứa header). Bản đầy đủ chuyển cho Khiêm khi cần.
