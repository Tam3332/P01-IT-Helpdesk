# EV-HD-22 — Kiểm tra Monitoring sidecar trên VM (bổ sung)

**Task:** T4-10 — SEC-TC-HD-02 / EV-HD-22
**Nguồn:** Kết quả kiểm tra do Nhi cung cấp trong cuộc trao đổi ngày 16/11/2026; các file không ghi rõ thời điểm chạy lệnh hoặc commit/tag đang triển khai.
**Phạm vi:** Xác nhận trạng thái container, Monitoring khởi động và khả năng đọc Audit Log. Không thay thế kết quả kiểm thử live alert.

## 1. Kết quả từ VM

| Hạng mục | Bằng chứng nhận được | Đánh giá |
|---|---|---|
| Container Monitoring | `p01-lab-v04-monitoring-1`, trạng thái `Up 5 minutes`, image `python:3.12-alpine` | **Đạt** — container đang chạy tại thời điểm ghi nhận |
| Lệnh Monitoring khởi động | `[monitor] watch /audit/audit_events.jsonl; append alerts to /alerts/alerts.jsonl` | **Đạt** — log xác nhận chế độ theo dõi và đường dẫn đầu ra đã cấu hình |
| Đọc file Audit | `AUDIT_READABLE: True`; `FIRST_LINE_READ_OK: True`; `FILE_SIZE: 37524` | **Đạt theo kết quả kiểm tra được cung cấp** — đọc được file khác rỗng |
| Ghi `alerts.jsonl` thành công | Không có file alert hoặc kết quả kiểm tra ghi để đối chiếu | **Chưa xác minh** |
| Sinh cảnh báo R1/R2 thực tế ngay trên sidecar VM | Không có cặp sự kiện Audit/Alert live cùng thời điểm | **Chưa xác minh** |

## 2. Bằng chứng đính kèm

- `monitoring-container-status.txt` — trạng thái container từ VM.
- `monitoring-logs.txt` — log khởi động của Monitoring.
- `monitoring-audit-read-check.txt` — kết quả kiểm tra file Audit có thể đọc.

Ba file trên là bản sao nguyên nội dung Nhi cung cấp, chưa sửa kết quả.

## 3. Đối chiếu các kiểm thử đã thực hiện

- HTTP/JWT live trên VM: **10 assertions PASS** (xem hai file CSV kiểm thử ngày 14/11 trong cùng thư mục).
- R1 từ Audit Log thật: **8 events, 0 skipped, 1 alert HIGH** bằng **offline replay**, không phải cảnh báo được chứng minh là do sidecar VM tạo ra.
- Unit test Monitoring và synthetic replay: được ghi nhận riêng trong `offline_precheck.txt`.

## 4. Kết luận và giới hạn

**Xác nhận đạt phần triển khai:** Monitoring sidecar có container đang chạy, chương trình khởi động và có kết quả đọc Audit Log thực tế trên VM. Đây là bằng chứng hoàn thành bước *khởi chạy/đọc log*, không phải bằng chứng hoàn thành toàn bộ chu trình phát cảnh báo live.

**Chưa chứng minh:** Sidecar tạo/ghi một cảnh báo thật vào `alerts.jsonl`; HTTP 429 của Nginx; JWT hết hạn theo thời gian; khóa tài khoản từ Backend. Không đánh dấu toàn bộ EV-HD-22 hoặc T4-10 là `Done` chỉ dựa vào ba file này.

**Trạng thái đề xuất:** `Review` (đã có bằng chứng VM sidecar hoạt động và đọc Audit Log, còn live alert cùng các ca kiểm thử bổ sung).

**Bảo mật:** Không đưa bản Audit Log chưa che hoặc Alert Log chứa thông tin định danh thực vào GitHub. Chỉ lưu trạng thái dịch vụ và kết quả kiểm tra không chứa dữ liệu nhạy cảm.
