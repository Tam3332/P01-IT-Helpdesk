import { Navigate, Route, Routes } from 'react-router-dom'
import { ROLES, ROLE_HOME } from '@/config/constants'
import { useAuth } from '@/context/AuthContext'
import ProtectedRoute from './ProtectedRoute'
import MainLayout from '@/layouts/MainLayout'
import LoginPage from '@/pages/auth/LoginPage'
import ForbiddenPage from '@/pages/auth/ForbiddenPage'
import NotFoundPage from '@/pages/auth/NotFoundPage'
import EmployeeDashboard from '@/pages/employee/EmployeeDashboard'
import MyTicketsPage from '@/pages/employee/MyTicketsPage'
import CreateTicketPage from '@/pages/employee/CreateTicketPage'
import TicketDetailPage from '@/pages/tickets/TicketDetailPage'
import StaffDashboard from '@/pages/staff/StaffDashboard'
import AssignedTicketsPage from '@/pages/staff/AssignedTicketsPage'
import ManagerDashboard from '@/pages/manager/ManagerDashboard'
import TicketManagementPage from '@/pages/manager/TicketManagementPage'
import ReopenRequestsPage from '@/pages/manager/ReopenRequestsPage'
import SlaMonitorPage from '@/pages/manager/SlaMonitorPage'
import AdminDashboard from '@/pages/admin/AdminDashboard'
import UsersPage from '@/pages/admin/UsersPage'
import SettingsPage from '@/pages/admin/SettingsPage'
import AuditLogsPage from '@/pages/admin/AuditLogsPage'

function HomeRedirect() {
  const { user } = useAuth()
  return <Navigate to={user ? ROLE_HOME[user.role] : '/login'} replace />
}

const { EMPLOYEE, STAFF, MANAGER, ADMIN } = ROLES

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/403" element={<ForbiddenPage />} />
      <Route path="/" element={<HomeRedirect />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<MainLayout />}>
          {/* Chi tiết ticket dùng chung cho mọi role đã đăng nhập (backend lọc quyền xem) */}
          <Route path="/tickets/:id" element={<TicketDetailPage />} />

          <Route element={<ProtectedRoute allowedRoles={[EMPLOYEE]} />}>
            <Route path="/employee" element={<EmployeeDashboard />} />
            <Route path="/employee/tickets" element={<MyTicketsPage />} />
            <Route path="/employee/tickets/new" element={<CreateTicketPage />} />
          </Route>

          <Route element={<ProtectedRoute allowedRoles={[STAFF]} />}>
            <Route path="/staff" element={<StaffDashboard />} />
            <Route path="/staff/tickets" element={<AssignedTicketsPage />} />
            <Route path="/staff/sla" element={<SlaMonitorPage />} />
          </Route>

          <Route element={<ProtectedRoute allowedRoles={[MANAGER]} />}>
            <Route path="/manager" element={<ManagerDashboard />} />
            <Route path="/manager/tickets" element={<TicketManagementPage />} />
            <Route path="/manager/sla" element={<SlaMonitorPage />} />
            <Route path="/manager/reopen-requests" element={<ReopenRequestsPage />} />
          </Route>

          <Route element={<ProtectedRoute allowedRoles={[ADMIN]} />}>
            <Route path="/admin" element={<AdminDashboard />} />
            <Route path="/admin/users" element={<UsersPage />} />
            <Route path="/admin/settings" element={<SettingsPage />} />
            <Route path="/admin/audit-logs" element={<AuditLogsPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
