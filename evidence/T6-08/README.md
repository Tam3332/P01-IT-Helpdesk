# T6-08 Hồ sơ bàn giao và việc còn mở

Nguyễn Trọng Khiêm · P01 Group2 · 25/11/2026 · **In progress**

## Đã cập nhật

- SDD v2.2 Draft: mapping WAF/lockout/P3/audit theo đúng PR và evidence; giữ phần kiến trúc/ADR của SE. WAF là Proposed qua PR38, chưa mô tả đã chạy trên VM nhóm.
- 02 v1.9 Draft, 04 v2.4 Draft và báo cáo T6-01 phân biệt bản sửa đã merge với AFTER chưa có.
- 08 v1.9 Draft: RACI, Sprint, contribution của Khiêm, Risk log và §5.1 bàn giao tuần6.
- [Sổ chuyển việc](Task_Transfer_Register.md): đối chiếu41 dòng tuần1–5 và task kế thừa tuần6. Done ở dòng chuyển tiếp là hoàn tất bàn giao, không thay PASS kiểm thử.
- File [7 tuần](../../docs/P01_Phan_cong_7_tuan.xlsx): bổ sung các task tuần6 hiện có trong Sheet, giữ kế hoạch tuần7. Sheet live là nguồn trạng thái khi có thay đổi tiếp theo.

## Biên bản và xác nhận phạm vi

Khiêm xác nhận “chưa có” ngày25/11/2026 đối với dữ kiện thực tế biên bản14/11 và Review2 ngày21/11, góp ý/quyết định GV và xác nhận bộ01/02/03.

| Mục | 14/11 | Review2 21/11 |
|---|---|---|
| Giờ họp | Chưa có | Chưa có |
| Người tham dự thực tế | Chưa có | Chưa có |
| Build dùng demo và kết quả tại buổi họp | Chưa có | Chưa có |
| Góp ý/quyết định GV | Chưa có | Chưa có |
| Phạm vi01/02/03 đã được GV xác nhận | Chưa có | Chưa có |
| Nguồn xác nhận/biên bản ký | Chưa có | Chưa có |

Khiêm và Trí ghi dữ kiện, nguồn, người xử lý và hạn khi nhận được. Không ghi buổi họp đã diễn ra từ một lịch dự kiến. SecRS giữ Draft; chưa Reviewed/Approved.

## Đóng băng tính năng và ngoại lệ WAF

Theo Sheet T6-08: từ23/11 chỉ nhận fix/test/docs có BUG-ID; tuần6 tập trung sửa và retest. PR40/43/44 là các đầu việc audit/triage/BUG-HD-008 đã merge.

WAF PR38 là control đã có trong kế hoạch tuần5. Ghi **ngoại lệ Proposed/chờ xác nhận** trong08 §5.1, với điều kiện review, evidence, regression và scope hợp lệ. Chưa có quyết định nhóm/GV phê duyệt ngoại lệ được cung cấp. Nội dung này không cấp quyền chạy scan, truy cập mục tiêu hay merge PR.

Risk R-06 lịch sử trong08 mô tả PC-06/NFR/SCR. Giữ mã này và thêm R-06-FREEZE để đối chiếu cách gọi R-06 ở Sheet tuần6; không xóa nghĩa lịch sử.

## Việc cần nhận để đóng T6-08

| Đầu vào còn thiếu | Người phối hợp | Nơi ghi nhận |
|---|---|---|
| Biên bản14/11 và21/11, góp ý/quyết định thực tế | Khiêm, Trí; GV cung cấp/xác nhận | 08 §5 |
| Phạm vi bộ01/02/03 và quyết định Draft/Reviewed | Khiêm, Trí, GV | 08 §5;02header |
| Mapping SDD cuối theo audit/WAF/contract sau review | Hoà, Nhi, Cường, Chính, Trí; Khiêm tổng hợp | T6-09/10/11/14/15;03§7–8 |
| Xác nhận đã nhận việc tồn tuần1–5 | Owner task kế thừa | Sổ chuyển việc và Sheet Tuần6 |
| AFTER VM và kết luận before/after | Chính xác nhận build; Khánh/Hoà/Nhi kiểm; Khiêm tổng hợp | T6-01/02/03/04/15/16 |

Mốc theo kế hoạch:26–27/11 retest;28/11 review nội bộ/final;29/11 nộp04/05. Giữ T6-08 In progress khi các đầu vào bắt buộc còn thiếu.

Nguồn: [Sheet Tuần6](https://docs.google.com/spreadsheets/d/18NX_o1GiopEFlLNYtmjXm7pl4dlEY1cn7u21AKC8k7U/edit#gid=923885192), [PR44](https://github.com/Tam3332/P01-IT-Helpdesk/pull/44), [DEPLOY_NOTES](../../DEPLOY_NOTES.md), [báo cáo T6-01](../T6-01/Before_After_Vulnerability_Report.md).
