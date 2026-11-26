# T6 15 bàn giao triển khai WAF

Nguyễn Trọng Khiêm · SE192700 · 26/11/2026.

Phần cấu hình và tài liệu được chuẩn bị trên main `c61ccb2124a8e77529db815a21e72d00d2380cda` để Nhi review và Chính deploy. Bắt đầu tại [hướng dẫn cho Chính](DEPLOY_CHINH.md). Gói này chưa phải biên bản nghiệm thu VM nhóm.

- [ADR 05](../../docs/ADR-05_WAF.md)
- [15 ca nghiệm thu](checklist.csv) và [kịch bản từng ca](TEST_GUIDE.md)
- [Audit bàn giao Hoà](AUDIT_HANDOFF.md), [bảng exclusion](exclusions.csv)
- [TLS](TLS_DEPLOYMENT_NOTE.md), [hướng dẫn rollback](DEPLOY_CHINH.md#rollback)
- [Ảnh Ubuntu cá nhân đã có](EV-HD-28_WAF/ubuntu-personal-20261126/README.md)

Ảnh và nginx-t ngày 26/11 thuộc bản cấu hình trước khi thêm access-log metadata, X-Request-Id và chặn Actuator trong gói mới. Giữ để tham khảo; Chính phải chạy nginx-t và HTTP trên đúng SHA sẽ deploy. Kết quả kiểm tra mới được ghi trong `PREDEPLOY_VERIFICATION.md` khi đóng gói.
