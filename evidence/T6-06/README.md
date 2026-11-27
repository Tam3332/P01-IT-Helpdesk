# Evidence T6-06 – Test, sửa lỗi và hiệu năng (bản nháp, cập nhật dần)

| Mục | Nội dung | File / vị trí | Trạng thái |
|---|---|---|---|
| Unit + integration test | `mvnw test` 151/151 pass, dev 25/11 (114 cũ + 37 mới: AuthServiceLoginTest 8, CommentServiceTest 8, ApiSecurityWebTest 15, SlaFallbackTest 6) | `mvn_test_151_25-11.png` | Đạt |
| Sửa lỗi | Thiếu SLAConfig → mặc định Low 2h/48h ở tạo ticket, phân công lần đầu, duyệt mở lại (SRS §3 P2, §7); API Admin vẫn 404 | Commit 614ae92, PR #47 (merge 00be70f, 25/11) | Đã merge |
| Sửa lỗi BUG-HD-012 (Medium) | Admin đặt lại mật khẩu: bỏ mật khẩu mặc định cố định, bắt buộc nhập 8–100 ký tự (trống → 400); unit test `UserServiceResetPasswordTest` 6 ca tái hiện lỗi | `bug_hd_012_unit_6_pass_27-11.png` (6/6), `mvn_test_157_27-11.png` (toàn bộ 157/157, dev 27/11); commit 1df7236, PR #57 (cũng nằm trong PR #58) | Backend xong, chờ merge; frontend Chính |
| Newman | `tests/newman/` – 65 request, 217 assertion | Dev 26/11: `newman_dev_summary.md` (65/65 request, 217/217 assertion, 0 lỗi; trung bình 42 ms, lớn nhất 295 ms) + `newman_dev_26-11.png`. Lab 27/11 (v0.6-rc1 `ed229a3`) qua Tailscale + Nginx TLS: `newman_lab_summary.md` (65/65 request, 217/217 assertion, 0 lỗi; trung bình 115 ms, p95 353 ms, lớn nhất 498 ms) + `newman_lab_27-11.png` | Dev và lab đạt |
| Hiệu năng (~1.000 ticket) | `tests/perf/` – trung bình, p95, lớn nhất | Lab 27/11 (v0.6-rc1 `ed229a3`), 1.005 ticket (seed 1.000 PERF, đã dọn): `nfr_perf_20261127_1133.md` + `nfr_perf_lab_27-11.png` (seed: `nfr_perf_seed_1000_27-11.png`; dọn: `nfr_perf_cleanup_27-11.png`). 10/10 API đạt; GET p95 lớn nhất 49 ms, upload 10 MB p95 398 ms; không ca nào vượt 2 s | Đạt |
| 05 §7 Troubleshooting | 11 lỗi thường gặp + khung Lưu ý | 05 Deployment Operations Guide v0.3 | Đã có trong 05 |

Mã evidence (EV-HD-xx) đặt theo quy ước T5-09 khi đưa ảnh vào thư mục này; xác nhận với Khánh trước khi cập nhật Evidence Index.
