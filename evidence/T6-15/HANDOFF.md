# T6-15 — Hoàn thiện WAF và bàn giao

26/11/2026 · Nguyễn Trọng Khiêm · SE192700 · **In progress**.

## Đã có

- WAF đã vào main qua PR #38, merge `38a5a29400feeb4ba164b85448e920e140a19a5b`. File nguồn: [docker-compose.yml](https://github.com/Tam3332/P01-IT-Helpdesk/blob/38a5a29400feeb4ba164b85448e920e140a19a5b/docker-compose.yml), [nginx/nginx-waf.conf](https://github.com/Tam3332/P01-IT-Helpdesk/blob/38a5a29400feeb4ba164b85448e920e140a19a5b/nginx/nginx-waf.conf). Không còn bước “bỏ Draft PR #38”.
- Gói follow-up này thêm ngưỡng 5/4 tường minh và JSON 403 thân thiện, giữ TLS/header/rate limit/proxy/11m. Đã có 38/38 kiểm tra tĩnh trên Ubuntu cá nhân và nginx-t hai chế độ PASS sau TLS staging riêng. Chưa có nghiệm thu HTTP/VM nhóm; xem EV-HD-28_WAF/ubuntu-personal-20261126/README.md.
- ADR-05, 15 ca nghiệm thu, bảng exclusion và hợp đồng bàn giao audit được chuẩn bị để review. Bảng exclusion chưa có dòng đã duyệt, không có nghĩa không có false positive.

## Trình tự bàn giao

| Bước | Người | Việc và bằng chứng cần trả |
|---|---|---|
| 1 | Nhi | Review follow-up trên main 38a5a29; xác nhận giữ control, handler 403, audit privacy và ngoại lệ đóng băng T6-08 |
| 2 | Chính | Deploy DetectionOnly trong cửa sổ nhóm duyệt; trả SHA đầy đủ, digest, project, mode thực tế, thời gian, nginx-t và ảnh gốc |
| 3 | Khánh + nhóm SE | Luồng hợp lệ P1/P3 approve/reject, upload gần 10 MB, JSON/ký tự hợp lệ; Actual và EV theo 15 ca |
| 4 | Khiêm + Nhi | Rà false positive; exclusion chỉ có rule/route/parameter/lý do/evidence/reviewer, không tắt ruleset rộng |
| 5 | Khánh | Kiểm chặn và regression theo kế hoạch đã duyệt; On PL1 và ngưỡng phải được ghi nhận, 403 có audit cùng request |
| 6 | Hoà | R3 từ audit đã che; phân biệt 403 RBAC và WAF, kiểm correlation và thiếu trường |
| 7 | Trí + Khiêm | Gộp ADR/SDD, SecRS và 04 theo kết quả thực; cập nhật EV-HD-28_WAF và trạng thái |

## Evidence và build

Mỗi lượt lưu: Run ID, SHA đầy đủ, tag nếu có, môi trường, WAF mode render thực tế, digest, giờ/múi giờ, người chạy, case/Expected/Actual, file ảnh/log gốc và hash. Không chụp JWT, secret, mật khẩu hoặc IP ngoài phạm vi được chia sẻ. Ảnh phải là màn hình chạy thật, không dựng terminal hay dùng lại ảnh PoC làm bằng chứng VM nhóm. Gói hiện có ba PNG mới: 38 static PASS, nginx-t FAIL do quyền key, retest syntax PASS. Cả ba đều là Ubuntu cá nhân.

Đường dẫn đề nghị: `evidence/T6-15/EV-HD-28_WAF/<run-id>/`. Không tạo file ảnh/report giả để lấp chỗ trống. Parts AZ và chế độ RelevantOnly có thể khiến request hợp lệ không tạo bản ghi; kiểm log rỗng không tự kết luận WAF hỏng.

## Điều kiện Done

Main có bản đã review; VM nhóm triển khai đúng SHA; có hai chế độ và valid-flow/regression; privacy/R3/stats đạt; threshold/document sign-off có người/ngày và EV. Hiện chưa đủ. Không đổi tag rc1 cũ hoặc gộp kết quả từ build khác vào cùng một run.

## Kết quả cá nhân mới đã bổ sung

Xem [timeline và ảnh/log](EV-HD-28_WAF/ubuntu-personal-20261126/README.md), [bảng Actual syntax](personal_syntax_results.csv), [lưu ý TLS khi deploy](TLS_DEPLOYMENT_NOTE.md). Giữ 15 ca nghiệm thu VM nhóm ở NOT RUN/chờ; không đổi Actual nhóm từ kết quả cá nhân.

## Gói hoàn thiện cho Chính

Bắt đầu tại [DEPLOY_CHINH.md](DEPLOY_CHINH.md). Source chuẩn bị dựa main c61ccb2124a8e77529db815a21e72d00d2380cda. Công cụ snapshot/preflight/smoke/privacy, sửa cổng lab-check, TLS riêng và access-log metadata đã bổ sung. Kết quả kiểm mới tại PREDEPLOY_VERIFICATION.md; các ca VM nhóm vẫn NOT RUN. Người dùng yêu cầu Chính deploy; chưa có biên nhận Chính đã nhận hoặc đã triển khai.
