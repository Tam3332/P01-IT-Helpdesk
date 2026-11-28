# Triển khai P01 bằng Docker Compose (VM Ubuntu lab)

## Bàn giao WAF T6 15 ngày 26 tháng 11 năm 2026

Phần WAF đã có trên main qua PR #38; follow-up chuẩn bị cho Nhi review và Chính deploy theo [hướng dẫn riêng](evidence/T6-15/DEPLOY_CHINH.md). Dùng đúng SHA đã review trên project/volumes hiện hành, DetectionOnly trước rồi On sau nghiệm thu. Gateway image WAF nghe 8443 trong container; host port vẫn theo HTTPS_PORT. TLS cần bản sao đọc được bởi user image, không dùng key root 0600 trực tiếp hoặc sửa key gốc. Chỉ recreate nginx bằng `up -d --no-deps --no-build --pull never nginx`; giữ các container ứng dụng. Các phần tuần4/5 bên dưới là lịch sử triển khai cũ. Với lượt WAF không chạy `down -v` hoặc thay DB để xử lý lỗi gateway. Chưa có kết quả deploy VM nhóm cho follow-up này.

## Thành phần
| Dịch vụ | Container | Ghi chú |
|---|---|---|
| postgres | p01-db | PostgreSQL 18 theo Compose hiện hành; nạp `database/schema.sql`, `init_data.sql`, `init_demo_users.sql` lần đầu |
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
# Các lệnh nhập mật khẩu dưới đây chạy trong Bash, không dùng sh/dash
read -r -s -p 'Mật khẩu lab: ' DEMO_PASSWORD; printf '\n'
export DEMO_PASSWORD
sh scripts/lab-check.sh                      # kiểm tra tự động, không in mật khẩu/token
unset DEMO_PASSWORD
                                             # mục 2 chỉ xét cổng của project compose này, không tính container khác trên VM
```
Mở `https://<IP-VM>/` (trình duyệt cảnh báo chứng chỉ tự ký, bấm tiếp tục).

Tài khoản demo (đăng nhập bằng **email**; credential cố định còn xuất hiện trong mã nguồn và evidence lịch sử, theo BUG-HD-012; chưa coi secrets đã đạt): `employee@`, `staff@`, `manager@`, `admin@` + `helpdesk.local`. Chỉ dùng ở lab; chạy thật thì bỏ dòng `init_demo_users.sql` khỏi compose.

## Lệnh hay dùng
```bash
docker compose logs -f backend               # xem log backend
docker compose logs --tail=50 nginx
docker compose up -d --build backend         # build lại một dịch vụ
docker compose down                          # dừng (giữ dữ liệu)
# Không dùng down -v cho stack có dữ liệu; sao lưu và chạy migration đã review.
```

## Lỗi thường gặp
| Hiện tượng | Nguyên nhân / cách xử lý |
|---|---|
| `POSTGRES_PASSWORD` hoặc `JWT_SECRET` báo bắt buộc | Chưa tạo `.env` hoặc còn trống giá trị |
| nginx thoát ngay, log nói không đọc được `server.crt` | Chưa chạy `gen-self-signed-cert.sh` |
| backend `unhealthy` | `docker compose logs backend`: thường do sai mật khẩu DB hoặc schema không khớp; sao lưu, đối chiếu migration, không xóa volume để sửa schema |
| Đăng nhập 401 | Dùng email (không dùng tên tài khoản); mật khẩu do nhóm cung cấp qua kênh riêng |
| `\r: command not found` khi chạy script | File sửa trên Windows bị CRLF: `sed -i 's/\r$//' scripts/*.sh` |
| Cổng 443 bận | Đặt `HTTPS_PORT=8443` trong `.env` rồi mở `https://<IP>:8443/` |

## Thử Frontend + DB khi chưa có backend
```bash
cp docker-compose.override.yml.example docker-compose.override.yml   # không commit
docker compose up -d --build postgres frontend
```
Mở http://localhost:8081 (dòng `ports` này chỉ để thử, không có trong cấu hình thật).

