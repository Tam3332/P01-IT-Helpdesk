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
