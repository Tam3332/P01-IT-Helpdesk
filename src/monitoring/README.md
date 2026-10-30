# Security Monitoring P01

Thư mục dành cho mã monitoring do Hoà phụ trách trên nhánh `ia`, phối hợp backend phát sinh audit events. Hiện file này chỉ xác định vị trí và trách nhiệm; chưa bổ sung chương trình phát hiện/cảnh báo hay kết quả kiểm thử.

- Mã monitoring khi triển khai đặt tại `src/monitoring/`; thiết kế audit/monitoring đối chiếu SDD §7 và luồng log trong SecRS/DFD.
- Hoà cùng Cường chốt schema sự kiện, nguồn log, nơi đọc/lưu, quy tắc phát hiện và cách cảnh báo trước khi tích hợp. Thay đổi kiến trúc cần báo Khiêm cập nhật DFD và mapping.
- Kế hoạch, kết quả và bằng chứng kiểm thử lưu theo task trong `docs/` và `evidence/`; giữ các link và tên evidence cũ.
- Chỉ ghi PASS khi có kết quả kiểm tra thực tế trong phạm vi được duyệt; không đưa mật khẩu/token hoặc dữ liệu nhạy cảm vào log/evidence.
- Gửi PR vào `main` cùng mã task, cách kiểm tra và giới hạn còn lại. Hướng dẫn chạy sẽ bổ sung cùng mã thật.

Xem [quy trình nhánh và gửi PR](../../README.md#nhánh-làm-việc-và-người-phối-hợp).
