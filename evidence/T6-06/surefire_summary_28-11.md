# Kết quả mvn test xuất từ surefire (T6-06)

- Thời điểm: 28/11/2026 10:26 (UTC+7) · Máy dev (Windows, Java 21.0.13), PostgreSQL 18 Docker · Mã nguồn: main `0569b47` (nhánh `cuong/t6-06-surefire`, chưa đổi code)
- Lệnh: `src\backend\.\mvnw.cmd test` · Nguồn: `src/backend/target/surefire-reports/TEST-*.xml` (25 file, 23 lớp có ca kiểm thử; 2 lớp cha chỉ chứa lớp con @Nested)
- **Tổng: 157 ca · 0 failure · 0 error · 0 skipped · BUILD SUCCESS** (thời gian chạy test 36,1 s; tổng lệnh 50 s)
- Ảnh màn hình lần chạy này: `mvn_test_157_28-11.png`. Báo cáo XML gốc không đưa vào repo (sinh lại bằng lệnh trên).
- Coverage JaCoCo: **không làm** – sheet ghi "nếu kịp"; cần thêm plugin vào `pom.xml` (thay đổi build) trong thời gian đóng băng tính năng, ngày gắn tag v0.6-final.

| # | Lớp kiểm thử | Số ca | Failure | Error | Skipped | Thời gian (s) |
|---|---|---|---|---|---|---|
| 1 | `P01ApplicationTests` | 1 | 0 | 0 | 0 | 16.183 |
| 2 | `audit.AuditLoggerTest` | 5 | 0 | 0 | 0 | 4.143 |
| 3 | `controller.ApiSecurityWebTest` | 15 | 0 | 0 | 0 | 11.063 |
| 4 | `security.JwtAuthenticationFilterTest` | 7 | 0 | 0 | 0 | 0.945 |
| 5 | `service.AttachmentServiceTest` | 9 | 0 | 0 | 0 | 0.713 |
| 6 | `service.AuthServiceLockoutTest` | 7 | 0 | 0 | 0 | 0.211 |
| 7 | `service.AuthServiceLoginTest` | 8 | 0 | 0 | 0 | 0.067 |
| 8 | `service.AuthServiceTest` | 5 | 0 | 0 | 0 | 0.046 |
| 9 | `service.CommentServiceTest` | 8 | 0 | 0 | 0 | 0.067 |
| 10 | `service.DashboardServiceTest` | 2 | 0 | 0 | 0 | 0.034 |
| 11 | `service.SlaBackgroundWorkerTest` | 1 | 0 | 0 | 0 | 0.012 |
| 12 | `service.SlaFallbackTest` | 6 | 0 | 0 | 0 | 0.771 |
| 13 | `service.TicketDateRangeTest` | 4 | 0 | 0 | 0 | 0.010 |
| 14 | `service.TicketSearchScopeTest` | 6 | 0 | 0 | 0 | 0.783 |
| 15 | `service.TicketServiceTest$BrHd01Test` | 3 | 0 | 0 | 0 | 0.029 |
| 16 | `service.TicketServiceTest$BrHd02Test` | 5 | 0 | 0 | 0 | 0.086 |
| 17 | `service.TicketServiceTest$BrHd04Test` | 2 | 0 | 0 | 0 | 0.038 |
| 18 | `service.TicketServiceTest$BrHd05Test` | 3 | 0 | 0 | 0 | 0.114 |
| 19 | `service.TicketStatusTransitionValidatorTest$AllowedTransitionsSetTest` | 7 | 0 | 0 | 0 | 0.031 |
| 20 | `service.TicketStatusTransitionValidatorTest$InvalidTransitionsTest` | 32 | 0 | 0 | 0 | 0.484 |
| 21 | `service.TicketStatusTransitionValidatorTest$ValidTransitionsTest` | 9 | 0 | 0 | 0 | 0.022 |
| 22 | `service.UserServiceAuditTest` | 6 | 0 | 0 | 0 | 0.178 |
| 23 | `service.UserServiceResetPasswordTest` | 6 | 0 | 0 | 0 | 0.030 |
| | **Tổng** | **157** | 0 | 0 | 0 | 36.1 |

Ghi chú: `P01ApplicationTests` (nạp toàn bộ context) cần PostgreSQL đang chạy; các lớp còn lại là unit/WebMvc test không cần DB. Log WARN `[SLA] Thiếu SLAConfig…` khi chạy là log cố ý của `SlaFallbackTest`.
