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
