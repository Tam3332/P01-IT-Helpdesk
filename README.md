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

| Thành viên | MSSV | Ngành | Mảng dự án | Vai trò |
| --- | --- | --- | --- | --- |
| Nguyễn Trọng Khiêm | SE192700 | IA | IA | Trưởng nhóm và IA Lead |
| Nguyễn Tăng Minh Khánh | SE193903 | IA | IA | Access Control và AuthZ Testing |
| Huỳnh Minh Hoà | SE193707 | IA | IA | Logging và Security Monitoring |
| Vũ Hồng Nhi | SE182824 | IA | IA | Hardening Upload và Deployment Security |
| Phạm Nguyễn Minh Trí | SE192434 | SE | SE | SE Lead BA và Backend nghiệp vụ |
| Lê Phú Cường | SE200624 | AI | SE | Backend DB Auth và CRUD API |
| Nguyễn Nhật Chính | SE196524 | IS | SE | Frontend ReactJS và Deployment |

Giảng viên hướng dẫn: Ngô Đăng Hà An.

## Phạm vi sản phẩm

Luồng chính: Employee tạo ticket → IT Manager phân công → IT Staff xử lý và cập nhật giải pháp → Employee xác nhận → đóng hoặc mở lại ticket theo chính sách. MVP gồm HD01–HD14: tài khoản, danh mục, ticket, tệp đính kèm, trao đổi, phân công, trạng thái, lịch sử, SLA, tìm kiếm và dashboard.

Nhánh IA phối hợp xác định và kiểm chứng phân quyền theo vai trò/từng ticket, upload an toàn, audit log, phát hiện bất thường, đánh giá bảo mật và hardening. Các yêu cầu cần nối FR → SR → test case → evidence theo từng giai đoạn. Thiết kế và backend cụ thể do nhóm thống nhất.


## Nhánh làm việc và người phối hợp

Nhóm dùng một repo công khai, với `main` là bản tích hợp và ba nhánh làm việc:

