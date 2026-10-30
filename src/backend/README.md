# Backend P01

Thư mục dành cho mã backend do Cường và Trí phát triển trên nhánh `backend`. Hiện file này chỉ xác định vị trí và trách nhiệm; chưa bổ sung ứng dụng, Dockerfile hoặc kết quả chạy thử.

- Mã backend đặt tại `src/backend/`; schema và dữ liệu khởi tạo dùng chung ở `database/` tại gốc repo.
- Nghiệp vụ, hợp đồng API và DB cần khớp SRS/SDD đã được SE–IA xác nhận. Rà thay đổi `REOPEN_PENDING` theo bảng trong [README gốc](../../README.md).
- Phối hợp Khánh về xác thực/phân quyền; Nhi về upload/deployment; Hoà về audit events; Chính về tích hợp API.
- Trước khi gửi PR vào `main`, ghi mã task, file thay đổi, kiểm tra thực tế và phần còn chờ. Hướng dẫn chạy/build sẽ do người triển khai bổ sung cùng mã thật.

Xem [quy trình nhánh và gửi PR](../../README.md#nhánh-làm-việc-và-người-phối-hợp).
