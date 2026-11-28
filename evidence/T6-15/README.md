# T6 15 bàn giao triển khai WAF

Nguyễn Trọng Khiêm · SE192700 · 26/11/2026.

Phần cấu hình và tài liệu được chuẩn bị trên main `c61ccb2124a8e77529db815a21e72d00d2380cda` để Nhi review và Chính deploy. Bắt đầu tại [hướng dẫn cho Chính](DEPLOY_CHINH.md). Gói này chưa phải biên bản nghiệm thu VM nhóm.

- [ADR 05](../../docs/ADR-05_WAF.md)
- [15 ca nghiệm thu](checklist.csv) và [kịch bản từng ca](TEST_GUIDE.md)
- [Audit bàn giao Hoà](AUDIT_HANDOFF.md), [bảng exclusion](exclusions.csv)
- [TLS](TLS_DEPLOYMENT_NOTE.md), [hướng dẫn rollback](DEPLOY_CHINH.md#rollback)
- [Ảnh Ubuntu cá nhân đã có](EV-HD-28_WAF/ubuntu-personal-20261126/README.md)

Ảnh và nginx-t ngày 26/11 thuộc bản cấu hình trước khi thêm access-log metadata, X-Request-Id và chặn Actuator trong gói mới. Giữ để tham khảo; Chính phải chạy nginx-t và HTTP trên đúng SHA sẽ deploy. Kết quả kiểm tra mới được ghi trong `PREDEPLOY_VERIFICATION.md` khi đóng gói.

## Bổ sung sau review ngày 26/11

Access log thêm `$uri` để xác định route, không ghi query; vẫn cần rà PII trong path. `lab-check.sh` bắt buộc DEMO_PASSWORD, không còn mật khẩu fallback. Nhập bằng `read -r -s -p` trong Bash, export rồi chạy script bằng sh; unset sau khi dùng. Cú pháp read -s/-p không dùng với sh/dash. DEPLOY_NOTES và DEPLOY_CHINH đã cập nhật cùng yêu cầu.

Đã chạy lại 15/15 kiểm tra tooling bằng fixtures/stubs và sh -n sau sửa. Bộ 37 source/14 tooling ở trên là kết quả lịch sử của 2ca3143, không phải số kiểm tra mới. Bản cấu hình mới chưa chạy nginx -t/HTTP hoặc VM nhóm. DOCX và Excel được tách khỏi PR #52 sang PR tài liệu riêng để Trí review; Nhi/Chính vẫn phải chọn đúng SHA cuối cùng và kiểm TLS/runtime trước triển khai. BUG-HD-012 về credential trong ứng dụng được theo dõi ở PR tài liệu, chưa được sửa bằng thay đổi lab-check này.

## Trạng thái đối chiếu ngày 28 tháng 11 năm 2026

PR #52 đã merge vào main tại 11df1cf; head trước merge 77046f2 sau khi xử lý conflict tài liệu với #56. rc1 ed229a3 được Khánh retest trong #61 vẫn là WAF none. Chưa có manifest/nginx-t/HTTP cho WAF trên VM nhóm được cung cấp trong lượt cập nhật hồ sơ này; giữ 15 ca VM ở trạng thái đã có, không tự đánh PASS.

Finding Gitleaks tại test_tools.py:127 của commit 2ca3143 là fixture privacy scanner; chỉ finding đó được phân loại false positive. Không allowlist cả thư mục tests hoặc kết luận Gitleaks toàn repo PASS. Nhi cần ghi ref/range thực tế: commit này không nằm trong ancestry rc1 ed229a3. Căn cứ và giới hạn tại [quyết định IA 28/11](../T6-01/REVIEW_DECISIONS_20261128.md#5-finding-gitleaks-trong-fixture-waf).

BUG012 BE/FE đã có bản sửa trên main qua #57/#60; credential seed/VM và AFTER final vẫn mở, không dùng thay đổi lab-check để đóng. Các đoạn ngày 26/11 ở trên là lịch sử chuẩn bị.