## Bản triển khai v0.6-final (T6-07)

| Mục | Giá trị |
|---|---|
| Ngày triển khai | 28/11/2026 |
| Mã nguồn ứng dụng | tag `v0.6-final` = commit `701c00c9d5b6103fb014b17dc4441c92ccf4e454` |
| VM lab | `Helpdesk-Security-Lab`; thư mục `~/P01-lab-v06`, project compose `p01-lab-v06`; HTTPS cổng `8443` |
| Cách cập nhật | Giữ project và volume (không `down -v`). Chuyển thư mục triển khai sang đúng commit của tag, build lại `backend` và `frontend`, tạo lại hai container đó; sau đó chỉ thay container gateway |
| Gateway | `owasp/modsecurity-crs@sha256:c95914e1855c41791e6745c2a63864a218f99e04948b64d8b31cb7b29b1ba5b3`, chế độ `DetectionOnly`, ngưỡng PL1 inbound 5 / outbound 4. Gateway nghe `8443` trong container và được publish `8443:8443` |
| TLS cho WAF | Bản sao riêng ngoài repo (thư mục `0700`, key `0400`, crt `0444`, chủ UID/GID 101); key gốc giữ nguyên quyền để rollback |
| Quay lui | Snapshot gateway trước thay đổi lưu riêng trên VM (ngoài repo) |

### Kết quả
| Kiểm tra | Kết quả |
|---|---|
| Build backend và frontend | Thành công |
| Container | `p01-db`, `p01-backend`, `p01-frontend` healthy; `p01-gateway` chạy image WAF, publish `8443`; container giám sát tiếp tục chạy |
| `nginx -t` | Exit 0 (đã nạp 857 luật CRS) |
| `preflight check` (đúng SHA, cây sạch, cấu hình hợp lệ) | PASS, cả trước và sau khi thay gateway |
| `scripts/waf/smoke.py` (DetectionOnly) | 11/11 kiểm tra PASS |
| `scripts/lab-check.sh 8443` | Exit 0; 4 vai trò đăng nhập được; Swagger 404; API không token 401 |
| Header qua gateway | `Strict-Transport-Security` đúng 1 dòng ở phản hồi 200, 401, 404; có `Content-Security-Policy` |
| Backend nội bộ | `/actuator/health` trong container backend trả `{"status":"UP"}` |

### Dữ liệu và tài khoản
- Dọn dữ liệu kiểm thử của Khánh (ticket 1006 đến 1009 và tài khoản kiểm thử đã khóa); giữ ticket khác và tài khoản `audit-test`.
- Đặt lại mật khẩu 7 tài khoản demo trên DB lab hiện có (6 tài khoản seed dùng hash trong `database/init_demo_users.sql`, và `audit-test`); phiên cũ bị thu hồi, mật khẩu cũ bị từ chối (401). Mật khẩu mới do nhóm giữ riêng, không ghi trong repo.

### Sự cố gặp khi triển khai
- `nginx -t` lần đầu thất bại (exit 2): file `nginx/nginx-waf.conf` trong thư mục triển khai có quyền `0640`, user 101 của image WAF không đọc được. Xử lý: `chmod 0644` file cấu hình (không đổi nội dung, Git không theo dõi quyền đọc).
- `.git/index` bị chuyển chủ sở hữu sang root sau khi chạy `preflight` bằng root. Xử lý: trả quyền sở hữu `.git` về chủ thư mục triển khai.

### Phạm vi và hạn chế
- WAF chạy `DetectionOnly`; chưa chuyển sang `On`.
- BUG-HD-009 (CSP còn `style-src 'unsafe-inline'`) là rủi ro còn lại đã được duyệt riêng cho lab v0.6-final.
- Container giám sát chạy từ file compose khác (compose báo là orphan): không dùng `--remove-orphans`.

