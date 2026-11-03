import PageHeader from '@/components/PageHeader'
import { Kpi } from '@/components/Kpi'
import { Async } from '@/components/StateViews'
import TicketListCard from '@/components/TicketListCard'
import useAsync from '@/hooks/useAsync'
import { fetchOpenTickets, countTickets } from '@/services/ticketService'
import { slaOf } from '@/utils/sla'
import { NAV } from '@/config/nav'
import { ROLES } from '@/config/constants'

async function load() {
  const [open, total] = await Promise.all([fetchOpenTickets(), countTickets()])
  return { open, total }
}

export default function StaffOverview() {
  const item = NAV[ROLES.STAFF][0]
  const d = useAsync(load, [])
  const open = d.data?.open || []
  return (
    <>
      <PageHeader title={item.label} subtitle={item.subtitle} />
      <Async state={d}>
        {d.data && (
          <div className="kpis">
            <Kpi label="Ticket được giao" value={d.data.total} tone="primary" icon="inbox" />
            <Kpi label="Đang mở" value={open.length} tone="info" icon="clock" />
            <Kpi
              label="Ưu tiên cao"
              value={open.filter((t) => t.priority === 'HIGH' || t.priority === 'CRITICAL').length}
              tone="warn"
              icon="flag"
            />
            <Kpi
              label="Cảnh báo SLA"
              value={open.filter((t) => slaOf(t).key !== 'NORMAL').length}
              tone="bad"
              icon="alert"
            />
          </div>
        )}
      </Async>
      <TicketListCard title="Ticket cần xử lý" />
    </>
  )
}
