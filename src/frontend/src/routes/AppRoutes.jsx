import { Navigate, Route, Routes } from 'react-router-dom'
import MainLayout from '@/layouts/MainLayout'
import LoginPage from '@/pages/auth/LoginPage'
import NotFoundPage from '@/pages/common/NotFoundPage'
import ComingSoonPage from '@/pages/common/ComingSoonPage'
import { HomeRedirect, RedirectIfAuthed, RequireAuth, RequireRole } from './ProtectedRoute'
import { ROLES } from '@/config/constants'

// [đường dẫn tương đối, đợt sẽ nối API]. Thay phần tử ComingSoonPage bằng trang thật khi làm đợt đó.
const AREAS = [
  [ROLES.EMPLOYEE, 'employee', [['', 5], ['tickets', 2], ['tickets/new', 2], ['tickets/:id', 2]]],
  [ROLES.STAFF, 'staff', [['', 5], ['tickets', 3], ['tickets/:id', 3], ['sla', 5]]],
  [ROLES.MANAGER, 'manager', [['', 5], ['tickets', 3], ['tickets/:id', 3], ['sla', 5], ['reopen-requests', 3]]],
  [
    ROLES.ADMIN,
    'admin',
    [['', 5], ['users', 4], ['settings', 4], ['categories', 4], ['priorities', 4], ['statuses', 4], ['sla-config', 4], ['audit-logs', 4]],
  ],
]

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<RedirectIfAuthed><LoginPage /></RedirectIfAuthed>} />
      <Route element={<RequireAuth />}>
        <Route element={<MainLayout />}>
          <Route index element={<HomeRedirect />} />
          {AREAS.map(([role, base, pages]) => (
            <Route key={base} path={base} element={<RequireRole role={role} />}>
              {pages.map(([p, wave]) =>
                p === '' ? (
                  <Route key="index" index element={<ComingSoonPage wave={wave} />} />
                ) : (
                  <Route key={p} path={p} element={<ComingSoonPage wave={wave} />} />
                ),
              )}
              <Route path="*" element={<NotFoundPage />} />
            </Route>
          ))}
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
