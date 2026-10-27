# IT Helpdesk — Frontend (P01)

ReactJS + Vite + Ant Design. Chạy được ngay với **dữ liệu giả** (không cần backend).

## Chạy trên VS Code
1. Cài [Node.js](https://nodejs.org) ≥ 20, mở thư mục này bằng VS Code (`File > Open Folder`).
2. VS Code sẽ gợi ý cài extension (ESLint, Prettier...) → bấm **Install**.
3. Mở Terminal (`Ctrl+``) rồi chạy:
```bash
npm install
npm run dev        # http://localhost:5173
```
Tài khoản demo (mật khẩu `123456`): `employee`, `staff`, `manager`, `admin`, `locked` (bị khóa).

Lệnh khác: `npm run build`, `npm run lint`, `npm run format`.

## Nối backend Spring Boot
Sửa `.env.local`: `VITE_USE_MOCK=false` và `VITE_PROXY_TARGET=http://localhost:8080`. Vite proxy `/api` → backend.
API giả định (chỉnh lại theo tài liệu API của nhóm, chỉ cần sửa `src/services/`):
`POST /api/auth/login`, `GET /api/tickets`, `GET /api/tickets/{id}`, `POST /api/tickets`, `GET /api/reopen-requests`, `GET /api/audit-logs`, `GET /api/users`.

## Cấu trúc `src/`
| Thư mục | Vai trò |
|---|---|
| `config/` | hằng số (role, trạng thái, SLA, quy tắc file), menu theo role |
| `context/` | `AuthContext` (token + user trong localStorage) |
| `routes/` | `AppRoutes`, `ProtectedRoute` (chưa login → /login, sai role → /403) |
| `services/` | `apiClient` (axios + JWT + xử lý 401/403), service mock/thật |
| `mocks/` | dữ liệu giả |
| `layouts/` | `MainLayout` (sidebar theo role, responsive) |
| `components/` | Tags, PageHeader, PagePlaceholder, TicketTable |
| `pages/` | màn hình theo role (`auth`, `employee`, `staff`, `manager`, `admin`, `tickets`) |

## Route
| Role | Đường dẫn |
|---|---|
| Chung | `/login`, `/403`, `/tickets/:id` |
| Employee | `/employee`, `/employee/tickets`, `/employee/tickets/new` |
| IT Staff | `/staff`, `/staff/tickets`, `/staff/sla` |
| IT Manager | `/manager`, `/manager/tickets`, `/manager/sla`, `/manager/reopen-requests` |
| Admin | `/admin`, `/admin/users`, `/admin/settings`, `/admin/audit-logs` |

## Trạng thái
Đã làm: đăng nhập, route guard, layout/menu theo role, danh sách ticket, form tạo ticket (validate file), chi tiết ticket, audit log chỉ đọc.
Còn `PagePlaceholder`/TODO: dashboard các role, nút hành động trên chi tiết ticket (Assign/Resolve/Close/Reopen), duyệt mở lại, SLA monitor, form cấu hình.

> Chặn route ở FE chỉ để UX; phân quyền thật (RBAC, khóa tài khoản, audit log) do backend thực thi.
