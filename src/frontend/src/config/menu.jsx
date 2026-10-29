import {
  DashboardOutlined, UnorderedListOutlined, PlusCircleOutlined, TeamOutlined, ClockCircleOutlined,
  RedoOutlined, SettingOutlined, AuditOutlined, UserOutlined,
} from '@ant-design/icons'
import { ROLES } from './constants'

// key = đường dẫn route
export const MENU_BY_ROLE = {
  [ROLES.EMPLOYEE]: [
    { key: '/employee', icon: <DashboardOutlined />, label: 'Tổng quan' },
    { key: '/employee/tickets', icon: <UnorderedListOutlined />, label: 'Ticket của tôi' },
    { key: '/employee/tickets/new', icon: <PlusCircleOutlined />, label: 'Tạo ticket' },
  ],
  [ROLES.STAFF]: [
    { key: '/staff', icon: <DashboardOutlined />, label: 'Tổng quan' },
    { key: '/staff/tickets', icon: <UnorderedListOutlined />, label: 'Ticket được giao' },
    { key: '/staff/sla', icon: <ClockCircleOutlined />, label: 'Theo dõi SLA' },
  ],
  [ROLES.MANAGER]: [
    { key: '/manager', icon: <DashboardOutlined />, label: 'Tổng quan' },
    { key: '/manager/tickets', icon: <TeamOutlined />, label: 'Quản lý ticket' },
    { key: '/manager/sla', icon: <ClockCircleOutlined />, label: 'Theo dõi SLA' },
    { key: '/manager/reopen-requests', icon: <RedoOutlined />, label: 'Yêu cầu mở lại' },
  ],
  [ROLES.ADMIN]: [
    { key: '/admin', icon: <DashboardOutlined />, label: 'Tổng quan' },
    { key: '/admin/users', icon: <UserOutlined />, label: 'Người dùng' },
    { key: '/admin/settings', icon: <SettingOutlined />, label: 'Cấu hình' },
    { key: '/admin/audit-logs', icon: <AuditOutlined />, label: 'Audit log (chỉ đọc)' },
  ],
}
