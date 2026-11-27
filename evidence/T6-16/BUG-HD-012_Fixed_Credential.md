# BUG-HD-012 Credential cố định còn trong mã nguồn

BUG-HD-012: mật khẩu cố định trong luồng Admin reset, giao diện và tài liệu seed. Đọc mã xác nhận UserService.java dùng fallback cố định khi đầu vào trống; UsersPage.jsx có giá trị mặc định hiển thị trong source, tools/generate_seed_users.py và evidence/T3-06 còn ví dụ cũ. Chưa kiểm HTTP để xác nhận bundle được tải trước đăng nhập, chưa chứng minh khai thác trên VM. Medium đề xuất, chờ Trí/Cường triage và xử lý; Khánh retest. Liên quan IA-HD07/SEC-TC-HD-08.3 và quản lý tài khoản. Chưa fix, chưa Accepted và chưa có AFTER.

Loại fallback mật khẩu cố định và nội dung hiển thị trong frontend; yêu cầu mật khẩu tạm thời được sinh hoặc nhập qua kênh quản trị đã review; xử lý seed và credential đang dùng sau kiểm kê. Retest đầu vào trống, mật khẩu tạm, buộc đổi mật khẩu, token cũ và bundle công khai. Không viết lại lịch sử Git chỉ để xóa ví dụ; cần đánh giá và thay credential thực nếu còn dùng.

Nguồn đối chiếu ngày 26/11: UserService.java:184, UsersPage.jsx:234–264, tools/generate_seed_users.py và evidence/T3-06/test_giao_dien.md. Không chép lại giá trị credential vào finding mới. Lịch sử Git còn ví dụ cũ; xóa một câu tài liệu không loại bỏ chúng. Bản sửa lab-check chỉ giảm việc lộ credential trong hướng dẫn/script, chưa sửa hành vi reset của backend/frontend.
