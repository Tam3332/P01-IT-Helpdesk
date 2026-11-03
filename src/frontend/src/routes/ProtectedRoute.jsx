import { Navigate, Outlet, useLocation } from 'react-router-dom'
import Loading from '@/components/Loading'
import { useAuth } from '@/context/AuthContext'
import { ROLE_HOME } from '@/config/constants'
import ForbiddenPage from '@/pages/common/ForbiddenPage'

// Chưa đăng nhập -> /login (nhớ trang đang muốn vào). Sai vai trò -> trang 403.
// Đây chỉ là lớp UX; Backend vẫn là nơi quyết định quyền (trả 403 cho mọi API trái quyền).
export function RequireAuth() {
  const { user, ready } = useAuth()
  const location = useLocation()
  if (!ready) return <Loading text="Đang kiểm tra phiên đăng nhập..." />
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  return <Outlet />
}

export function RequireRole({ role }) {
  const { user } = useAuth()
  return user.role === role ? <Outlet /> : <ForbiddenPage />
}

// /login khi đã đăng nhập -> về trang chủ của vai trò
export function RedirectIfAuthed({ children }) {
  const { user, ready } = useAuth()
  if (!ready) return <Loading text="Đang kiểm tra phiên đăng nhập..." />
  return user ? <Navigate to={ROLE_HOME[user.role]} replace /> : children
}

export function HomeRedirect() {
  const { user } = useAuth()
  return <Navigate to={ROLE_HOME[user.role]} replace />
}
