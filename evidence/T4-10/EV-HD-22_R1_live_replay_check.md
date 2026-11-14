# EV-HD-22 — R1 audit replay verification

- Ngày kiểm tra: 14/11/2026 (Asia/Ho_Chi_Minh).
- Nguồn cung cấp: `audit_login_failed_r1.jsonl` (người dùng tải lên; nguồn VM cần đối chiếu với người quản trị).
- Chương trình kiểm: `src/monitoring/monitor.py` từ gói `P01_Hoa_Tuan3_Monitoring_v1_Aligned_Backend.zip`.
- Phương thức: chạy offline một lần trên file audit; **không xác nhận Monitoring sidecar chạy trên VM**.
- Kết quả: `8 audit events`, `0 skipped`, `1 alert`.
- Cảnh báo: `R1_BRUTE_FORCE_LOGIN`, `HIGH`, `event_count=5`, `window_seconds=300`, `detected_at=2026-11-14T15:32:15Z` (22:32:15 Việt Nam).
- 5 login failures cùng actor/IP xảy ra từ `2026-11-14T15:31:45Z` tới `2026-11-14T15:32:15Z`.
- `EV-HD-22_R1_live_replay_redacted.csv` không chứa địa chỉ IP, email, JWT, mật khẩu hoặc correlation ID.
- SHA-256 file log gốc: `1762f7fd39d80893dce434f8704cfe8d2c9ffa7ac70d94c6610a3ba060a8318a`

## Phạm vi chưa được xác minh

- Backend account lockout, Nginx HTTP 429, JWT thực tế hết hạn 15 phút, trạng thái sidecar trên VM.
- Chưa thể kết luận toàn bộ EV-HD-22 PASS chỉ dựa trên lần replay này.
