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
