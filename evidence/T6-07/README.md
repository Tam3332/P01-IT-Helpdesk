# Evidence T6-07: lab v0.6-rc1

Ngày triển khai: 26/11/2026. Mã nguồn ứng dụng: tag `v0.6-rc1` = commit `ed229a3`.
Môi trường: VM `Helpdesk-Security-Lab`, Ubuntu 26.04.1 LTS; thư mục `~/P01-lab-v06`; compose project `p01-lab-v06`.

| Mục | Kết quả | Nguồn evidence |
|---|---|---|
| Build image | Backend và frontend build thành công; build chạy nền và hoàn tất sau khoảng 6 phút | `build_v06rc1.log` trên VM |
| Trạng thái container | `p01-db`, `p01-backend`, `p01-frontend` healthy; `p01-gateway` Up | `sudo docker compose ps -a` trên VM |
| Publish cổng | Host port `8443` được map tới gateway container port `443`; DB `5432` và backend `8080` không publish trực tiếp ra host | `sudo docker compose ps -a` trên VM |
| HTTP response qua gateway | `curl -k -I https://localhost:8443/` trả `HTTP/1.1 200 OK` | kết quả terminal ngày 26/11/2026 |
| Truy cập giao diện | Trang `/login` mở được qua Tailscale | trình duyệt trên máy thành viên triển khai |

Không đưa IP Tailscale thật, nội dung `.env`, mật khẩu hoặc private key vào repository.
`curl -k` bỏ qua bước xác minh chứng chỉ; kết quả HTTP 200 chỉ xác nhận gateway trả response, không xác nhận chứng chỉ TLS hợp lệ.
Mốc evidence này không bao gồm kiểm thử P3 hoặc nghiệm thu TLS.

## Bổ sung: lab v0.6-final (28/11/2026)

Mã nguồn ứng dụng: tag `v0.6-final` = commit `701c00c9d5b6103fb014b17dc4441c92ccf4e454`. Cùng VM, thư mục và project compose như bản rc1; giữ volume, không `down -v`.

| Mục | Kết quả | Nguồn evidence |
|---|---|---|
| Build backend, frontend | Thành công | log build lưu riêng trên VM (ngoài repo) |
| Gateway | Chạy image WAF `owasp/modsecurity-crs` (DetectionOnly), publish `8443`; `nginx -t` exit 0 | kết quả terminal 28/11/2026 |
| preflight, smoke WAF | PASS; 11/11 PASS | `preflight.py check`, `smoke.py` (lưu riêng trên VM) |
| lab-check | Exit 0; 4 vai trò đăng nhập được | kết quả terminal 28/11/2026 |
| Header | HSTS đúng 1 dòng (200, 401, 404); có CSP | kết quả terminal 28/11/2026 |
| P3 trên lab | Employee gửi yêu cầu mở lại; Manager từ chối | `evidence/T6-12/EV-HD-10_p3_05_employee_gui_yeu_cau.png`, `evidence/T6-12/EV-HD-10_p3_06_manager_tu_choi.png` |

Không đưa IP thật, mật khẩu, token hoặc nội dung `.env` vào repository. Ảnh chụp trình duyệt dùng địa chỉ `localhost` qua đường hầm SSH.

