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

## Bổ sung sau review ngày 26/11

Access log thêm `$uri` để xác định route, không ghi query; vẫn cần rà PII trong path. `lab-check.sh` bắt buộc DEMO_PASSWORD, không còn mật khẩu fallback. Nhập bằng `read -r -s -p` trong Bash, export rồi chạy script bằng sh; unset sau khi dùng. Cú pháp read -s/-p không dùng với sh/dash. DEPLOY_NOTES và DEPLOY_CHINH đã cập nhật cùng yêu cầu.

Đã chạy lại 15/15 kiểm tra tooling bằng fixtures/stubs và sh -n sau sửa. Bộ 37 source/14 tooling ở trên là kết quả lịch sử của 2ca3143, không phải số kiểm tra mới. Bản cấu hình mới chưa chạy nginx -t/HTTP hoặc VM nhóm. DOCX và Excel được tách khỏi PR #52 sang PR tài liệu riêng để Trí review; Nhi/Chính vẫn phải chọn đúng SHA cuối cùng và kiểm TLS/runtime trước triển khai. BUG-HD-012 về credential trong ứng dụng được theo dõi ở PR tài liệu, chưa được sửa bằng thay đổi lab-check này.
