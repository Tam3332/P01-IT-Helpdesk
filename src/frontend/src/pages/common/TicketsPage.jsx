import { useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import TicketListCard from '@/components/TicketListCard'
import { useAuth } from '@/context/AuthContext'
import { ROLES } from '@/config/constants'
import { findNavItem } from '@/config/nav'
import { useLocation } from 'react-router-dom'

// Danh sách ticket cho Employee / Staff / Manager. Backend tự giới hạn theo quyền (HD05, IA-HD02).
export default function TicketsPage() {
  const { user } = useAuth()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const item = findNavItem(user.role, pathname)
  return (
    <>
      <PageHeader title={item.label} subtitle={item.subtitle}>
        {user.role === ROLES.EMPLOYEE && (
          <button type="button" className="btn pri" onClick={() => navigate('/employee/tickets/new')}>
            <Icon name="plus" />Tạo ticket
          </button>
        )}
      </PageHeader>
      <TicketListCard
        title={user.role === ROLES.MANAGER || user.role === ROLES.ADMIN ? 'Tất cả ticket' : 'Danh sách ticket'}
        fixed={user.role === ROLES.STAFF ? { assigneeId: user.id } : undefined}
      />
    </>
  )
}
