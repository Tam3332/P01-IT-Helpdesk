# Triển khai P01 bằng Docker Compose (VM Ubuntu lab)

## Thành phần
| Dịch vụ | Container | Ghi chú |
|---|---|---|
| postgres | p01-db | PostgreSQL 16, nạp `database/schema.sql`, `init_data.sql`, `init_demo_users.sql` lần đầu |
| backend | p01-backend | Spring Boot (Java 21), build từ `src/backend/Dockerfile` |
| frontend | p01-frontend | React build bằng Vite, Nginx phục vụ file tĩnh |
| nginx | p01-gateway | Cổng duy nhất ra ngoài (443, TLS tự ký), giới hạn tốc độ, header bảo mật |

Mạng (SDD §8): `p01-public` (nginx) · `p01-front` (nginx, frontend) · `p01-back` (nginx, backend) · `p01-db` (backend, postgres); ba mạng sau là `internal`. Chỉ cổng 443 mở ra ngoài; 5432 và 8080 không mở.

## Cấu trúc repo cần có
```
docker-compose.yml  .env.example  .gitattributes
database/   schema.sql  init_data.sql  init_demo_users.sql
nginx/      nginx.conf  ssl/ (server.crt, server.key: tạo bằng script, không commit)
scripts/    gen-self-signed-cert.sh  lab-check.sh
src/backend/ (pom.xml, Dockerfile, src/)     src/frontend/ (package.json, Dockerfile, nginx.conf, src/)
```

## Chạy trên Ubuntu
Yêu cầu: Docker Engine + Docker Compose plugin, `git`, `openssl`, VM có Internet (build lần đầu tải thư viện Maven và npm, mất vài phút), RAM từ 4 GB.
```bash
git clone https://github.com/Tam3332/P01-IT-Helpdesk.git && cd P01-IT-Helpdesk
git checkout <nhánh-triển-khai>
cp .env.example .env
nano .env                                   # đặt POSTGRES_PASSWORD; JWT_SECRET lấy từ: openssl rand -hex 32
sh scripts/gen-self-signed-cert.sh 192.168.x.x   # IP hoặc tên của VM (để trình duyệt khớp địa chỉ)
docker compose up -d --build
docker compose ps                            # postgres, backend, frontend phải (healthy); nginx là Up
sh scripts/lab-check.sh                      # kiểm tra tự động, chụp màn hình làm bằng chứng
```
Mở `https://<IP-VM>/` (trình duyệt cảnh báo chứng chỉ tự ký, bấm tiếp tục).

Tài khoản demo (mật khẩu `Demo@12345`, đăng nhập bằng **email**): `employee@`, `staff@`, `manager@`, `admin@` + `helpdesk.local`. Chỉ dùng ở lab; chạy thật thì bỏ dòng `init_demo_users.sql` khỏi compose.

## Lệnh hay dùng
```bash
docker compose logs -f backend               # xem log backend
docker compose logs --tail=50 nginx
docker compose up -d --build backend         # build lại một dịch vụ
docker compose down                          # dừng (giữ dữ liệu)
docker compose down -v                       # dừng và XÓA dữ liệu (cần khi đổi schema.sql)
```

## Lỗi thường gặp
| Hiện tượng | Nguyên nhân / cách xử lý |
|---|---|
| `POSTGRES_PASSWORD` hoặc `JWT_SECRET` báo bắt buộc | Chưa tạo `.env` hoặc còn trống giá trị |
| nginx thoát ngay, log nói không đọc được `server.crt` | Chưa chạy `gen-self-signed-cert.sh` |
| backend `unhealthy` | `docker compose logs backend`: thường do sai mật khẩu DB hoặc schema không khớp (đổi schema thì `down -v`) |
| Đăng nhập 401 | Dùng email (không dùng tên tài khoản); mật khẩu `Demo@12345` |
| `\r: command not found` khi chạy script | File sửa trên Windows bị CRLF: `sed -i 's/\r$//' scripts/*.sh` |
| Cổng 443 bận | Đặt `HTTPS_PORT=8443` trong `.env` rồi mở `https://<IP>:8443/` |

