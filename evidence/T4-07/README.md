# Evidence T4-14: deploy lab từ tag v0.4-week4

Ngày kiểm: 12/11/2026. Người thực hiện: Nguyễn Nhật Chính (SE196524). Mọi kết quả lấy từ VM Ubuntu lab, không chỉnh sửa.
Không có mật khẩu, token hay nội dung `.env` trong các tệp này.

| Mục | Kết quả | Tệp |
|---|---|---|
| Code triển khai | tag `v0.4-week4` = commit `a378fdb` (Merge PR #18, trùng `main` lúc deploy) | (xem DEPLOY_NOTES) |
| 4 container chạy | `p01-db`, `p01-backend`, `p01-frontend` healthy; `p01-gateway` Up | `lab-check_8443.txt` mục 1 |
| HTTPS và header bảo mật | HTTP 200; có HSTS, nosniff, X-Frame-Options DENY, CSP | `lab-check_8443.txt` mục 3 |
| Swagger bị chặn | 404 | `lab-check_8443.txt` mục 4 |
| API không token | 401 | `lab-check_8443.txt` mục 5 |
| Đăng nhập 4 vai trò | EMPLOYEE, IT_STAFF, IT_MANAGER, ADMIN đều đăng nhập được | `lab-check_8443.txt` mục 6 |
| Volume ghi được | `audit_events.jsonl` và `/app/uploads` ghi được | `lab-check_8443.txt` mục 7 |
| Volume giữ dữ liệu sau restart và `down`/`up` (không `-v`) | Đạt: tệp dấu vết giữ nguyên, audit 8 dòng trước và sau, tài khoản DB còn | `volume-persist.txt` |
| Upload 10 MB qua Nginx | HTTP 201, `fileSize` 10485760 | `upload-limits.txt` |
| Tệp 11 MB | HTTP 413 do Nginx chặn | `upload-limits.txt` |
| Giao diện web trên lab | Mở `https://100.124.80.30:8443/` qua Tailscale, đăng nhập `employee2@` thành công, thấy trang Tổng quan vai trò Nhân viên | `ui-employee2-dashboard.png` |
| Cổng mở ra ngoài của dự án | Chỉ `0.0.0.0:8443`; Postgres 5432 và backend 8080 chỉ trong mạng Docker | `lab-check_8443.txt` mục 2 và phần đối chiếu |

## Kết quả script lab-check: vẫn ghi "có mục lỗi" (mục 2)

`scripts/lab-check.sh 8443` kết thúc bằng `KẾT QUẢ: có mục lỗi` vì mục 2 báo `5432/8080 đang mở ra máy chủ`. Đây không phải lỗi của hệ thống dự án:

- Dòng `127.0.0.1:8080` thuộc container `helpdesk-web` có sẵn trên VM (do thử nghiệm riêng của lab), được ánh xạ `80/tcp -> 127.0.0.1:8080` (xem `docker port helpdesk-web`).
- `docker compose ps` cho thấy dự án chỉ mở cổng `8443`; backend và Postgres chỉ có cổng nội bộ trong mạng Docker.
- Script đối chiếu theo `grep ":8080"`, không phân biệt địa chỉ bind hay container sở hữu nên báo nhầm. Không sửa script và không sửa kết quả.
- Mục 1 và 7 trong lần chạy đầu (không `sudo`) lỗi `permission denied` do tài khoản `nhi` không thuộc nhóm `docker`; lần chạy lại bằng `sudo` đạt.

## Chưa kiểm trong T4-14 (không ghi là đạt)

- Giao diện web mới chụp với `employee2@` (Nhân viên). Các vai trò IT Staff, IT Manager, Admin mới kiểm đăng nhập bằng API trong `lab-check.sh`, chưa chụp giao diện.
- Rate limit (429) trên Nginx không thuộc phạm vi T4-14, do Nhi kiểm ở T4-11.

## Cách truy cập lab

VM chạy trên VirtualBox của Nhi (Adapter 1 NAT, Adapter 2 Internal Network `helpdesk-lab`, Ubuntu Server `192.168.50.10`). Để cả nhóm vào từ máy riêng, nhóm dùng Tailscale trên VM Server: `https://100.124.80.30:8443/` (chứng chỉ tự ký, trình duyệt sẽ cảnh báo). Mỗi thành viên cần được Nhi mời vào mạng Tailscale. VM phải đang bật mới truy cập được.
