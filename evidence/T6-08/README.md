# T6-08 Hồ sơ bàn giao và việc còn mở

Nguyễn Trọng Khiêm · P01 Group2 · 26/11/2026 · **In progress**

## Đã cập nhật

- SDD v2.2 Draft: mapping WAF/lockout/P3/audit theo đúng PR và evidence; giữ phần kiến trúc/ADR của SE. PR #38 đã merge WAF vào main sau rc1; follow-up PR #52 có SDD v2.3 còn review. Triển khai VM nhóm và ngoại lệ đóng băng vẫn chưa được xác nhận.
- Main có 02 v1.10 Draft, 04 v2.5 Draft; Bản gộp tài liệu 02 v1.11 / 04 v2.6 Draft được tách khỏi PR #52 và báo cáo T6-01 phân biệt bản sửa đã merge với AFTER chưa có.
- 08 main v2.0 Draft, bản chuẩn bị v2.1 Draft: RACI, Sprint, contribution của Khiêm, Risk log và §5.1 bàn giao tuần 6.
- [Sổ chuyển việc](Task_Transfer_Register.md): đối chiếu 41 dòng tuần 1–5 và task kế thừa tuần 6. Done ở dòng chuyển tiếp là hoàn tất bàn giao, không thay PASS kiểm thử.
- File [7 tuần](../../docs/P01_Phan_cong_7_tuan.xlsx): bổ sung các task tuần 6 hiện có trong Sheet, giữ kế hoạch tuần7. Sheet live là nguồn trạng thái khi có thay đổi tiếp theo.

## Biên bản và xác nhận phạm vi

Khiêm xác nhận “chưa có” ngày 25/11/2026 đối với dữ kiện thực tế biên bản 14/11 và Review 2 ngày 21/11, góp ý/quyết định GV và xác nhận bộ 01/02/03.

| Mục | 14/11 | Review 2 21/11 |
|---|---|---|
| Giờ họp | Chưa có | Chưa có |
| Người tham dự thực tế | Chưa có | Chưa có |
| Build dùng demo và kết quả tại buổi họp | Chưa có | Chưa có |
| Góp ý/quyết định GV | Chưa có | Chưa có |
| Phạm vi01/02/03 đã được GV xác nhận | Chưa có | Chưa có |
| Nguồn xác nhận/biên bản ký | Chưa có | Chưa có |

Khiêm và Trí ghi dữ kiện, nguồn, người xử lý và hạn khi nhận được. Không ghi buổi họp đã diễn ra từ một lịch dự kiến. SecRS giữ Draft; chưa Reviewed/Approved.

## Đóng băng tính năng và ngoại lệ WAF

Theo Sheet T6-08: từ23/11 chỉ nhận fix/test/docs có BUG-ID; tuần 6 tập trung sửa và retest. PR40/43/44 là các đầu việc audit/triage/BUG-HD-008 đã merge.

WAF PR #38 là control đã có trong kế hoạch tuần 5. Ghi **ngoại lệ Proposed/chờ xác nhận** trong 08 §5.1, với điều kiện review, evidence, regression và scope hợp lệ. Chưa có quyết định nhóm/GV phê duyệt ngoại lệ được cung cấp. Nội dung này không cấp quyền chạy scan, truy cập mục tiêu hay merge PR.

Risk R-06 lịch sử trong 08 mô tả PC-06/NFR/SCR. Giữ mã này và thêm R-06-FREEZE để đối chiếu cách gọi R-06 ở Sheet tuần 6; không xóa nghĩa lịch sử.

## Việc cần nhận để đóng T6-08

| Đầu vào còn thiếu | Người phối hợp | Nơi ghi nhận |
|---|---|---|
| Biên bản 14/11 và 21/11, góp ý/quyết định thực tế | Khiêm, Trí; GV cung cấp/xác nhận | 08 §5 |
| Phạm vi bộ 01/02/03 và quyết định Draft/Reviewed | Khiêm, Trí, GV | 08 §5;02header |
| Mapping SDD cuối theo audit/WAF/contract sau review | Hoà, Nhi, Cường, Chính, Trí; Khiêm tổng hợp | T6-09/10/11/14/15;03§7–8 |
| Xác nhận đã nhận việc tồn tuần 1–5 | Owner task kế thừa | Sổ chuyển việc và Sheet Tuần 6 |
| AFTER VM và kết luận before/after | Chính xác nhận build; Khánh/Hoà/Nhi kiểm; Khiêm tổng hợp | T6-01/02/03/04/15/16 |

