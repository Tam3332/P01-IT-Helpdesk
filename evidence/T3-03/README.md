# T3-03 — Evidence phát triển offline (Monitoring v1)

**Phạm vi:** mô phỏng offline bằng audit JSONL tổng hợp. Không có dữ liệu thật, không chạy VM lab, không gọi API đăng nhập thực tế. **Không dùng các kết quả này để đánh PASS cho SEC-TC-HD-02 / EV-HD-22.**

- `offline_unit_tests.txt`: unit tests 18/18 PASS (kiểu dữ liệu actor số, R1/R2, lọc login 403, legacy v0).
- `offline_backend_schema_replay.txt`: 12 sự kiện mẫu theo hình dạng Backend -> 2 cảnh báo.
- `offline_streaming_check.txt`: kiểm tra từ v0 (lưu lịch sử).
- `offline_streaming_v1_check.txt`: chạy chế độ `--follow --from-start` với 12 sự kiện mô phỏng mới nối vào file, tạo 2 alert; vẫn chưa kết nối VM thật.
- `offline_replay_result.txt`: kết quả v0, giữ nguyên lịch sử mẫu.

Mã nguồn/đầu vào tương ứng: `src/monitoring/monitor.py`, `src/monitoring/tests/`, `src/monitoring/examples/audit.backend.sample.jsonl`.

**Chưa kiểm được:** `src_ip` thật phía sau Nginx, việc ghi JSONL từ Backend trên VM, 429/lockout, quyền truy cập log, rotation/retention. Đây là những mục cần Cường/Nhi/Khiêm xác nhận khi test lab.
