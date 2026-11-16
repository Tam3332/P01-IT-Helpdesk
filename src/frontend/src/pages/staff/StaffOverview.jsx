import PageHeader from '@/components/PageHeader'
import { Kpi } from '@/components/Kpi'
import { Async } from '@/components/StateViews'
import TicketListCard from '@/components/TicketListCard'
import { useAuth } from '@/context/AuthContext'
import useAsync from '@/hooks/useAsync'
import { fetchOpenTickets, countTickets } from '@/services/ticketService'
import { slaOf } from '@/utils/sla'
import { NAV } from '@/config/nav'
import { ROLES } from '@/config/constants'
import { STATUS_KEYS } from '@/config/domain'
import { t } from '@/i18n'

// Backend chỉ trả ticket được giao cho Staff (SRS §4 HD05), không còn ticket NEW.
async function load() {
  const [open, total] = await Promise.all([fetchOpenTickets(), countTickets()])
  return { open, total }
}

export default function StaffOverview() {
  const item = NAV[ROLES.STAFF][0]
  const { user } = useAuth()
  const d = useAsync(load, [])
  const open = d.data?.open || []
  return (
    <>
      <PageHeader title={t(item.label)} subtitle={t(item.subtitle)} />
      <Async state={d}>
        {d.data && (
          <div className="kpis">
            <Kpi label={t('Ticket được giao')} value={d.data.total} tone="primary" icon="inbox" to="/staff/tickets" />
            <Kpi label={t('Đang mở')} value={open.length} tone="info" icon="clock" to="/staff/tickets" />
            <Kpi label={t('Ưu tiên cao')} value={open.filter((x) => x.priority === 'HIGH' || x.priority === 'CRITICAL').length} tone="warn" icon="flag" />
            <Kpi label={t('Cảnh báo SLA')} value={open.filter((x) => slaOf(x).key !== 'NORMAL').length} tone="bad" icon="alert" to="/staff/sla" />
          </div>
        )}
      </Async>
      <TicketListCard title={t('Ticket cần xử lý')} fixed={{ assigneeId: user.id }} statuses={STATUS_KEYS.filter((s) => s !== 'NEW')} />
    </>
  )
}
