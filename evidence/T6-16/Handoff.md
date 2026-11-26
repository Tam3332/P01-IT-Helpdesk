# Bàn giao T6 16 cho T6 02 và T6 01

Before v0.5-mvp 35c573d20d4244985e617e8a056f880faaadf1db đã được kiểm tra trên Ubuntu cá nhân, originhttps://127.0.0.1:28443, WAF absent, ZAP Docker2.17.0. Báo cáo và context đã loại token nằm trong EV-HD-27_VA_before_ubuntu_20261126/zap/. Test_Cases.csv gồm kết quả HTTP riêng từng role và năm lượt ZAP.

Khánh dùng automation/README.md và Coverage.md để nạp JWT mới vào đúng role; đối chiếu role-check và coverage thực, không chỉ tên context. Bản import là GET subset của OpenAPI, giữ hash bản gốc và danh sách operation loại. Active chỉ SQLi rule40018 có giới hạn; các write/upload và full browser flow không được scanner bao phủ. Phần này phải được ghi tương ứng ở AFTER.

T6-01 giữ BUG-HD-008 Low theo Defect Log hiện hành: Before đổi mật khẩu200 nhưng token cũ200. Bản sửa PR44 có trên main, không đánh PASS AFTER từ lượt Before này. Bổ sung BUG-HD-009 CSP inline style và BUG-HD-010 Google Fonts SRI, Medium do ZAP gán, owner và cách retest trong Defect_Log.md. Không coi thiếu hardening là bằng chứng đã khai thác XSS.

Seed Before riêng đã được khôi phục khi cleanup-manifest.json xác nhận. VM/project p01-it-helpdesk hiện có được đối chiếu container ID trước/sau. Không rollback hoặc reset VM nhóm. Tài liệu02§6 và04§3/§4/§6 đi kèm bản cập nhật kết quả. Sau khi publish, dùng đúng commit artifact cho phép so sánh AFTER; không nhầm artifact commit với baseline chạy ứng dụng.

Đã chuẩn bị gói bàn giao. Chưa tuyên bố Khánh đã nhận, nhóm đã chấp nhận risk hoặc security sign-off đã xong. Không gửi tin nhắn thay người dùng.

Bổ sung Low: BUG-HD-011 đề xuất cho HSTS trùng; header thực tại EV-HD-27_VA_before_ubuntu_20261126/api-header-check.txt. Khiêm đã duyệt gói xuất bản ngày 26/11/2026; mã009–011 được ghi trong Defect Log. Owner/retest là phân công trong hồ sơ, chưa phải xác nhận tiếp nhận của từng người.
