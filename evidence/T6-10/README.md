# T6-10 — Audit & Monitoring Evidence



**Owner:** Huỳnh Minh Hòa — IA  

**Ngày cập nhật:** 26/11/2026  

**Task:** T6-10, kế thừa T5-03 và T5-10  

**Test cases:** SEC-TC-HD-02, SEC-TC-HD-04  

**Evidence:** EV-HD-22, EV-HD-24  

**Môi trường:** VM lab nhóm v0.5-mvp và v0.6.



## 1. Kết quả kiểm chứng



| Hạng mục | Build | Status |
|---|---|---|
| Monitoring R1 live | v0.5 | PASS |
| Monitoring R2 live | v0.5 | PASS |
| TICKET_ASSIGNED | v0.6 | PASS |
| TICKET_STATUS_CHANGED | v0.6 | PASS |
| REOPEN_REQUESTED | v0.6 | PASS |
| REOPEN_APPROVED | v0.5 | PASS |
| REOPEN_REJECTED | v0.6 | PASS |
| TICKET_CLOSED | v0.6 | PASS |
| ROLE_CHANGED | v0.6 | PASS |



## 2. Evidence



- `audit/`: Audit Log thực tế của vòng đời ticket và thay đổi vai trò.

- `monitoring/`: Kết quả cảnh báo R1/R2 live trên v0.5.

- `screenshots/`: Kết quả giao diện, JWT expiry và kiểm tra dữ liệu nhạy cảm trong Audit Log.



## 3. Commit / PR liên quan



- PR #22: Audit và kiểm thử JWT tuần 4.

- PR #27: Monitoring sidecar trên VM.

- PR #28 — merge 07c71af: Monitoring v2, 39/39 unit tests PASS.

- PR #40 — merge 9f273bb: Thống nhất ROLE_CHANGED và ACCOUNT_LOCKED.



## 4. Kết luận



Đã xác minh 4/4 Audit Event tồn đọng của T5-03/T5-10. Tài khoản kiểm thử đã được khôi phục về vai trò EMPLOYEE.



Kết quả được phân biệt theo từng build. Không sử dụng evidence v0.6 để đánh PASS ngược cho v0.5.



Các ca retest còn thiếu, Monitoring live trên v0.6 và hoàn thiện tài liệu tiếp tục theo T6-03/T6-10. Chưa kết luận PASS toàn bộ SEC-TC-HD-02/04.

## 5. Evidence REOPEN_APPROVED trên v0.5

- Ngày kiểm chứng: 24/11/2026.
- Ticket: HD-2026-0005.
- Event: REOPEN_APPROVED.
- Before: REOPEN_PENDING.
- After: REOPENED.
- HTTP status: 200.
- Kết quả kiểm chứng trước đó: PASS.

### Evidence tham chiếu từ T6-12

- [Manager nhận yêu cầu mở lại](../T6-12/EV-HD-10_p3_01_ticket_cho_duyet_mo_lai_manager.png)
- [Manager xem xét yêu cầu](../T6-12/EV-HD-10_p3_02_manager_xem_xet_yeu_cau_mo_lai.png)
- [Manager phê duyệt thành công](../T6-12/EV-HD-10_p3_03_manager_phe_duyet_thanh_cong.png)
- [Ticket chuyển trạng thái REOPENED](../T6-12/EV-HD-10_p3_04_staff_ticket_chuyen_trang_thai_mo_lai_reopened.png)

Audit Event đã được kiểm chứng trên v0.5 ngày 24/11/2026.
Build v0.5 đã ngừng hoạt động, chưa truy xuất lại JSONL gốc.
Các ảnh T6-12 được dẫn chiếu để bổ trợ bằng chứng.
Chưa retest REOPEN_APPROVED trên v0.6.