Mốc theo kế hoạch: 26–27/11 retest; 28/11 review nội bộ/final; 29/11 nộp 04/05. Giữ T6-08 In progress khi các đầu vào bắt buộc còn thiếu.

Nguồn: [Sheet Tuần 6](https://docs.google.com/spreadsheets/d/18NX_o1GiopEFlLNYtmjXm7pl4dlEY1cn7u21AKC8k7U/edit#gid=923885192), [PR #44](https://github.com/Tam3332/P01-IT-Helpdesk/pull/44), [DEPLOY_NOTES](../../DEPLOY_NOTES.md), [báo cáo T6-01](../T6-01/Before_After_Vulnerability_Report.md).

## Đối chiếu thêm ngày 26 tháng 11

Đã đọc trực tiếp gói Review 2 và nội dung 9 slide trên Drive. Checklist/kịch bản/slide có tồn tại; mẫu biên bản vẫn trống, không chứng minh buổi họp hoặc quyết định GV. Không còn đánh “không mở được” cho hai nguồn này. Slide giữ giá trị bản chuẩn bị 19–20/11; thông tin build/BUG cũ cần phụ lục cập nhật, không sửa lịch sử thành kết quả họp.

PR #46/50 đã merge; PR #52 thay #51 do đổi tên nhánh, giữ commit 2ca3143. Chưa có review/merge cho follow-up và chưa có manifest deploy nhóm trong các nguồn đã đọc. T6-16: nguồn Sheet R17 đã có phương án lab cá nhân, 26/11 chạy đúng baseline, trễ 2 ngày so hạn 24/11; không phải VM nhóm/Kali. BEFORE đã có, AFTER và biên nhận Khánh còn chờ. Hai dòng chuyển T5-15/16 là đề xuất, chưa ghi Sheet.

Ngoại lệ WAF/JSON403/log format/Actuator ở tuần đóng băng vẫn Proposed; người dùng duyệt xuất bản không thay quyết định kỹ thuật của nhóm/GV. Biên bản 14/11,21/11 và xác nhận 01/02/03 tiếp tục chưa có nguồn xác nhận, không dựng lại. Khiêm/Trí xin thông tin thực tế; chủ nhiệm/owner trả kết quả đúng build trước khi đổi Reviewed/Approved.

## Quyết định và phiên bản ngày 28 tháng 11 năm 2026

Các phần 25–26/11 ở trên giữ làm lịch sử. Hiện PR #52 đã merge 11df1cf, #57/#60 sửa BE/FE BUG012 đã merge, #64 self-host font đã merge 8cd144c. #61 evidence rc1, #62 i18n và #63 phần seed/docs còn Open lúc đối chiếu. Không ghi các PR đó là đã vào final.

Nhận 106/106 assertions đã chạy trong PR #61 theo đúng rc1/WAF none; T6-02 In progress. BUG008 biên cùng giây còn Open/chưa Accepted; BUG012 High/Open, cần seed/rotation/AFTER; BUG009 Accepted risk bởi Khiêm 28/11 chỉ lab v0.6-final với điều kiện ở 02 §8.2; BUG010 fix merged/chờ ZAP lab; BUG011 Open/chưa Accepted.

02 v1.12 Draft, 04 v2.7 Draft, 08 v2.2 Draft đã cập nhật. [Quyết định review 28/11](../T6-01/REVIEW_DECISIONS_20261128.md) ghi nguồn và checklist trước final. Biên bản/GV chưa có xác nhận mới. Không chuyển T6-08 Done, không ghi Sheet/Drive hoặc tạo tag final trong lượt cập nhật repo.
