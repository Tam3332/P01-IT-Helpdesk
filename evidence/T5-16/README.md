# T5-16 · IA-HD06 · Vulnerability Assessment v1 (before)

Rà soát ngày 22/11/2026. Hồ sơ đang chờ kết quả kiểm thử T5-16 trên VM nhóm.

## Phạm vi và build

P01 IT Helpdesk trên VM Ubuntu lab nhóm qua HTTPS cổng 8443. Phạm vi gồm giao diện, API theo OpenAPI và xác thực bằng bốn vai trò thử với dữ liệu seed. Worktree trên Ubuntu cá nhân dùng để đối chiếu mã nguồn.

Mốc before là `v0.5-mvp` (`35c573d20d4244985e617e8a056f880faaadf1db`). T5-07 ghi nhận triển khai thẳng MVP ngày 20/11 và bỏ qua `v0.5-rc1`. Lượt T5-02 ngày 22/11 xác nhận cùng tag, gateway Nginx và chưa tích hợp WAF. [T5-07](https://github.com/Tam3332/P01-IT-Helpdesk/blob/d4903147fae4a6255c6bbab04c13a7a6f54a6b8e/DEPLOY_NOTES.md#L65-L75) · [T5-02](https://github.com/Tam3332/P01-IT-Helpdesk/blob/49038407477dc1871966bf4ba9986aee3c40c816/evidence/T5-02/README.md#L3-L9)

## Bảng đối chiếu

| VA-ID | Nhóm OWASP API 2023 | Phạm vi | Quan sát và trạng thái | Nguồn | Mức độ / BUG-ID |
|---|---|---|---|---|---|
| VA-AUTH-01 | API2 | API ticket thiếu xác thực | VM nhóm đã trả 401 trong lần kiểm tra T5-07 | [Log T5-07](https://github.com/Tam3332/P01-IT-Helpdesk/blob/d4903147fae4a6255c6bbab04c13a7a6f54a6b8e/evidence/T5-07/lab-check_v05_8443.txt#L27-L28) | — |
| VA-AUTH-02 | API2 | JWT | Parser kiểm chữ ký và có kiểm tra thời hạn. Chờ kết quả kiểm thử T5-16 | [JWT provider](https://github.com/Tam3332/P01-IT-Helpdesk/blob/35c573d20d4244985e617e8a056f880faaadf1db/src/backend/src/main/java/com/ojt/p01/security/JwtTokenProvider.java#L54-L82) | — |
| VA-AUTH-03 | API2 | Thu hồi JWT khi logout | Có lưu JTI đã thu hồi và kiểm tra trước khi xác thực. Chờ bằng chứng chạy | [AuthService](https://github.com/Tam3332/P01-IT-Helpdesk/blob/35c573d20d4244985e617e8a056f880faaadf1db/src/backend/src/main/java/com/ojt/p01/service/AuthService.java#L246-L255), [JWT filter](https://github.com/Tam3332/P01-IT-Helpdesk/blob/35c573d20d4244985e617e8a056f880faaadf1db/src/backend/src/main/java/com/ojt/p01/security/JwtAuthenticationFilter.java#L60-L63) | — |
| VA-AUTH-04 | API2 | Phản hồi đăng nhập sai | Email không tồn tại và sai mật khẩu đi qua cùng nhánh báo lỗi. Chờ đối chiếu HTTP | [AuthService](https://github.com/Tam3332/P01-IT-Helpdesk/blob/35c573d20d4244985e617e8a056f880faaadf1db/src/backend/src/main/java/com/ojt/p01/service/AuthService.java#L119-L159) | — |
| VA-AUTH-05 | API2 | Phiên sau đổi mật khẩu | T5-02 xác nhận vấn đề trên lab local. Chờ kết quả trên VM nhóm | [T5-02](https://github.com/Tam3332/P01-IT-Helpdesk/blob/49038407477dc1871966bf4ba9986aee3c40c816/evidence/T5-02/README.md#L54-L61) | Theo lỗi hiện có · BUG-HD-008 |
| VA-API-01 | API3 | Trường trả về ở API | DTO đã rà không có trường hash mật khẩu. Chờ đối chiếu phản hồi theo vai trò | [UserResponse](https://github.com/Tam3332/P01-IT-Helpdesk/blob/35c573d20d4244985e617e8a056f880faaadf1db/src/backend/src/main/java/com/ojt/p01/dto/response/UserResponse.java#L9-L18) | — |
| VA-API-02 | API4 | Phân trang ticket | Mã giới hạn mỗi trang tối đa 100. Chờ bằng chứng chạy | [TicketService](https://github.com/Tam3332/P01-IT-Helpdesk/blob/35c573d20d4244985e617e8a056f880faaadf1db/src/backend/src/main/java/com/ojt/p01/service/TicketService.java#L229-L232), [OpenAPI](https://github.com/Tam3332/P01-IT-Helpdesk/blob/35c573d20d4244985e617e8a056f880faaadf1db/openapi.yaml#L201-L207) | — |
| VA-API-03 | API8 | Phương thức ngoài đặc tả | Chờ đối chiếu HTTP trên VM nhóm | [Xử lý lỗi](https://github.com/Tam3332/P01-IT-Helpdesk/blob/35c573d20d4244985e617e8a056f880faaadf1db/src/backend/src/main/java/com/ojt/p01/exception/GlobalExceptionHandler.java#L131-L140) | — |
| VA-API-04 | API8 | CORS | Chờ đối chiếu header từ VM nhóm | — | — |
| VA-API-05 | API8 | Lỗi 500 | Handler chung dùng thông báo cố định. Chờ bằng chứng chạy | [Xử lý lỗi](https://github.com/Tam3332/P01-IT-Helpdesk/blob/35c573d20d4244985e617e8a056f880faaadf1db/src/backend/src/main/java/com/ojt/p01/exception/GlobalExceptionHandler.java#L143-L147) | — |
| VA-EDGE-01 | API8 | Swagger, OpenAPI, Actuator | Swagger trả 404 ở T5-07. Còn chờ phản hồi riêng cho OpenAPI và Actuator | [Log T5-07](https://github.com/Tam3332/P01-IT-Helpdesk/blob/d4903147fae4a6255c6bbab04c13a7a6f54a6b8e/evidence/T5-07/lab-check_v05_8443.txt#L24-L25), [Nginx](https://github.com/Tam3332/P01-IT-Helpdesk/blob/35c573d20d4244985e617e8a056f880faaadf1db/nginx/nginx.conf#L51-L86) | — |
| VA-ZAP-01 | Theo alert | Giao diện và API | Chờ báo cáo HTML/JSON của ZAP Baseline/API, phân loại alert và context đã bỏ token | — | — |

Khiêm phụ trách bảng VA. Giữ mã BUG-HD-008 hiện có. Kết quả T5-02 dùng làm tham chiếu EV-HD-27 cho đúng ca của T5-02. Khi bổ sung bằng chứng T5-16, ghi rõ build, thời điểm, WAF, kết quả, nguồn và BUG-ID liên quan.

## Bàn giao Tuần 6

Phân công hiện hành đã có tab Tuần 6. Khiêm phụ trách T6-01, T6-08, T6-15 và T6-16; T6-16 hoàn thiện bản before để T6-01 tổng hợp và T6-02 chạy after. [Sheet Tuần 6](https://docs.google.com/spreadsheets/d/18NX_o1GiopEFlLNYtmjXm7pl4dlEY1cn7u21AKC8k7U/edit?gid=923885192)

- **T6-01 · Khiêm.** Tổng hợp Before/After Vulnerability Report, liên kết BUG → commit sửa → kết quả retest, cập nhật 04 §4–5 và nháp SecRS §9. Bản before ở hồ sơ này còn chờ kết quả chạy T5-16.
- **T6-02 · Khánh.** Bàn giao báo cáo đánh giá after, retest quyền, Access Matrix final và Evidence Index. Khi đối chiếu với before, ghi commit triển khai và trạng thái WAF của mỗi lượt.
- **Bản sửa đã vào main.** PR #40 đã merge ngày 23/11/2026, fix commit `f1b4abe`, merge commit `9f273bb`. Bản sửa thống nhất OpenAPI, `ROLE_CHANGED` với before/after và `ACCOUNT_LOCKED`. BUG-HD-003 ở mức đã sửa trên main, chờ triển khai và retest audit trên VM nhóm. Baseline before vẫn là `35c573d`; kết quả after cần ghi commit triển khai thực tế. [PR #40](https://github.com/Tam3332/P01-IT-Helpdesk/pull/40)
- **WAF.** PR #38 đang Draft, chờ review và kết quả trên VM nhóm. Lượt triển khai WAF cần được ghi lại riêng để biết trước và sau đã thay đổi cấu hình gì. [PR #38](https://github.com/Tam3332/P01-IT-Helpdesk/pull/38)

Các kết quả chạy lab bổ sung cần có VA-ID, build, thời điểm, môi trường, trạng thái WAF, kết quả thực tế, evidence và BUG-ID liên quan. T5-16 tiếp tục ở trạng thái In progress.
