import { Suspense, lazy } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import Loading from '@/components/Loading'
import MainLayout from '@/layouts/MainLayout'
import LoginPage from '@/pages/auth/LoginPage'
import { HomeRedirect, RedirectIfAuthed, RequireAuth, RequireRole } from './ProtectedRoute'
import { ROLES } from '@/config/constants'
import { useLang } from '@/context/LangContext'
import useDocTitle from '@/hooks/useDocTitle'
import { t } from '@/i18n'

const NotFoundPage = lazy(() => import('@/pages/common/NotFoundPage'))
const ProfilePage = lazy(() => import('@/pages/common/ProfilePage'))
const ChangePasswordPage = lazy(() => import('@/pages/common/ChangePasswordPage'))
const TicketsPage = lazy(() => import('@/pages/common/TicketsPage'))
const SlaMonitorPage = lazy(() => import('@/pages/common/SlaMonitorPage'))
const TicketDetailPage = lazy(() => import('@/pages/tickets/TicketDetailPage'))
const EmployeeOverview = lazy(() => import('@/pages/employee/EmployeeOverview'))
const CreateTicketPage = lazy(() => import('@/pages/employee/CreateTicketPage'))
const StaffOverview = lazy(() => import('@/pages/staff/StaffOverview'))
const ManagerOverview = lazy(() => import('@/pages/manager/ManagerOverview'))
const ReopenRequestsPage = lazy(() => import('@/pages/manager/ReopenRequestsPage'))
const ReopenReviewPage = lazy(() => import('@/pages/manager/ReopenReviewPage'))
const AdminOverview = lazy(() => import('@/pages/admin/AdminOverview'))
const UsersPage = lazy(() => import('@/pages/admin/UsersPage'))
const SettingsPage = lazy(() => import('@/pages/admin/SettingsPage'))
const CategoriesPage = lazy(() => import('@/pages/admin/CategoriesPage'))
const SlaConfigPage = lazy(() => import('@/pages/admin/SlaConfigPage'))
const ReadonlyConfigPage = lazy(() => import('@/pages/admin/ReadonlyConfigPage'))
const AuditLogsPage = lazy(() => import('@/pages/admin/AuditLogsPage'))

export default function AppRoutes() {
  useLang() // đổi ngôn ngữ thì tạo lại các element bên dưới để t() đọc lại, state giữ nguyên
  const { pathname } = useLocation()
  useDocTitle(pathname === '/login' ? t('Đăng nhập') : undefined)
  return (
    <Suspense fallback={<Loading />}>
    <Routes>
      <Route path="/login" element={<RedirectIfAuthed><LoginPage /></RedirectIfAuthed>} />
      <Route element={<RequireAuth />}>
        <Route element={<MainLayout />}>
          <Route index element={<HomeRedirect />} />

          <Route path="employee" element={<RequireRole role={ROLES.EMPLOYEE} />}>
            <Route index element={<EmployeeOverview />} />
            <Route path="tickets" element={<TicketsPage />} />
            <Route path="tickets/new" element={<CreateTicketPage />} />
            <Route path="tickets/:ticketId" element={<TicketDetailPage />} />
            <Route path="profile" element={<ProfilePage />} />
            <Route path="profile/password" element={<ChangePasswordPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>

          <Route path="staff" element={<RequireRole role={ROLES.STAFF} />}>
            <Route index element={<StaffOverview />} />
            <Route path="tickets" element={<TicketsPage />} />
            <Route path="tickets/:ticketId" element={<TicketDetailPage />} />
            <Route path="sla" element={<SlaMonitorPage />} />
            <Route path="profile" element={<ProfilePage />} />
            <Route path="profile/password" element={<ChangePasswordPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>

          <Route path="manager" element={<RequireRole role={ROLES.MANAGER} />}>
            <Route index element={<ManagerOverview />} />
            <Route path="tickets" element={<TicketsPage />} />
            <Route path="tickets/:ticketId" element={<TicketDetailPage />} />
            <Route path="sla" element={<SlaMonitorPage />} />
            <Route path="reopen-requests" element={<ReopenRequestsPage />} />
            <Route path="reopen-requests/:requestId" element={<ReopenReviewPage />} />
            <Route path="profile" element={<ProfilePage />} />
            <Route path="profile/password" element={<ChangePasswordPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>

          <Route path="admin" element={<RequireRole role={ROLES.ADMIN} />}>
            <Route index element={<AdminOverview />} />
            <Route path="tickets" element={<TicketsPage />} />
            <Route path="tickets/:ticketId" element={<TicketDetailPage />} />
            <Route path="users" element={<UsersPage />} />
            <Route path="settings" element={<SettingsPage />} />
            <Route path="categories" element={<CategoriesPage />} />
            <Route path="priorities" element={<ReadonlyConfigPage kind="priorities" />} />
            <Route path="statuses" element={<ReadonlyConfigPage kind="statuses" />} />
            <Route path="sla-config" element={<SlaConfigPage />} />
            <Route path="audit-logs" element={<AuditLogsPage />} />
            <Route path="profile" element={<ProfilePage />} />
            <Route path="profile/password" element={<ChangePasswordPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>

          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
    </Suspense>
  )
}
