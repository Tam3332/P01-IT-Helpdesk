# T5-15 — Bằng chứng WAF audit (EV-HD-28)

## Kiểm thử ngày 19/11/2026

- Môi trường: VM Ubuntu cá nhân, worktree `t5-15-waf`, Nginx + ModSecurity v3 và OWASP CRS.
- Preview thử audit: `p01-waf-audit-smoke`, HTTPS cục bộ cổng `18446`. Chỉ preview này đặt `MODSEC_AUDIT_ENGINE=On` để xác nhận đường ghi audit với một yêu cầu GET hợp lệ.
- Kết quả: HTTPS trả `200`. File `/var/log/modsecurity/audit/waf-audit-smoke.jsonl` có một dòng. Đọc bản ghi JSON xác nhận `http_code=200` và `is_interrupted=false`.

| Ảnh | Thông tin xác nhận |
| --- | --- |
| [01-audit-smoke-http-va-so-dong.png](01-audit-smoke-http-va-so-dong.png) | Lệnh tạo preview, HTTPS `200` và số dòng audit là `1`. |
| [02-audit-smoke-ket-qua-json.png](02-audit-smoke-ket-qua-json.png) | Bản ghi JSON hiển thị thời điểm, mã HTTP `200` và `interrupted=False`. |

Ca này xác nhận ghi audit thành công trên VM cá nhân. Tích hợp cấu hình WAF và kiểm thử lại trên VM nhóm được theo dõi tại T5-15.
