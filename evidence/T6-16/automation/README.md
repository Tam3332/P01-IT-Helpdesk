# Dùng lại context và automation

Context XML thực nằm trong ../EV-HD-27_VA_before_ubuntu_20261126/zap/. Bốn file tương ứng EMPLOYEE, IT_STAFF, IT_MANAGER, ADMIN. Context chỉ chứa scope, không chứa JWT hoặc tài khoản đăng nhập. Trước khi chạy AFTER phải thay build/target theo scope được duyệt, không dùng cứng Before.

Bộ Python ở đây là đúng mã đã chạy trên Ubuntu cá nhân. Các helper nhận root từ thư mục chứa script, yêu cầu source archive đúngSHA trong source/, TLS riêng trong tls/, private/runtime.json, private/zap.key, queue/results/logs và PyYAML. setup_before.py tạo secrets riêng tại runtime; private/ không đưa vào repo. Root worker cần chủ VM tự xác thực, danh sách job cố định và hạn90phút. Không cài sudoers/service lâu dài.

Lượt thực: job.py submit <id> build/up; python3 http_cases.py; python3 http_supplement.py; python3 http_data_scope.py; job.py submit <id> zap-start --role ANON; python3 zap_runs.py baseline; python3 zap_runs.py roles. Đọc từng kết quả và tránh tái sử dụng jobID hoặc dữ liệu seed đã bị thay đổi. Không chạy tất cả câu lệnh mù sau một bước lỗi.

Token mới được login tại runtime và đặt vào environment của ZAP container. Script xác minh /auth/me qua chính ZAP trước import và sau scan. Import context đơn lẻ không khôi phục token. Lượt AFTER cần kiểm lại seed/account, quyền thật, hạn token và state WAF; không dùng Expected như Actual. Không đưa file private, .env hay TLS key vào repo. Các port28443/28090 là Before cá nhân được phê duyệt cho lượt này, không phải mặc định cho VM nhóm.
