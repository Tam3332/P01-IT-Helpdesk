# Kiểm tra phần chuẩn bị WAF T6 15

Ngày 26/11/2026. Base main `c61ccb2124a8e77529db815a21e72d00d2380cda`. Người phụ trách Nguyễn Trọng Khiêm.

| Phạm vi đã kiểm | Kết quả |
|---|---|
| Source/YAML/contract, cấu hình và tài liệu | 37/37 PASS; chi tiết tại predeploy-source-checks.json |
| Công cụ Python và lab-check trên Ubuntu cục bộ qua WSL | 14/14 PASS; dùng fixtures/stubs để kiểm hành vi công cụ, không phải HTTP/WAF runtime |
| Shell syntax | `sh -n scripts/lab-check.sh` PASS |
| Tài liệu Word | Xuất PDF bằng Word, tạo ảnh bằng pipeline render_docx.py; đã xem các trang: SecRS31, SDD45, Test Plan58. Đồng bộ chân trang SDD và sửa pageBreakBefore ở §8.1 |
| Bảo toàn phạm vi | PostgreSQL/backend/frontend, networks và volumes trong Compose giữ nguyên; không sửa mã ứng dụng. Giữ đoạn VA Before và mã finding/BUG hiện có |

14 kiểm tra công cụ gồm: cổng WAF8443/cổng cũ443; body lỗi login không in ra; snapshot ngoài Git và không ghi đè; pin image/env thực của gateway; sao cấu hình để checkout sau không đổi rollback; giữ TLS gốc và external volume/network; từ chối mount không khớp; phát hiện port DB public/engineOff; JSON403 phải có request ID khớp; không chấp nhận RBAC403 làm WAF block; DetectionOnly không kỳ vọng chặn; không theo redirect; pattern/canary privacy không in dữ liệu; log rỗng và output cũ không được chấm PASS/ghi đè.

Tái lập kiểm công cụ trên Ubuntu:

```bash
python3 -m unittest discover -s scripts/waf/tests -v
sh -n scripts/lab-check.sh
```

Máy Windows/WSL hiện không có Docker để chạy image WAF của bản mới. nginx-t trong ảnh Ubuntu cá nhân buổi trước thuộc hash cấu hình cũ `b99e9a409f71762a432804af4d4d3d1875bd784c913f2a2d40569a61c0b33b33`; không dùng để chứng nhận bản bổ sung access-log/X-Request-Id/Actuator này.

Chính cần chạy nginx-t trước/sau deploy, HTTP hai chế độ, luồng P1/P3/upload/UI, audit privacy/R3, stats và rollback theo DEPLOY_CHINH.md. Những ca đó vẫn NOT RUN, chưa có biên nhận triển khai VM nhóm. ADR/SecRS chưa Approved; T6-15 chưa đủ điều kiện Done.
