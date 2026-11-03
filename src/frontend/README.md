# IT Helpdesk (SecDesk) — Frontend

ReactJS + Vite. Giao diện giống hệt prototype (CSS thuần, không dùng thư viện UI). **Không có dữ liệu giả, không đăng nhập demo**: mọi dữ liệu lấy từ Backend qua `/api/v1`.

## Chạy
Cần Node.js ≥ 20 và Backend Spring Boot đang chạy (mặc định `http://localhost:8080`).
```bash
npm install
cp .env.example .env.local     # Windows: copy .env.example .env.local
npm run dev                    # http://localhost:5173  (Vite proxy /api -> VITE_PROXY_TARGET)
```
Lệnh khác: `npm run build`, `npm run lint`, `npm run format`.

## Màn hình đã làm (4 vai trò)
| Vai trò | Màn hình |
|---|---|
| Chung | Đăng nhập, đăng xuất, khung layout, route guard (403/404), chi tiết ticket (bình luận, tệp đính kèm, lịch sử, CSAT) |
| Employee | Tổng quan, ticket của tôi (lọc, tìm, phân trang), tạo ticket + đính kèm, đóng ticket, yêu cầu mở lại |
| IT Staff | Tổng quan, ticket được giao, bắt đầu xử lý, hoàn tất (giải pháp ≥ 20 ký tự), ghi chú nội bộ, giám sát SLA |
| IT Manager | Tổng quan, quản lý ticket, phân công/phân công lại, giám sát SLA, duyệt/từ chối yêu cầu mở lại |
| Admin | Tổng quan, người dùng (tạo, gán role, khoá/kích hoạt), danh mục, cấu hình SLA, mức ưu tiên và trạng thái (chỉ xem), nhật ký |

Các nút chỉ hiện theo vai trò và trạng thái để dễ dùng. Quyền thật (RBAC, chống IDOR, luồng trạng thái) do Backend kiểm tra và trả 401/403/404/422.

## API mà FE gọi (base `/api/v1`)
Bám theo `openapi.yaml` của nhánh `tri/update-schema-openapi`. Bóc dữ liệu chấp nhận cả `{success,data}` và `{code,message,data}`; lỗi đọc từ `error.message` hoặc `message`; phân trang đọc được cả `page/size` lẫn `pageNo/pageSize`.

| Nhóm | Endpoint |
|---|---|
| Auth | `POST /auth/login` `{email,password}` → `{accessToken,...}`, `GET /auth/me`, `POST /auth/logout` |
| Ticket | `GET /tickets` (page, size, status, priority, categoryId, search), `GET /tickets/{ticketNumber}`, `POST /tickets` |
| Workflow | `PUT /tickets/{ref}/assign`, `PATCH /tickets/{ref}/status`, `POST /tickets/{ref}/close`, `POST /tickets/{ref}/reopen` |
| Chi tiết | `GET/POST /tickets/{ref}/comments`, `GET/POST /tickets/{ref}/attachments`, `GET /tickets/{ref}/attachments/{id}`, `GET /tickets/{ref}/history`, `GET/POST /tickets/{ref}/feedback` |
| Mở lại | `GET /reopen-requests?status=`, `POST /reopen-requests/{id}/approve`, `POST /reopen-requests/{id}/reject` |
| Tham chiếu | `GET /categories`, `GET /users/it-staff` |
| Admin | `GET/POST /admin/users`, `PATCH /admin/users/{id}/role`, `PATCH /admin/users/{id}/status`, `GET/POST /admin/categories`, `GET /admin/sla-configs`, `PUT /admin/sla-configs/{priority}` |

Mã ticket dùng để gọi API nằm ở `TICKET_REF_FIELD` trong `src/config/domain.js` (hiện là `ticketNumber`, ví dụ `HD-2026-0001`). Nếu backend đổi sang `id` chỉ cần sửa một dòng.

## Điểm cần thống nhất với Backend
1. **Đăng nhập**: nhánh `tri/be` chưa có `/auth/login`, `SecurityConfig` đang `permitAll` và dùng user `employee` mặc định (chờ JWT của Cường). `user` trong `LoginResponse`/`/auth/me` cần có `id, email, fullName, role`.
2. **`GET /admin/audit-logs`** chưa có trong openapi. Màn Nhật ký hiện báo "API chưa sẵn sàng". Gợi ý trả trang gồm `actorName, action, target, result, createdAt`.
3. **Tổng quan/giám sát SLA**: tạm tính phía FE từ `GET /tickets` (đếm theo trạng thái, tính % thời hạn từ `createdAt` và `resolutionDeadline`). Khi có API dashboard (HD14) nên thay.
4. **Danh sách ticket**: cần có `assigneeName` trong `TicketResponse` (cột "Người xử lý") và hỗ trợ tham số `search`. Staff chỉ nên thấy vé được giao.
5. **Trạng thái**: FE dùng `NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED, REOPENED, REOPEN_PENDING`. Mở lại: nhân viên gửi yêu cầu khi `RESOLVED` → ticket sang `REOPEN_PENDING` → Manager duyệt/từ chối.
6. Mức ưu tiên và trạng thái là tập cố định (ràng buộc CHECK trong DB) nên màn hình chỉ xem.

## Phiên đăng nhập
Token lưu `sessionStorage` (đóng tab là mất), tự đăng xuất khi hết hạn theo claim `exp` của JWT. 401 ở bất kỳ API nào → về `/login`; 429 khi đăng nhập → thông báo khoá 15 phút.

## Cấu trúc `src/`
| Thư mục | Vai trò |
|---|---|
| `config/` | vai trò, menu theo vai trò (`nav.js`), trạng thái/ưu tiên/giới hạn (`domain.js`), quyền hiển thị (`permissions.js`) |
| `context/` | `AuthContext` (phiên, login/logout, tự hết hạn), `ToastContext` |
| `routes/` | `AppRoutes`, `ProtectedRoute` (`RequireAuth`, `RequireRole`) |
| `layouts/` | `MainLayout` (sidebar, topbar, drawer < 900px) |
| `services/` | `apiClient` (axios + JWT + 401/403), `authService`, `ticketService`, `referenceService` |
| `hooks/`, `utils/` | `useAsync`, `useDebounced`, tính SLA, định dạng ngày |
| `components/` | Badges, Modal, Pagination, TicketTable, TicketListCard, Kpi... |
| `pages/` | `auth`, `employee`, `staff`, `manager`, `admin`, `tickets`, `common` |
| `styles/app.css` | CSS lấy nguyên từ prototype |
