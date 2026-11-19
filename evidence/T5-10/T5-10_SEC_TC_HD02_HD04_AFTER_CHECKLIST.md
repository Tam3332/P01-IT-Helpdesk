# T5-10 — Checklist đóng T4-03/T4-10 (EV-HD-22/24)

**Ngày lập:** 17/11/2026. **Owner:** Huỳnh Minh Hòa. **Trạng thái tổng thể: IN PROGRESS — chưa đủ evidence AFTER từ VM.**

## A. Thứ đã có bằng chứng, KHÔNG kiểm thử lại không cần thiết

1. PR #22 đã merge vào main, bằng chứng 12 file (EV-HD-22 và EV-HD-24).
2. PR #27 đã merge vào main, bổ sung 5 file sidecar (container chạy, log startup, đọc được audit).
3. HTTP/JWT tuần 4: 10 assertions PASS, có CSV. Logout/revoked JWT 401 PASS.
4. Offline replay log thật: R1 = 1 alert từ 8 sự kiện; R2 = 3 alerts từ 33 sự kiện; file Audit 81 event tạo 4 alert.
5. Monitoring v2 offline tuần 5: 39/39 tests PASS, bộ giả lập 10 event tạo 2 alerts. Đây **chưa phải** chứng cứ VM runtime.

## B. Thứ phải chạy trên VM mới

| ID | Hành động kiểm chứng theo TC được duyệt | Expected/giới hạn | Actual | Status | EV cần lưu |
|---|---|---|---|---|---|
| HD02-01 | Không token; đăng nhập đúng; logout và dùng JWT cũ | 401/200/200/401 | Kết quả cũ tuần 4 | PASS (week4) | EV-HD-22 CSV hiện có |
| HD02-02 | Test lockout trên **tài khoản demo được phép**, 5 lần sai/5 phút, chặn khi khóa, hết 15 phút | 401×4, khóa+429, tự mở, event khóa tên thống nhất | Chờ build VM mới | NOT RUN | HTTP + JSONL đã che |
| HD02-03 | Thử Nginx `limit_req` theo procedure lab | 429 gateway (phân biệt 429 Backend lockout) | Chưa có bằng chứng | NOT RUN | HTTP response header/log gateway |
| HD02-04 | Dùng JWT sau khi hết hạn thực tế | HTTP 401 | Chưa test | NOT RUN | HTTP status/time không chứa token |
| HD02-05 | Sidecar có ghi alert R1 mới vào `alerts.jsonl` | 1 alert, đúng ngưỡng & thời gian | Chưa có evidence | NOT RUN | Audit + alert redacted cùng cửa sổ |
| HD04-01 | Phân công, đổi trạng thái, đóng, reopen | `before/after` chính xác, event sau commit | Chờ Trí/Cường main + VM | NOT RUN | EV-HD-24 AFTER |
| HD04-02 | Upload hợp lệ, từ chối file, comment | Có `FILE_UPLOADED`, `FILE_UPLOAD_REJECTED(reason)`, `COMMENT_ADDED` | Chờ Backend mới | NOT RUN | EV-HD-24 AFTER |
| HD04-03 | Admin đổi role | `ROLE_CHANGED` có before/after, không dùng `CHANGE_ROLE` | Chờ chốt event | NOT RUN | EV-HD-24 AFTER |
| HD04-04 | SLA worker đánh dấu quá hạn | `SLA_BREACHED` lần đầu, before false/after true; không lặp | Chờ Trí T5-05 | NOT RUN | EV-HD-24 AFTER |
| HD04-05 | Login sai/403-404/State check | IP, UTC, correlation_id đúng; không chứa secrets | Chờ VM mới | NOT RUN | Snapshot log đã che, schema checker |
| HD05-R2 | 3 từ chối ticket hợp lệ trong 5 phút trên môi trường lab | 1 alert R2 mới trong alerts.jsonl | Chưa có evidence live | NOT RUN | Audit + alert redacted |

## C. Phụ thuộc và người chịu trách nhiệm

- **Trí T5-05/T5-12:** event workflow before/after, `SLA_BREACHED`, tag v0.5-rc1 và v0.5-mvp, API ticket.
- **Cường T5-06/T5-13:** Backend lockout, tên event `ACCOUNT_LOCKED`/`BRUTE_FORCE_DETECTED`, upload/comment, ROLE_CHANGED, before/after.
- **Chính T5-07:** triển khai MVP VM, Dashboard/Admin; Nhi T5-04/T5-11 hỗ trợ Monitoring/volumes và hardening.
- **Hòa T5-03/T5-10:** code R1 v2, test rule, kiểm thử lại, evidence, SDD §7 v2, Test Plan §3–4, R-01.
- **Khiêm T5-15:** chuẩn bị log WAF để xét R3 tùy thời gian; hiện chưa triển khai R3.

## D. Bàn giao Review 2

- Timeline theo Sheet: 18/11 build `v0.5-rc1`, 19/11 tag `v0.5-mvp`, 20/11 retest/demo thử, 21/11 Review 2. Đây là **mốc kế hoạch**, không chứng minh build đã tồn tại.
- Cập nhật 04 §3 Actual/Status và §4 BUG với đúng commit/tag, ngày/giờ, evidence. Không đánh dấu T5-10 hoặc T5-03 Done khi thiếu live alert, lockout/expiry hoặc Audit AFTER.
- Đề xuất Sheet: T5-03=`In progress`, T5-10=`In progress` đến khi các mục NOT RUN đổi thành PASS/FAIL có evidence.
