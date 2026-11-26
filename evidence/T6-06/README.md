# Evidence T6-06 – Test, sửa lỗi và hiệu năng (bản nháp, cập nhật dần)

| Mục | Nội dung | File / vị trí | Trạng thái |
|---|---|---|---|
| Unit + integration test | `mvnw test` 151/151 pass, dev 25/11 (114 cũ + 37 mới: AuthServiceLoginTest 8, CommentServiceTest 8, ApiSecurityWebTest 15, SlaFallbackTest 6) | Ảnh kết quả (chờ đặt mã theo T5-09) | Có ảnh, chưa đưa vào thư mục |
| Sửa lỗi | Thiếu SLAConfig → mặc định Low 2h/48h ở tạo ticket, phân công lần đầu, duyệt mở lại (SRS §3 P2, §7); API Admin vẫn 404 | Commit 614ae92, PR #47 | Chờ merge |
| Newman | `tests/newman/` – 65 request, 217 assertion | Dev 26/11: `newman_dev_summary.md` (65/65 request, 217/217 assertion, 0 lỗi; trung bình 42 ms, lớn nhất 295 ms) + ảnh bảng tổng kết. Lab: chưa chạy | Dev đạt; chờ chạy lab |
| Hiệu năng (~1.000 ticket) | `tests/perf/` – trung bình, p95, lớn nhất | `nfr_perf_<ngày>.md` | Chưa chạy (cần VM lab) |
| 05 §7 Troubleshooting | 11 lỗi thường gặp + khung Lưu ý | 05 Deployment Operations Guide v0.3 | Đã có trong 05 |

Mã evidence (EV-HD-xx) đặt theo quy ước T5-09 khi đưa ảnh vào thư mục này; xác nhận với Khánh trước khi cập nhật Evidence Index.