| Nhánh Git | Nội dung chính | Người phụ trách | Phối hợp kiểm tra |
| --- | --- | --- | --- |
| [frontend](https://github.com/Tam3332/P01-IT-Helpdesk/tree/frontend) | Giao diện React, screen flow, tích hợp API trong `src/frontend/` | Chính | Trí chốt nghiệp vụ; Cường đối chiếu API; Khánh đối chiếu quyền |
| [backend](https://github.com/Tam3332/P01-IT-Helpdesk/tree/backend) | Backend trong `src/backend/`, DB trong `database/`, hợp đồng API | Cường và Trí | Khánh: xác thực/phân quyền; Nhi: upload/deployment; Hoà: audit events |
| [ia](https://github.com/Tam3332/P01-IT-Helpdesk/tree/ia) | SecRS/DFD, Access Matrix, tài liệu kiểm thử và evidence; monitoring trong `src/monitoring/` khi có mã | Khiêm, Khánh, Hoà, Nhi theo phần được giao | Trí/Cường/Chính khi thay đổi liên quan nghiệp vụ, API hoặc giao diện |
| [main](https://github.com/Tam3332/P01-IT-Helpdesk/tree/main) | Bản đã tích hợp qua PR | Khiêm và Trí phối hợp quản lý | Người phụ trách phần thay đổi kiểm tra trước khi gộp |

**Nhánh là một bản của toàn bộ dự án.** Chuyển sang `frontend` vẫn thấy cả docs, database và backend; phân chia thư mục giúp biết sửa ở đâu. Tên nhánh không tự giới hạn quyền chỉnh sửa của từng người.

Các nhánh cũ được giữ để bảo toàn lịch sử và PR đang mở. Trước khi tiếp tục một việc cũ, đọc PR liên quan rồi thống nhất chuyển việc sang nhánh nào; không chép lại hoặc gộp lại các commit đã có trên `main`. Repo vẫn thuộc Tam3332; việc chuyển Organization sẽ quyết định riêng.

### Cách cập nhật và gửi PR

1. Clone repo một lần; các lần sau mở bản clone đang dùng. Đọc SRS/SDD, README và PR liên quan trước khi sửa.
2. Lưu hoặc commit công việc đang làm trước khi đổi nhánh. Chọn đúng nhánh `frontend`, `backend` hoặc `ia`; lấy cập nhật mới từ remote, rồi tích hợp `main` nếu `main` có thay đổi mới.
3. Sửa đúng phần việc đã nhận. Nếu nhiều người cùng dùng một nhánh, phối hợp trước khi sửa cùng file; có thể tạo nhánh ngắn theo task để tránh dẫm thay đổi.
4. Xem danh sách file thay đổi, chạy kiểm tra phù hợp, rồi commit và push bình thường. Không force push các nhánh chung; nếu bị từ chối push, lấy commit mới về và giải quyết xung đột trước.
5. Mở Pull Request với **base: main**, **compare: nhánh chứa việc cần gộp**. Một PR cần ghi: mã task, thay đổi gì, file/mục liên quan, ai cần kiểm tra, kiểm tra đã chạy và link bằng chứng. Không ghi PASS cho kiểm tra chưa chạy.
6. Người liên quan kiểm tra; Khiêm/Trí phối hợp gộp khi đạt yêu cầu. Sau khi gộp, các nhánh còn lại lấy cập nhật từ `main` trước khi làm phần phụ thuộc. Không xóa ba nhánh làm việc chung sau mỗi lần gộp.
7. Gắn link PR/commit và evidence vào Sheet/board; chuyển Review khi có đầu ra, Done sau khi đã được kiểm tra.

Ví dụ lệnh cho một bản clone **đã có sẵn**, đang sạch và làm frontend (thay `frontend` bằng `backend` hoặc `ia` theo việc được giao):

```bash
git status
git fetch origin
git switch frontend
git pull --ff-only origin frontend
git merge origin/main
# Sửa file và chạy kiểm tra phù hợp; nếu có conflict thì xử lý trước.
git diff
git add <duong-dan-file-da-kiem-tra>
git commit -m "T3-07: mo ta thay doi giao dien"
git push origin frontend
```

Lần đầu chưa có nhánh local: dùng `git switch --track origin/frontend` sau `git fetch origin`. Không copy nguyên dòng `git add <...>`; thay bằng đường dẫn file thật. Nếu `pull --ff-only` bị từ chối, dừng để xử lý lịch sử với người đang cùng nhánh; không ép ghi đè.

### File dùng chung: cần kiểm tra chéo

| Vị trí | Người cập nhật chính theo phần việc | Cần phối hợp |
| --- | --- | --- |
| SRS, SDD nghiệp vụ/API/DB | Trí, Cường; Chính cho giao diện | IA rà yêu cầu bảo mật và mapping |
| SecRS, DFD, kế hoạch kiểm thử, rủi ro | Khiêm và IA theo dòng được giao | SE xác nhận hệ thống thực tế |
| Access Matrix | Khánh | Trí chốt nghiệp vụ; Cường/Chính áp dụng |
| Audit/monitoring | Hoà | Cường phát sự kiện; Khiêm đối chiếu DFD |
| Compose/gateway/TLS/upload | Nhi và Chính, Cường theo thành phần | SE–IA cùng rà trước khi tích hợp |
| File 08 và liên kết Sheet/board | Khiêm tổng hợp; mỗi người cập nhật phần mình | Trí và cả nhóm xác nhận trạng thái |

Các file 01–02–03–08 trong `docs/` là tài liệu dùng chung. Khi sửa Word, thống nhất người đang giữ bản chỉnh sửa; dẫn phiên bản/link nguồn trong PR vì không thể tự gộp nội dung nhị phân an toàn như file text.

## Việc cần đồng bộ theo SRS mới: REOPEN_PENDING

**Trạng thái: cần SE–IA đối chiếu và cập nhật; việc chia nhánh chưa triển khai thay đổi nghiệp vụ này.**

Nguồn đối chiếu là [SRS Trí cập nhật ngày 30/10/2026](https://docs.google.com/document/d/1iGDgs5MjEbOUycZbdVMG-zL4vRov60f5/edit), các phần HD07/P3 và quy tắc reopen. SRS mới bổ sung `REOPEN_PENDING` cho ticket trong lúc chờ Manager duyệt. Cần phân biệt trạng thái ticket này với `PENDING` của bản ghi yêu cầu mở lại.

Bản `main` dùng làm mốc kiểm tra ngày 31/10/2026 là [30f1ebd](https://github.com/Tam3332/P01-IT-Helpdesk/tree/30f1ebdd22aaf6bff10f92b8ca6baf32c493b0b2). Tại mốc này:
- [database/schema.sql dòng 73](https://github.com/Tam3332/P01-IT-Helpdesk/blob/30f1ebdd22aaf6bff10f92b8ca6baf32c493b0b2/database/schema.sql#L73) chưa có `REOPEN_PENDING` trong ràng buộc trạng thái ticket.
- [src/frontend/src/config/constants.js](https://github.com/Tam3332/P01-IT-Helpdesk/blob/30f1ebdd22aaf6bff10f92b8ca6baf32c493b0b2/src/frontend/src/config/constants.js#L22) chưa có trạng thái mới trong cấu hình trạng thái giao diện.
- [PR #3 — hồ sơ Review 1](https://github.com/Tam3332/P01-IT-Helpdesk/pull/3) cần đối chiếu với SRS mới trước khi gộp; tránh đưa bản SRS cũ trong PR đè lên nội dung Trí vừa cập nhật.

| Người | Việc cần chốt/cập nhật |
| --- | --- |
| Trí | Xác nhận vòng đời ticket khi chờ duyệt, duyệt/từ chối reopen và thời điểm tính lại SLA; cập nhật SRS/SDD thống nhất |
| Cường + Trí | Đối chiếu schema, ràng buộc/index, API và chuyển trạng thái; bổ sung migration khi đã có dữ liệu cần giữ |
| Chính | Đối chiếu nhãn trạng thái, bộ lọc, màn chờ duyệt và các nút theo role/trạng thái |
| Khánh | Rà quyền trong Access Matrix cho REOPEN_PENDING và các thao tác duyệt/từ chối |
| Hoà + Cường | Chốt sự kiện audit khi gửi yêu cầu, duyệt và từ chối; đối chiếu thiết kế monitoring |
| Nhi | Rà các kiểm soát upload/deployment nếu luồng mới tác động tới thiết kế đã chốt |
| Khiêm | Đồng bộ SecRS, STRIDE, kế hoạch/expected result kiểm thử, risk và tham chiếu hồ sơ sau khi SE–IA thống nhất |

Không chuyển các dòng trên thành Done chỉ vì đã lập bảng. Kết quả cần có PR/tài liệu đã cập nhật và xác nhận kiểm tra.

## Cấu trúc thư mục

```text
docs/       Tài liệu và biểu mẫu 00–09, bảng phân công
evidence/   Bằng chứng có mô tả, nguồn gốc và nội dung đã che dữ liệu nhạy cảm
diagrams/   BPMN, DFD, ERD; file chỉnh sửa được và bản xuất
src/frontend/   Giao diện React hiện có — Chính
src/backend/    Vị trí backend — Cường/Trí; README chưa phải mã ứng dụng
src/monitoring/ Vị trí monitoring — Hoà; README chưa phải dịch vụ chạy được
database/       Schema và dữ liệu khởi tạo — Cường/Trí
docker-compose.yml  Cấu hình tích hợp — Nhi/Chính phối hợp SE
```

## Đầu ra tuần 1

Thời gian theo phân công: 19–25/10/2026. Mốc chung: duyệt rủi ro và requirement; SRS Lite, BPMN và backlog.

- Khiêm: repo/board, header 10 template, SecRS mục 1–3 bản nháp và quản lý tuần 1.
- Khánh, Hoà, Nhi: các dòng Security Requirements và đầu ra IA được giao.
- Trí, Cường, Chính: SRS, BPMN, Business Rules, Use Cases, NFR, screen flow và khung frontend theo bảng phân công.

Header khởi tạo của 10 biểu mẫu ngày 22/10/2026: P01, Group2, thành viên/vai trò, v0.1 và Draft. Đây là mốc khởi tạo; phiên bản hiện tại cần đọc trong từng tài liệu. Phần thân biểu mẫu do người được giao tiếp tục hoàn thiện; chuẩn hóa header không đồng nghĩa đã xong SRS/SecRS.

## Quy ước theo dõi

Giữ mã T1-01…T1-07 và HD/IA-HD/TH/TC/EV nhất quán. Board dùng Todo, In Progress, Review và Done. Chuyển Review khi có bản nháp và link; chỉ Done khi đầu ra được kiểm tra và có bằng chứng thực tế. Phần setup không tự đánh hoàn thành nội dung threat model hoặc phần việc của các thành viên.

Không đưa mật khẩu, token thật hoặc dữ liệu cá nhân nhạy cảm vào repo/evidence. Chỉ kiểm thử trong môi trường được phép, theo scope và quy trình phê duyệt của dự án. Hướng dẫn triển khai hiện có tại [DEPLOY_NOTES.md](DEPLOY_NOTES.md); frontend có [README riêng](src/frontend/README.md). Việc có Compose hoặc ảnh giao diện chưa chứng minh toàn bộ hệ thống đã chạy tích hợp hay đạt kiểm thử bảo mật.

