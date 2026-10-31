# Group2 P01 — Review 1 v1.2 (30/10/2026)

Bản chuẩn bị review: 12 slide, dùng ảnh DFD v0.2 nguyên bản. Bản v1.1/7 slide được giữ làm lịch sử.

## Mở tài liệu
- [Slide 12 trang trên Google Slides](https://docs.google.com/presentation/d/1qMh5TJrEDtnNpJ78k63mToE69SVZ1wj7eP_LXGamjYg/edit)
- [Thư mục hồ sơ](https://drive.google.com/drive/folders/1o5670jSSJyzpq_LQEqLiZiqBLDQ1X5at)
- [SRS](https://docs.google.com/document/d/1iGDgs5MjEbOUycZbdVMG-zL4vRov60f5/edit)
- [SecRS](https://docs.google.com/document/d/178ulBVk9rQASXX3_5tPjoc1DTZG4iGUeREI69dD-xMw/edit)
- [SDD](https://docs.google.com/document/d/1Wj4q4_IUBI4LX74SAq-ScLbN66R9EYP68d5IKZJEBuY/edit)
- [08 quản lý/đóng góp](https://docs.google.com/document/d/1tYsp1Omfkrt7lsZ0VuTwTnsHArhhEbWn/edit)
- [Draft PR #3](https://github.com/Tam3332/P01-IT-Helpdesk/pull/3)

Trong thư mục này có 01/02/03/08 bản Word v1.2, kịch bản và mẫu biên bản, PowerPoint 12 slide, PDF trình chiếu và DFD nguồn/PNG/ghi chú. Trong repo, 01/02/03/08 giữ tên chuẩn không có tiền tố Group2.

## Cách đọc tiến độ
PR #2 Frontend và #4 Access Matrix đã gộp. PR #5 lab và #6 Compose còn mở tại thời điểm rà soát. SDD §5 đã có 9 SCR/flow/wireframe, nhưng ảnh Employee còn Closed→Reopen và Login dùng Username; Chính cần sửa nguồn cho khớp SRS. PR #6 có minh chứng FE/DB, chưa đủ Backend/gateway/TLS tích hợp.

PR #5 báo cáo IA-HD03 NOT RUN; IA-HD07 Partially Verified; DB khởi tạo được báo cáo PASS trong môi trường riêng. Bốn log gốc được dẫn nhưng chưa đính kèm trong 8 tệp PR #5. Không suy diễn thành PASS bảo mật hoặc kết quả tích hợp.

Các PC-01…06 tại SecRS §3.4 cần owner xác nhận. Thứ tự mã lỗi 401 → endpoint role 403 → object scope 404 → object action 403 → state 422 là đề xuất đồng bộ; validation giải pháp rỗng/khoảng trắng/<20 ký tự trả 400 theo BR-HD-01. Không ghi thay quyết định của SE/Khánh/GV.

T2-01 giữ In progress; T2-08 giữ Review. Chưa có biên bản/góp ý hoặc scope acceptance của GV. Không chạy scanner, pentest hay kiểm thử mục tiêu trong lần cập nhật hồ sơ này.

## Nguồn hình
DFD: P01_DFD_v0.2.png do nhóm cung cấp, giữ nguyên toàn ảnh/tỷ lệ. Ảnh Employee lấy từ evidence trong main; ảnh Ticket detail lấy nguyên bản SDD. Nền bìa tạo bằng công cụ imagegen; chỉ dùng trang trí, không phải bằng chứng kỹ thuật. Chữ, bảng và ghi chú thuyết trình vẫn sửa được trong PowerPoint/Slides.
