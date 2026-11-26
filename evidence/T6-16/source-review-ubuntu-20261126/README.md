# Source review trên Ubuntu — 26/11/2026

Lượt 10:40:38 VN chạy 38 kiểm tra tĩnh cho gói T6-15/T6-16: PASS trên Ubuntu/Linux. [Ảnh gốc](01_38-kiem-tra-Ubuntu.png), [JSON](38-checks-ubuntu.json), [log](static-session.log), [worktree inventory](ubuntu-worktree-inventory.txt).

Mã Before trong gói vẫn `35c573d20d4244985e617e8a056f880faaadf1db`; worktree cá nhân T5-16 đang ở đúng commit này. Gói static dùng snapshot riêng để kiểm source/permalink, CSV/metadata và patch. Không chạy ứng dụng baseline hoặc gửi request từ lượt source review này.

**Không phải VA runtime**: chưa ZAP Baseline/API, chưa context bốn role, chưa kiểm AuthN/JWT/API/HTTP. Giữ 18 ca NOT RUN và 9 đầu mục evidence/handoff còn thiếu. Các kết quả T5-02/T5-07 lịch sử vẫn mang môi trường và build riêng.
