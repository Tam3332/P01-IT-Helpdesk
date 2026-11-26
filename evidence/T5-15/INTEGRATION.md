# T5-15 · Tích hợp WAF vào gateway lab

Bản cấu hình này dùng ModSecurity và OWASP CRS cho gateway Nginx. Giữ cổng HTTPS phía host theo `HTTPS_PORT`, dùng cổng 8443 trong container và khởi đầu ở `DetectionOnly`. Audit JSON có volume riêng. Danh sách phương thức HTTP giữ các thao tác REST đang dùng trong Helpdesk.

## Bằng chứng và trạng thái

- Ubuntu cá nhân. Bằng chứng HTTPS, demo ticket và ca ghi audit EV-HD-28 đã có trong PR #31. Ca audit smoke trả HTTP 200 và ghi một dòng JSON. [PR #31](https://github.com/Tam3332/P01-IT-Helpdesk/pull/31)
- VM nhóm. Lượt T5-02 ngày 22/11 ghi gateway Nginx và chưa có WAF. [T5-02](https://github.com/Tam3332/P01-IT-Helpdesk/blob/49038407477dc1871966bf4ba9986aee3c40c816/evidence/T5-02/README.md#L3-L9)
- Kiểm tra bản gửi review. Đối chiếu nội dung cấu hình với `main` và kiểm tra cổng, volume audit, template Nginx, TLS và định tuyến. Runtime của bản PR này còn chờ chạy trên VM nhóm.

## Bàn giao

Nhi review cấu hình và Chính phối hợp triển khai. Khi có kết quả trên VM nhóm, lưu kiểm tra khởi động, HTTPS, luồng ticket, tệp gần giới hạn kích thước và audit theo build thực tế. Sau lượt DetectionOnly, nhóm thống nhất điều chỉnh cần thiết và điều kiện chuyển chế độ. Cập nhật ADR-05, SecRS §7, SDD §8 và EV-HD-28 cùng lượt triển khai.
