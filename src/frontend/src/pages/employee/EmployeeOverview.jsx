import { useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import { Kpi } from '@/components/Kpi'
import { Async } from '@/components/StateViews'
import TicketListCard from '@/components/TicketListCard'
import useAsync from '@/hooks/useAsync'
import { getStatusCounts } from '@/services/ticketService'
import { getDashboardStats } from '@/services/dashboardService'
import { OPEN_STATUSES } from '@/config/domain'
import { NAV } from '@/config/nav'
import { ROLES } from '@/config/constants'
import { t } from '@/i18n'

async function loadEmployeeStats() {
  try {
    const stats = await getDashboardStats()
    if (stats?.byStatus) {
      return stats.byStatus
    }
  } catch {
    // Fallback sang hàm đếm cũ nếu lỗi
  }
  return getStatusCounts()
}

export default function EmployeeOverview() {
  const navigate = useNavigate()
  const counts = useAsync(loadEmployeeStats, [])
  const item = NAV[ROLES.EMPLOYEE][0]
  const c = counts.data
  const sum = (keys) => keys.reduce((a, k) => a + (c?.[k] || 0), 0)
  return (
    <>
      <PageHeader title={t(item.label)} subtitle={t(item.subtitle)}>
        <button type="button" className="btn pri" onClick={() => navigate('/employee/tickets/new')}>
          <Icon name="plus" />{t('Tạo ticket')}
        </button>
      </PageHeader>
      <Async state={counts}>
        {c && (
          <div className="kpis">
            <Kpi label={t('Tổng số ticket')} value={sum(Object.keys(c))} tone="primary" icon="ticket" to="/employee/tickets" />
            <Kpi label={t('Đang mở')} value={sum([...OPEN_STATUSES, 'REOPEN_PENDING'])} tone="info" icon="clock" />
            <Kpi label={t('Đã giải quyết (chờ xác nhận)')} value={c.RESOLVED} tone="ok" icon="check" to="/employee/tickets?status=RESOLVED" />
            <Kpi label={t('Đã đóng')} value={c.CLOSED} tone="neu" icon="lock" to="/employee/tickets?status=CLOSED" />
          </div>
        )}
      </Async>
      <TicketListCard title={t('Ticket gần đây')} />
    </>
  )
}