## Thử Frontend + DB khi chưa có backend
```bash
cp docker-compose.override.yml.example docker-compose.override.yml   # không commit
docker compose up -d --build postgres frontend
```
Mở http://localhost:8081 (dòng `ports` này chỉ để thử, không có trong cấu hình thật).

## Bản triển khai tuần 4 (T4-14)

| Mục | Giá trị |
|---|---|
| Ngày triển khai | 12/11/2026 |
| Mã nguồn | tag `v0.4-week4` = commit `a378fdb` (Merge pull request #18, trùng `main` lúc triển khai) |
| VM lab | Ubuntu 26.04.1 LTS, Docker 29.1.3, Docker Compose 2.40.3 (VirtualBox của Nhi) |
| Cổng và địa chỉ | HTTPS cổng `8443` (`HTTPS_PORT=8443` trong `.env`); VM Server `192.168.50.10` (Internal Network `helpdesk-lab`) |
| Cách truy cập của nhóm | Tailscale trên VM Server, URL `https://100.124.80.30:8443/`; Nhi mời từng thành viên vào mạng Tailscale. Chứng chỉ tự ký nên trình duyệt cảnh báo. VM cần đang bật |
| Dựng từ đầu | `docker compose down -v` rồi `docker compose up -d --build` trong thư mục clone riêng (`~/P01-lab-v04`) để nạp schema có REOPEN_PENDING; dữ liệu của project compose trước đó bị xóa |
| Image thực tế | `postgres:18-alpine`, `nginx:1.27-alpine`, `p01-backend`, `p01-frontend` (build từ tag) |

### Kết quả kiểm tra
| Kiểm tra | Kết quả | Evidence |
|---|---|---|
| `scripts/lab-check.sh 8443` | Mục 1, 3, 4, 5, 6, 7 đạt. Mục 2 báo lỗi do container `helpdesk-web` của VM (bind `127.0.0.1:8080`, không thuộc dự án); cổng mở ra ngoài của dự án chỉ có 8443 | `evidence/T4-14/lab-check_8443.txt`, `README.md` |
| Volume `uploads_data`, `audit_logs`, `pgdata` giữ dữ liệu sau `restart` và `down`/`up` (không `-v`) | Đạt | `evidence/T4-14/volume-persist.txt` |
| Upload 10 MB qua Nginx (`client_max_body_size 11m`) | HTTP 201 | `evidence/T4-14/upload-limits.txt` |
| Tệp 11 MB | HTTP 413, Nginx chặn | `evidence/T4-14/upload-limits.txt` |
| Giao diện web trên lab | Đăng nhập `employee2@` thành công qua `https://100.124.80.30:8443/` | `evidence/T4-14/ui-employee2-dashboard.png` |

### Tài khoản demo trên lab
`employee@`, `employee2@`, `staff@`, `manager@`, `admin@` + `helpdesk.local` (mật khẩu demo xem phần "Chạy trên Ubuntu" phía trên, không ghi lại ở đây). `employee2@` (id 6, `Lê Thị Cúc`) được thêm bằng SQL sau khi dựng, nên sẽ mất khi chạy `down -v`. Muốn có sẵn lâu dài thì thêm dòng tương ứng vào `database/init_demo_users.sql` (bản có `employee2` hiện chưa được đưa lên GitHub). Lab còn một ticket thử `HD-2026-0001` của `employee@` kèm tệp `test_10mb.txt`.

### Phạm vi và hạn chế
- Chứng chỉ TLS tự ký, trình duyệt sẽ cảnh báo.
- Giao diện web mới kiểm bằng trình duyệt với `employee2@` (Nhân viên), có ảnh trong `evidence/T4-14/ui-employee2-dashboard.png`; các vai trò khác mới kiểm đăng nhập bằng script. Rate limit 429 do T4-11 kiểm riêng.
- Trên VM còn các container thử nghiệm cũ của lab (`helpdesk-web`, `helpdesk-https` đã dừng, v.v.), không thuộc dự án và không bị thay đổi, trừ việc `helpdesk-https` đã được dừng và đặt `--restart=no` để nhường cổng 8443.
- VM chạy trên máy cá nhân nên có thể tắt hoặc ngủ; khi đó lab không truy cập được qua Tailscale.
