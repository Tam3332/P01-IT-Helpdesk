# Kiểm thử API P01 (T6-06)

Hai bộ công cụ, đều chạy được trên máy dev và VM lab:

| Thư mục | Dùng để | Yêu cầu |
|---|---|---|
| `newman/` | Kiểm thử chức năng và phân quyền qua HTTP: 65 request theo luồng P1, P3, admin, đăng xuất; mỗi request kiểm mã HTTP, `error.code` và thời gian phản hồi < 2 s | Node.js + `npm i -g newman` |
| `perf/` | Đo thời gian phản hồi với khoảng 1.000 ticket (sheet ghi NFR04; trong SRS là NFR-02 Performance): trung bình, p95, lớn nhất | `sh`, `curl`, `awk` (có sẵn trên Ubuntu) |

Không có mật khẩu nào lưu trong file. Mật khẩu demo truyền vào lúc chạy.

## 1. Newman

Collection tự tạo dữ liệu riêng (2 ticket có tiêu đề `[Newman <số>]`), không phụ thuộc ticket có sẵn. Cần 6 tài khoản demo (`database/init_demo_users.sql`). Mỗi lần chạy tạo thêm 2 ticket và 1 tệp đính kèm nhỏ.

**Máy dev** (backend chạy ở `localhost:8080`, PowerShell, đứng ở thư mục `tests\newman`):

```powershell
newman run P01_API.postman_collection.json -e P01_dev.postman_environment.json --env-var "demoPassword=<mật khẩu demo>" -r cli,junit --reporter-junit-export newman_dev.xml
```

**VM lab** (qua gateway HTTPS, đứng ở thư mục `tests/newman`; thay `<IP-VM>` hoặc dùng `localhost` khi chạy ngay trên VM):

```sh
newman run P01_API.postman_collection.json -e P01_lab.postman_environment.json \
  --env-var "baseUrl=https://<IP-VM>:8443/api/v1" --env-var "demoPassword=<mật khẩu demo>" \
  --insecure -r cli,junit --reporter-junit-export newman_lab.xml
```

- `--insecure`: lab dùng chứng chỉ tự ký.
- Môi trường `lab` đặt `loginDelayMs=7000`: mỗi lần đăng nhập chờ 7 giây vì Nginx giới hạn `/auth/login` 10 request/phút/IP (burst 5). Có 9 request đăng nhập nên phần này mất khoảng 1 phút.
- Không chạy collection 2 lần liên tiếp trong vòng 1 phút trên lab (vẫn có thể chạm giới hạn đăng nhập).
- Collection có 1 lần đăng nhập sai cho employee2, chưa tới ngưỡng khóa 5 lần.
- Muốn mở trong Postman: Import 2 file `.json`, chọn environment, điền `demoPassword` (kiểu secret) rồi Run collection. Thư mục làm việc phải là `tests/newman` để tìm thấy `files/`.

Nhóm request:

| Nhóm | Nội dung | Liên kết |
|---|---|---|
| 01 Đăng nhập và phiên | Đăng nhập 6 vai trò; sai mật khẩu 401; thiếu dữ liệu 400; không token / token giả 401 | HD01, UC-01, IA-HD01 |
| 02 Luồng P1 | Tạo → phân công (nâng CRITICAL) → IN_PROGRESS → bình luận / ghi chú nội bộ → tải tệp → RESOLVED → đóng → đánh giá; Anti-IDOR 404; vai trò sai 403; dữ liệu sai 400; ticket đóng 422 | HD03–HD09, HD15, BR-HD-01/02/04, PC-03 |
| 03 Luồng P3 | RESOLVED → yêu cầu mở lại → gửi trùng 422 → Manager duyệt → REOPENED | HD10, UC-07/08 |
| 04 Quản trị | /admin 403 với Employee; SLA: Manager chỉ xem, Admin đặt 0 giờ 400, mức không tồn tại 404; audit log; dashboard 4 vai trò | HD02, HD09, HD11, HD14 |
| 05 Đăng xuất | Đăng xuất rồi dùng lại token → 401 | HD01 |

## 2. Đo hiệu năng (chỉ trên VM lab, môi trường test)

```sh
# 1) Seed 1.000 ticket PERF-0001…PERF-1000
docker compose exec -T postgres psql -U p01_admin -d p01_db < tests/perf/seed_perf_tickets.sql
# 2) Đo (mặc định 30 lần mỗi API; upload 10 MB đo 5 lần)
DEMO_PASSWORD='<mật khẩu demo>' sh tests/perf/nfr_perf.sh https://localhost:8443/api/v1 30
# 3) Dọn dữ liệu đo
docker compose exec -T postgres psql -U p01_admin -d p01_db < tests/perf/cleanup_perf_tickets.sql
```

- Kết quả ghi ra `nfr_perf_<ngày>_<giờ>.md` ở thư mục đang đứng (bảng: trung bình, p95, lớn nhất, kết luận). Ca p95 ≥ 2.000 ms → ghi BUG vào 04 §4.
- Báo nhóm trước khi seed/đo trên VM nhóm; Chính reset seed sau khi đo nếu cần.

### Dọn dẹp

`cleanup_perf_tickets.sql` xóa ticket PERF và dữ liệu liên quan trong DB. 5 tệp 10 MB đã tải lên trong lúc đo vẫn còn trong volume `uploads_data` (tên ngẫu nhiên, không còn bản ghi tham chiếu). Xóa nếu cần dung lượng, sau khi đối chiếu với bảng `ticket_attachments`.