## Bản triển khai tuần 6 (T6-07)

| Mục | Giá trị |
|---|---|
| Ngày triển khai | 26/11/2026 |
| Mã nguồn ứng dụng | tag `v0.6-rc1` = commit `ed229a3` |
| VM lab | `Helpdesk-Security-Lab`, Ubuntu 26.04.1 LTS |
| Cổng và địa chỉ | HTTPS cổng `8443` (`0.0.0.0:8443->443/tcp`). Truy cập qua Tailscale `https://<IP-VM>:8443/login`; IP thật của VM không ghi trong repo. Thành viên cần được mời vào cùng mạng Tailscale; VM cần đang bật |
| Dựng từ đầu | Thư mục `~/P01-lab-v06`, project compose `p01-lab-v06`. `.env` và chứng chỉ TLS được chép từ `~/P01-lab-v05`; không thay đổi dữ liệu/volume của project v0.5 |
| Image thực tế | `postgres:18-alpine`, `nginx:1.27-alpine`, `p01-backend`, `p01-frontend` (build từ tag `v0.6-rc1`) |

### Kết quả triển khai
| Kiểm tra | Kết quả | Evidence |
|---|---|---|
| Build image | `backend` và `frontend` build thành công; quá trình build khoảng 6 phút | log `build_v06rc1.log` trên VM |
| 4 container | `p01-db`, `p01-backend`, `p01-frontend` healthy; `p01-gateway` Up, publish cổng `8443` | `docker compose ps -a` trên VM |
| Gateway HTTPS | `curl -k -I https://localhost:8443/` trả về `HTTP/1.1 200 OK` | `evidence/T6-07/README.md` |
| Giao diện đăng nhập | Trang `/login` mở được qua địa chỉ Tailscale | `evidence/T6-07/README.md` |

Chạy lệnh Docker Compose bằng `sudo` vì tài khoản `nhi` không thuộc nhóm `docker`.

### Cách sử dụng lab chung
- Thành viên kết nối Tailscale và truy cập `https://<IP-VM>:8443/login`; liên hệ Nhi nếu chưa được cấp quyền vào tailnet.
- VM là môi trường dùng chung. Không tự ý `git pull`, checkout phiên bản khác hoặc chạy `docker compose up -d --build` trên VM.
- Điều khiển dịch vụ bằng một trong các lệnh: `sudo docker compose start`, `sudo docker compose stop` hoặc `sudo docker compose restart`; xem log bằng `sudo docker compose logs --tail=100`.
- Không chạy `docker compose down -v` vì lệnh này xóa volume dữ liệu.
- Trình duyệt có thể cảnh báo chứng chỉ; phần xử lý/cấu hình TLS sẽ do IA phối hợp thực hiện ở bước riêng.

### Phạm vi và hạn chế
- Mốc này xác nhận việc build, khởi động container và khả năng truy cập giao diện để nhóm tiếp tục làm việc; chưa phải kết quả nghiệm thu P3 hoặc kiểm thử TLS.
- Chứng chỉ đang dùng được kế thừa từ v0.5; chưa xác nhận tính hợp lệ/trust của chứng chỉ trên trình duyệt.
- VM chạy trên máy cá nhân nên có thể tắt hoặc ngủ; khi đó lab không truy cập được qua Tailscale.

## Bản triển khai tuần 5 (T5-07)

