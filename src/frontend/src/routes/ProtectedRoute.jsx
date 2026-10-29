import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'

// Chưa đăng nhập -> /login ; sai role -> /403.
// Lưu ý: chặn ở FE chỉ để UX, backend mới là nơi thực thi RBAC thật.
export default function ProtectedRoute({ allowedRoles }) {
  const { user } = useAuth()
  const location = useLocation()
  if (!user) return <Navigate to="/login" state={{ from: location }} replace />
  if (allowedRoles && !allowedRoles.includes(user.role)) return <Navigate to="/403" replace />
  return <Outlet />
}
