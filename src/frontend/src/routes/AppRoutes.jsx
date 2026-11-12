import { Navigate, Route, Routes } from 'react-router-dom'
import MainLayout from '@/layouts/MainLayout'
import LoginPage from '@/pages/auth/LoginPage'
import NotFoundPage from '@/pages/common/NotFoundPage'
import ProfilePage from '@/pages/common/ProfilePage'
import ChangePasswordPage from '@/pages/common/ChangePasswordPage'
import TicketsPage from '@/pages/common/TicketsPage'
import SlaMonitorPage from '@/pages/common/SlaMonitorPage'
import TicketDetailPage from '@/pages/tickets/TicketDetailPage'
import EmployeeOverview from '@/pages/employee/EmployeeOverview'
import CreateTicketPage from '@/pages/employee/CreateTicketPage'
import StaffOverview from '@/pages/staff/StaffOverview'
import ManagerOverview from '@/pages/manager/ManagerOverview'
import ReopenRequestsPage from '@/pages/manager/ReopenRequestsPage'
import ReopenReviewPage from '@/pages/manager/ReopenReviewPage'
import AdminOverview from '@/pages/admin/AdminOverview'
import UsersPage from '@/pages/admin/UsersPage'
import SettingsPage from '@/pages/admin/SettingsPage'
import CategoriesPage from '@/pages/admin/CategoriesPage'
import SlaConfigPage from '@/pages/admin/SlaConfigPage'
import ReadonlyConfigPage from '@/pages/admin/ReadonlyConfigPage'
import AuditLogsPage from '@/pages/admin/AuditLogsPage'
import { HomeRedirect, RedirectIfAuthed, RequireAuth, RequireRole } from './ProtectedRoute'
import { ROLES } from '@/config/constants'

export default function AppRoutes() {
  return (
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
  )
}
