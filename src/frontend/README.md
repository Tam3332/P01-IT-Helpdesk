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

## Tiến độ theo đợt
| Đợt | Nội dung | Trạng thái |
|---|---|---|
| 1 | Đăng nhập, đăng xuất, khung layout, route guard, menu theo vai trò | Xong |
| 2 | Employee: danh sách, tạo, chi tiết ticket, bình luận, đính kèm | Chưa |
| 3 | Workflow: giao việc, đổi trạng thái, đóng, mở lại, duyệt mở lại | Chưa |
| 4 | Admin: người dùng, danh mục, SLA, nhật ký | Chưa |
| 5 | Dashboard 4 vai trò (cần API HD14) | Chưa |

Các route chưa làm hiển thị trang "sẽ được nối API ở đợt N" (`ComingSoonPage`), không có số liệu giả.

## Hợp đồng API đợt 1 (SDD §4)
| API | Dùng cho |
|---|---|
| `POST /api/v1/auth/login` body `{email,password}` → `data` = `{accessToken, tokenType, expiresIn, user}` | Đăng nhập |
| `GET /api/v1/auth/me` → `data` = user | Khôi phục phiên khi tải lại trang |
| `POST /api/v1/auth/logout` | Đăng xuất (đưa token vào blacklist) |

- Thành công: `{success:true, data, ...}`. Lỗi: `{success:false, error:{code,message,details}}`.
- `user` cần có: `id`, `email`, `fullName`, `role` (`EMPLOYEE | IT_STAFF | IT_MANAGER | ADMIN`; chấp nhận cả `role: {roleName}`).
- 401 ở bất kỳ API nào → xoá phiên, về `/login`. 429 ở login → thông báo khoá 15 phút.
- Token lưu `sessionStorage` (đóng tab là mất), tự đăng xuất khi hết hạn theo claim `exp` của JWT.

## Cấu trúc `src/`
| Thư mục | Vai trò |
|---|---|
| `config/` | hằng số (vai trò, khoá lưu trữ), menu theo vai trò |
| `context/` | `AuthContext` (phiên, login/logout, tự hết hạn), `ToastContext` |
| `routes/` | `AppRoutes`, `ProtectedRoute` (`RequireAuth`, `RequireRole`) |
| `layouts/` | `MainLayout` (sidebar, topbar, breadcrumb, drawer < 900px) |
| `services/` | `apiClient` (axios + JWT + 401/403), `authService` |
| `pages/` | `auth/LoginPage`, `common/*` (403, 404, ComingSoon) |
| `styles/app.css` | CSS lấy nguyên từ prototype |

> Chặn route ở FE chỉ để UX. Phân quyền thật (RBAC, chống IDOR, khoá tài khoản, audit) do Backend thực thi.
