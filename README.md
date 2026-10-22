# Group2 P01 IT Helpdesk và Security Monitoring

Dự án OJT tích hợp Kỹ thuật phần mềm (SE) và An toàn thông tin (IA), thực hiện trong 7 tuần. Nhóm xây dựng hệ thống quản lý hỗ trợ CNTT có phân quyền, bảo vệ dữ liệu ticket và giám sát bảo mật.

## Liên kết làm việc

- [Board và backlog tuần 1](https://github.com/users/Tam3332/projects/1)
- [Tài liệu dự án](docs/)
- [Phân công tuần 1](docs/P01_Tuan1_Phan_cong.xlsx)
- [Phân công toàn bộ 7 tuần](docs/P01_Phan_cong_7_tuan.xlsx)
- [SecRS và Threat Model](docs/02_SecRS_Threat_Model.docx)
- [Quản lý dự án và đóng góp](docs/08_Project_Management_Contribution.docx)

Repo và board được công khai để chia sẻ link xem. Quyền xem công khai không đồng nghĩa quyền chỉnh sửa trực tiếp. Người phụ trách được ghi trong mô tả các thẻ backlog; chưa gán tài khoản GitHub khi nhóm chưa cung cấp username.

## Thành viên và vai trò

| Thành viên | MSSV | Ngành | Nhánh | Vai trò |
| --- | --- | --- | --- | --- |
| Nguyễn Trọng Khiêm | SE192700 | IA | IA | Trưởng nhóm và IA Lead |
| Nguyễn Tăng Minh Khánh | SE193903 | IA | IA | Access Control và AuthZ Testing |
| Huỳnh Minh Hoà | SE193707 | IA | IA | Logging và Security Monitoring |
| Vũ Hồng Nhi | SE182824 | IA | IA | Hardening Upload và Deployment Security |
| Phạm Nguyễn Minh Trí | SE192434 | SE | SE | SE Lead BA và Backend nghiệp vụ |
| Lê Phú Cường | SE200624 | AI | SE | Backend DB Auth và CRUD API |
| Nguyễn Nhật Chính | SE196524 | IS | SE | Frontend ReactJS và Deployment |

Giảng viên hướng dẫn: Chưa cập nhật.

## Phạm vi sản phẩm

Luồng chính: Employee tạo ticket → IT Manager phân công → IT Staff xử lý và cập nhật giải pháp → Employee xác nhận → đóng hoặc mở lại ticket theo chính sách. MVP gồm HD01–HD14: tài khoản, danh mục, ticket, tệp đính kèm, trao đổi, phân công, trạng thái, lịch sử, SLA, tìm kiếm và dashboard.

Nhánh IA phối hợp xác định và kiểm chứng phân quyền theo vai trò/từng ticket, upload an toàn, audit log, phát hiện bất thường, đánh giá bảo mật và hardening. Các yêu cầu cần nối FR → SR → test case → evidence theo từng giai đoạn. Thiết kế và backend cụ thể do nhóm thống nhất.

## Cấu trúc thư mục

```text
docs/       Tài liệu và biểu mẫu 00–09, bảng phân công
evidence/   Bằng chứng có mô tả, nguồn gốc và nội dung đã che dữ liệu nhạy cảm
diagrams/   BPMN, DFD, ERD; file chỉnh sửa được và bản xuất
src/        Mã nguồn frontend và backend do nhóm phát triển
```

## Đầu ra tuần 1

Thời gian theo phân công: 19–25/10/2026. Mốc chung: duyệt rủi ro và requirement; SRS Lite, BPMN và backlog.

- Khiêm: repo/board, header 10 template, SecRS mục 1–3 bản nháp và quản lý tuần 1.
- Khánh, Hoà, Nhi: các dòng Security Requirements và đầu ra IA được giao.
- Trí, Cường, Chính: SRS, BPMN, Business Rules, Use Cases, NFR, screen flow và khung frontend theo bảng phân công.

Thông tin đầu 10 biểu mẫu: P01, Group2, thành viên/vai trò, phiên bản v0.1 ngày 22/10/2026 và trạng thái Draft. Phần thân biểu mẫu do người được giao tiếp tục hoàn thiện; chuẩn hóa header không đồng nghĩa đã xong SRS/SecRS.

## Quy ước theo dõi

Giữ mã T1-01…T1-07 và HD/IA-HD/TH/TC/EV nhất quán. Board dùng Todo, In Progress, Review và Done. Chuyển Review khi có bản nháp và link; chỉ Done khi đầu ra được kiểm tra và có bằng chứng thực tế. Phần setup không tự đánh hoàn thành nội dung threat model hoặc phần việc của các thành viên.

Không đưa mật khẩu, token thật hoặc dữ liệu cá nhân nhạy cảm vào repo/evidence. Chỉ kiểm thử trong môi trường được phép, theo scope và quy trình phê duyệt của dự án. Repo này chưa có hướng dẫn chạy ứng dụng; nhóm sẽ cập nhật khi có bản triển khai thực tế.
