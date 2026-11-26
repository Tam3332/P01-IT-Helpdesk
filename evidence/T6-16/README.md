# T6 16 VA Before hoàn thành kiểm thử

Nguyễn Trọng Khiêm · SE192700 · 26/11/2026 · Khiêm đã duyệt xuất bản

Đã hoàn tất lượt Before trên v0.5-mvp 35c573d20d4244985e617e8a056f880faaadf1db ở lab Ubuntu cá nhân, WAF absent. Kết quả20 dòng HTTP:19PASS,1FAIL tái hiện BUG-HD-008; bốn role có dữ liệu ticket thật. Đã xuất Baseline và API cho4role, mỗi lượt có HTML/JSON/context, role-check và coverage. Hai nhóm alertMedium mới là BUG-HD-009/010; HSTS trùng là BUG-HD-011Low; không có findingHigh trong lượt này. Mã009–011 được ghi vào Defect Log sau khi Khiêm duyệt gói xuất bản; mức độ/owner và retest vẫn theo triage. Hoàn tất đánh giá Before không có nghĩa mọi testPASS hoặc hệ thống đã được security sign-off.

Đọc VA_Register.md, Test_Cases.csv, ZAP_Triage.md/CSV, Defect_Log.md, Coverage.md và Handoff.md. Gói raw được giữ riêng trên VM, bản nộp đã sanitize. Lỗi của detector/status parser và lần thử đầu được giữ trong log, không che giấu hoặc tínhPASS. Hồ sơ static38checks được giữ riêng, không thay cho runtimeevidence.

Khiêm đã duyệt gói và yêu cầu “up đi thầy” ngày 26/11/2026. Xuất bản qua PR #39; trạng thái Done trên sheet chỉ cập nhật sau khi evidence đã publish và PR đã gộp. Bàn giao qua repo không có nghĩa Khánh đã xác nhận tiếp nhận hoặc AFTER đã hoàn tất.

## Bổ sung ảnh chi tiết ngày 26/11/2026

Đã bổ sung 37 ảnh report ZAP, chi tiết từng dòng HTTP và kiểm tra bốn role. Xem [Screenshot_Index.md](EV-HD-27_VA_before_ubuntu_20261126/Screenshot_Index.md) hoặc [gallery](EV-HD-27_VA_before_ubuntu_20261126/Screenshot_Gallery.html). Test_Cases.csv liên kết ảnh tương ứng từng dòng. Ảnh HTTP là ảnh xem lại JSON đã sanitize; kết quả kiểm thử không thay đổi. Gói đã được Khiêm duyệt xuất bản ngày 26/11/2026.
