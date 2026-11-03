# P01 DFD v0.2 — thiết kế tham chiếu (Draft)

Ngày: 29-10-2026. Nhóm: Group2. Người phụ trách tổng hợp: Nguyễn Trọng Khiêm.

## Thay đổi so với v0.1

- P0 là Nginx/TLS theo SDD §1; P1 là Spring Boot và D1 là PostgreSQL theo thiết kế hiện có. Đây chưa phải bằng chứng đã triển khai.
- Sửa đường log thành **P1 —F7→ D3 —F8→ P2**: Backend ghi `audit_events.jsonl`, Monitoring đọc JSONL để phát hiện và tạo cảnh báo. Backend chịu trách nhiệm khóa đăng nhập; Monitoring không sửa tài khoản/ticket.
- D3 là nơi lưu Security Audit Log, tách với `ticket_history` trong D1. Không khẳng định D3 là kho lưu mọi cảnh báo.
- Giữ các mã E1, P0/P1/P2, D1/D2/D3, F1–F8, F1R/F2R và TB1–TB3 để tiếp tục truy vết TH/SR/test/risk. P1/P2 trên DFD là tiến trình kỹ thuật, khác mã quy trình nghiệp vụ P1/P2/P3 trong SRS.
- Bổ sung liên kết dữ liệu D1 với AS-02 (blacklist token) và AS-04 (metadata file); tệp thật vẫn ở D2.

## Các luồng

| Mã | Chiều | Dữ liệu |
|---|---|---|
| F1 / F1R | E1 ↔ P0 | HTTPS: yêu cầu và phản hồi được phép |
| F2 / F2R | P0 ↔ P1 | API và phản hồi/mã trạng thái |
| F3 / F4 | P1 ↔ D1 | Truy vấn/cập nhật và dữ liệu/kết quả |
| F5 / F6 | P1 ↔ D2 | Ghi tệp và đọc byte tệp qua Backend |
| F7 | P1 → D3 | Ghi Security Audit Log JSONL; không ghi mật khẩu/JWT |
| F8 | D3 → P2 | Monitoring đọc JSONL để phát hiện/cảnh báo |

## Điểm chưa được coi là đã chốt

1. Nhi–Cường/Trí: cơ chế tải stream/signed URL, thời hạn và thu hồi URL (SDD §4.3, T2-11); hình hiện thể hiện đường qua Backend.
2. Hoà–Backend: tên event chi tiết, schema before/after, cách lưu/cung cấp cảnh báo; Python+JSONL là Proposed, Wazuh là phương án thay thế. Ngưỡng AUTHZ_DENIED 3 lần/5 phút và retention 30 ngày chưa phê duyệt (SDD §7).
3. Nhi–Chính–Trí: mạng/container/volume, port, TLS, quyền DB/log và backup thực tế (SDD §8/T2-04). DFD là ranh giới tin cậy logic; không chứng minh Docker/network đã được cấu hình.

## Nguồn đối chiếu

- [SDD §1, §3, §4.3, §7–9](https://docs.google.com/document/d/1Wj4q4_IUBI4LX74SAq-ScLbN66R9EYP68d5IKZJEBuY/edit), bản v1.0 / 28-10-2026.
- [SecRS §2–6, §8](https://docs.google.com/document/d/178ulBVk9rQASXX3_5tPjoc1DTZG4iGUeREI69dD-xMw/edit).
- [SRS HD01, HD04, HD09 và NFR](https://docs.google.com/document/d/1iGDgs5MjEbOUycZbdVMG-zL4vRov60f5/edit).

Không có quét mục tiêu, pentest hoặc kiểm thử ứng dụng trong lần rà tài liệu này. Các mã SEC-TC/EV là kế hoạch; chưa có kết quả PASS hoặc chấp nhận rủi ro còn lại.