| Mục | Giá trị |
|---|---|
| Ngày triển khai | 20/11/2026 |
| Mã nguồn ứng dụng | tag `v0.5-mvp` = commit `35c573d` (nhánh `frontend`: toàn bộ `main` @`671d43a` cộng thay đổi mã evidence, không đổi code ứng dụng). Bỏ qua mốc `v0.5-rc1`, triển khai thẳng bản MVP |
| Script kiểm tra | `scripts/lab-check.sh` ở commit `a44208a` (sửa mục 2 và mục 7), chép riêng một tệp lên VM khi chạy; phần còn lại của thư mục đúng tag `v0.5-mvp` |
| VM lab | Ubuntu 26.04.1 LTS, Docker 29.1.3, Docker Compose 2.40.3 (VirtualBox của Nhi, như bản v0.4) |
| Cổng và địa chỉ | HTTPS cổng `8443`. Truy cập qua Tailscale `https://<IP-VM>:8443/`; trong mạng nội bộ của VM là `https://<IP-LAN-VM>:8443/`. IP thật của VM do Nhi giữ, không ghi trong repo (`<IP-VM>` là địa chỉ Tailscale, `<IP-LAN-VM>` là địa chỉ mạng nội bộ). Cả nhóm truy cập qua Tailscale (Nhi mời từng thành viên); VM cần đang bật |
| Dựng từ đầu | Thư mục `~/P01-lab-v05`, project compose `p01-lab-v05` (volume mới, cơ sở dữ liệu rỗng). `.env` và chứng chỉ TLS chép từ `~/P01-lab-v04`. Bản v0.4 dừng bằng `docker compose down` (**không** `-v`), 5 volume `p01-lab-v04_*` còn nguyên |
| Image thực tế | `postgres:18-alpine`, `nginx:1.27-alpine`, `p01-backend`, `p01-frontend` (build từ tag). Lưu ý: hai image `p01-*` dùng tên cố định, nên muốn chạy lại v0.4 phải `up -d --build` trong thư mục v0.4 |

### Kết quả kiểm tra
| Kiểm tra | Kết quả | Evidence |
|---|---|---|
| `scripts/lab-check.sh 8443` | Mục 1 đến 7 đều đạt, kết thúc `KẾT QUẢ: đạt`. Mục 2 không còn báo nhầm | `evidence/T5-07/lab-check_v05_8443.txt` |
| 4 container | `p01-db`, `p01-backend`, `p01-frontend` healthy; `p01-gateway` Up, chỉ mở `0.0.0.0:8443` | cùng tệp, mục 1 và 2 |
| Giao diện 4 vai trò trên lab | Employee, IT Staff, IT Manager, Admin: 11 màn hình (Tổng quan, SLA, ticket, yêu cầu mở lại, người dùng, cài đặt hệ thống, nhật ký) | `evidence/T5-07/ui/` |
| Dữ liệu thử | 3 ticket (`HD-2026-0001` đến `0003`), 1 yêu cầu mở lại của `HD-2026-0002` đang chờ duyệt; nhật ký hệ thống có 38 bản ghi | ảnh `manager_yeu_cau_mo_lai.png`, `admin_nhat_ky.png` |

Chạy script bằng `sudo` vì tài khoản `nhi` không thuộc nhóm `docker`.

### Hạn chế đã biết (sẽ sửa ở commit riêng)
- Chế độ English còn vài chuỗi tiếng Việt: trang Giám sát SLA của Manager (tiêu đề, dòng phụ, nhãn "Bình thường"), mô tả trong Cài đặt hệ thống của Admin, các nhãn hành động trong Nhật ký hệ thống.
- Dòng phụ trang Yêu cầu mở lại ghi "ticket đã đóng", trong khi quy tắc BR-HD-04 chỉ cho mở lại ticket ở trạng thái Resolved.
- Trang Tổng quan của Employee hiện "—" ở ô "Đã giải quyết" và "Đã đóng" khi chưa có ticket; cần xác nhận với Trí API thống kê có trả hai trường này không.
- Container giám sát của v0.4 (`p01-lab-v04-monitoring-1`) vẫn chạy và đọc volume v0.4, chưa được nối sang v0.5.
- Chứng chỉ TLS tự ký; VM chạy trên máy cá nhân nên có thể tắt hoặc ngủ.

## Bản triển khai tuần 4 (T4-14)

