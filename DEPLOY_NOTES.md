# Triển khai P01 bằng Docker Compose

## Cấu trúc thư mục trong repo
```
docker-compose.yml
.env.example            (sao chép thành .env rồi điền giá trị)
database/               schema.sql, init_data.sql, init_demo_users.sql
nginx/                  nginx.conf, ssl/ (server.crt, server.key: tạo bằng script, không commit)
scripts/                gen-self-signed-cert.sh
src/backend/            pom.xml, src/, Dockerfile
src/frontend/           package.json, src/, Dockerfile, nginx.conf, .dockerignore
```

## Chạy trên Ubuntu Server (lab)
```bash
cp .env.example .env                  # sửa POSTGRES_PASSWORD và JWT_SECRET (openssl rand -hex 32)
sh scripts/gen-self-signed-cert.sh    # tạo chứng chỉ TLS tự ký
docker compose up -d --build
docker compose ps                     # các dịch vụ phải ở trạng thái running/healthy
```
Mở https://<địa-chỉ-máy-lab>/ (trình duyệt sẽ cảnh báo chứng chỉ tự ký, bấm tiếp tục).
Chỉ cổng 443 được mở ra ngoài; PostgreSQL (5432) và Backend (8080) chỉ truy cập được trong mạng Docker.

Tài khoản demo (mật khẩu `Demo@12345`): employee@, staff@, manager@, admin@ + `helpdesk.local`.
Chỉ dùng ở lab; chạy thật thì bỏ file `init_demo_users.sql` khỏi compose.

## Lưu ý
- Script trong `database/` chỉ chạy lần đầu khi volume rỗng. Đổi schema thì chạy `docker compose down -v` (xóa dữ liệu) rồi `up` lại.
- Frontend mặc định VITE_USE_MOCK=true (dữ liệu giả). Khi Backend sẵn sàng đặt false và build lại: `docker compose up -d --build frontend`.
- Dừng hẳn: `docker compose down`.

## Mạng Docker (khớp SDD §8)
| Mạng | Thành viên | Ghi chú |
|---|---|---|
| p01-public | nginx | duy nhất có cổng ra ngoài (443) |
| p01-front | nginx, frontend | internal |
| p01-back | nginx, backend | internal |
| p01-db | backend, postgres | internal |

## Thử Frontend + DB khi Backend chưa có code
```bash
cp docker-compose.override.yml.example docker-compose.override.yml   # không commit
docker compose up -d --build postgres frontend
```
Mở http://localhost:8081. Dòng `ports` này chỉ dùng để thử, không có trong cấu hình thật.
