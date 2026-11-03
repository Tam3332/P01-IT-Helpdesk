import { useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import { Kpi } from '@/components/Kpi'
import { Async } from '@/components/StateViews'
import TicketListCard from '@/components/TicketListCard'
import useAsync from '@/hooks/useAsync'
import { getStatusCounts } from '@/services/ticketService'
import { OPEN_STATUSES } from '@/config/domain'
import { NAV } from '@/config/nav'
import { ROLES } from '@/config/constants'

export default function EmployeeOverview() {
  const navigate = useNavigate()
  const counts = useAsync(getStatusCounts, [])
  const item = NAV[ROLES.EMPLOYEE][0]
  const c = counts.data
  const sum = (keys) => keys.reduce((a, k) => a + (c?.[k] || 0), 0)
  return (
    <>
      <PageHeader title={item.label} subtitle={item.subtitle}>
        <button type="button" className="btn pri" onClick={() => navigate('/employee/tickets/new')}>
          <Icon name="plus" />
          Tạo ticket
        </button>
      </PageHeader>
      <Async state={counts}>
        {c && (
          <div className="kpis">
            <Kpi label="Tổng số ticket" value={sum(Object.keys(c))} tone="primary" icon="ticket" />
            <Kpi
              label="Đang mở"
              value={sum([...OPEN_STATUSES, 'REOPEN_PENDING'])}
              tone="info"
              icon="clock"
            />
            <Kpi label="Đã giải quyết (chờ xác nhận)" value={c.RESOLVED} tone="ok" icon="check" />
            <Kpi label="Đã đóng" value={c.CLOSED} tone="neu" icon="lock" />
          </div>
        )}
      </Async>
      <TicketListCard title="Ticket gần đây" />
    </>
  )
}