| Mục | Giá trị |
|---|---|
| Ngày triển khai | 12/11/2026 |
| Mã nguồn | tag `v0.4-week4` = commit `a378fdb` (Merge pull request #18, trùng `main` lúc triển khai) |
| VM lab | Ubuntu 26.04.1 LTS, Docker 29.1.3, Docker Compose 2.40.3 (VirtualBox của Nhi) |
| Cổng và địa chỉ | HTTPS cổng `8443` (`HTTPS_PORT=8443` trong `.env`); VM Server `<IP-LAN-VM>` (Internal Network `helpdesk-lab`) |
| Cách truy cập của nhóm | Tailscale trên VM Server, URL `https://<IP-VM>:8443/`; Nhi mời từng thành viên vào mạng Tailscale. Chứng chỉ tự ký nên trình duyệt cảnh báo. VM cần đang bật |
| Dựng từ đầu | `docker compose down -v` rồi `docker compose up -d --build` trong thư mục clone riêng (`~/P01-lab-v04`) để nạp schema có REOPEN_PENDING; dữ liệu của project compose trước đó bị xóa |
| Image thực tế | `postgres:18-alpine`, `nginx:1.27-alpine`, `p01-backend`, `p01-frontend` (build từ tag) |

### Kết quả kiểm tra
| Kiểm tra | Kết quả | Evidence |
|---|---|---|
| `scripts/lab-check.sh 8443` | Mục 1, 3, 4, 5, 6, 7 đạt. Mục 2 báo lỗi do container `helpdesk-web` của VM (bind `127.0.0.1:8080`, không thuộc dự án); cổng mở ra ngoài của dự án chỉ có 8443 | `evidence/T4-14/lab-check_8443.txt`, `README.md` |
| Volume `uploads_data`, `audit_logs`, `pgdata` giữ dữ liệu sau `restart` và `down`/`up` (không `-v`) | Đạt | `evidence/T4-14/volume-persist.txt` |
| Upload 10 MB qua Nginx (`client_max_body_size 11m`) | HTTP 201 | `evidence/T4-14/upload-limits.txt` |
| Tệp 11 MB | HTTP 413, Nginx chặn | `evidence/T4-14/upload-limits.txt` |
| Giao diện web trên lab | Đăng nhập `employee2@` thành công qua `https://<IP-VM>:8443/` | `evidence/T4-14/ui-employee2-dashboard.png` |

### Tài khoản demo trên lab
`employee@`, `employee2@`, `staff@`, `manager@`, `admin@` + `helpdesk.local` (mật khẩu demo xem phần "Chạy trên Ubuntu" phía trên, không ghi lại ở đây). `employee2@` (id 6, `Lê Thị Cúc`) được thêm bằng SQL sau khi dựng, nên sẽ mất khi chạy `down -v`. Muốn có sẵn lâu dài thì thêm dòng tương ứng vào `database/init_demo_users.sql` (bản có `employee2` hiện chưa được đưa lên GitHub). Lab còn một ticket thử `HD-2026-0001` của `employee@` kèm tệp `test_10mb.txt`.

### Phạm vi và hạn chế
- Chứng chỉ TLS tự ký, trình duyệt sẽ cảnh báo.
- Giao diện web mới kiểm bằng trình duyệt với `employee2@` (Nhân viên), có ảnh trong `evidence/T4-14/ui-employee2-dashboard.png`; các vai trò khác mới kiểm đăng nhập bằng script. Rate limit 429 do T4-11 kiểm riêng.
- Trên VM còn các container thử nghiệm cũ của lab (`helpdesk-web`, `helpdesk-https` đã dừng, v.v.), không thuộc dự án và không bị thay đổi, trừ việc `helpdesk-https` đã được dừng và đặt `--restart=no` để nhường cổng 8443.
- VM chạy trên máy cá nhân nên có thể tắt hoặc ngủ; khi đó lab không truy cập được qua Tailscale.
