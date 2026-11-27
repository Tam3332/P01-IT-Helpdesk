# TLS trước khi deploy WAF — quan sát từ Ubuntu cá nhân

Lượt 26/11 ghi nhận key root:root 0600 làm image default user nginx không khởi động được parser TLS. Cùng config và image, retest với bản sao key0400/dir0700 đúng UID101:101 thành công. Đây là lỗi quyền trong môi trường test đã chứng minh, chưa kết luận VM nhóm mắc cùng lỗi.

Nguồn: [manifest FAIL](EV-HD-28_WAF/ubuntu-personal-20261126/logs/nginx-before/nginx-syntax-results.json), [log FAIL](EV-HD-28_WAF/ubuntu-personal-20261126/logs/nginx-before/nginx-DetectionOnly.log#L63), [manifest retest](EV-HD-28_WAF/ubuntu-personal-20261126/logs/nginx-after/nginx-syntax-results.json).

Chính/Nhi xác nhận UID/GID của đúng digest trước deploy, chuẩn bị TLS trong thư mục riêng không theo dõi Git, key chỉ cho UID/GID cần thiết đọc và mount read-only. Không mặc định mọi image có UID101, không chạy gateway root hoặc mở quyền đọc cho mọi user để xử lý lỗi. Key/cert không nằm trong gói evidence này.

Lượt retest không sửa key gốc của week4. Quyền gốc vẫn root:root0600, vì vậy không được mount lại đúng thư mục đó rồi suy ra WAF sẽ chạy. Cần hoàn thiện bước chuẩn bị TLS thực tế trước deploy nhóm và chạy lại startup/valid-flow trên build triển khai.
