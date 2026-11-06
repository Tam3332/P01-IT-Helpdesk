import { useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import { Kpi } from '@/components/Kpi'
import { PriorityBadge, SlaBadge } from '@/components/Badges'
import { Async, EmptyState } from '@/components/StateViews'
import useAsync from '@/hooks/useAsync'
import { PRIORITY, PRIORITY_KEYS } from '@/config/domain'
import { NAV } from '@/config/nav'
import { ROLES } from '@/config/constants'
import { fetchOpenTickets, ticketRef } from '@/services/ticketService'
import { listReopenRequests } from '@/services/referenceService'
import { dur, slaOf } from '@/utils/sla'

async function load() {
  const [open, pending] = await Promise.all([fetchOpenTickets(), listReopenRequests('PENDING').catch(() => [])])
  return { open, pending: pending.length }
}

const BAR = { LOW: 'var(--neu)', MEDIUM: 'var(--info)', HIGH: 'var(--warn)', CRITICAL: 'var(--bad)' }

export default function ManagerOverview() {
  const item = NAV[ROLES.MANAGER][0]
  const navigate = useNavigate()
  const d = useAsync(load, [])
  const open = d.data?.open || []
  const sl = open.map((t) => ({ t, s: slaOf(t) }))
  const near = sl.filter((x) => x.s.key === 'NEAR_BREACH').length
  const breach = sl.filter((x) => x.s.key === 'BREACHED').length
  const unassigned = open.filter((t) => t.status === 'NEW').length
  const okCount = open.length - near - breach
  const pct = open.length ? Math.round((okCount / open.length) * 100) : 100
  const mx = Math.max(1, ...PRIORITY_KEYS.map((p) => open.filter((t) => t.priority === p).length))
  const attention = sl.filter((x) => x.s.key !== 'NORMAL').sort((a, b) => b.s.pct - a.s.pct)

  return (
    <>
      <PageHeader title={item.label} subtitle={item.subtitle} />
      <Async state={d}>
        {d.data && (
          <>
            {d.data.pending > 0 && (
              <div className="banner info">
                <Icon name="refresh" />
                <div className="grow">Có <b>{d.data.pending}</b> yêu cầu mở lại đang chờ bạn xem xét.</div>
                <button type="button" className="lnk" onClick={() => navigate('/manager/reopen-requests')}>Xem ngay</button>
              </div>
            )}
            <div className="kpis">
              <Kpi label="Ticket đang mở" value={open.length} tone="primary" icon="ticket" />
              <Kpi label="Chưa phân công" value={unassigned} tone="info" icon="inbox" />
              <Kpi label="Sắp quá hạn" value={near} tone="warn" icon="clock" />
              <Kpi label="Quá hạn" value={breach} tone="bad" icon="alert" />
            </div>
            <div className="grid2">
              <section className="card">
                <div className="ch"><h2>Ticket đang mở theo mức ưu tiên</h2></div>
                <div className="cb">
                  <div className="bars">
                    {PRIORITY_KEYS.slice().reverse().map((p) => {
                      const n = open.filter((t) => t.priority === p).length
                      return (
                        <div className="bar" key={p}>
                          <span>{PRIORITY[p].label}</span>
                          <i><s style={{ width: `${(n / mx) * 100}%`, background: BAR[p] }} /></i>
                          <b className="num">{n}</b>
                        </div>
                      )
                    })}
                  </div>
                </div>
              </section>
              <section className="card">
                <div className="ch"><h2>Tình trạng SLA của ticket đang mở</h2></div>
                <div className="cb">
                  <div className="donutw">
                    <div className="donut" style={{ background: `conic-gradient(var(--ok) ${pct}%,var(--bad-bg) 0)` }}><div>{pct}%<small>trong hạn</small></div></div>
                    <div className="legend">
                      <span><i style={{ background: 'var(--ok)' }} />{okCount} ticket trong hạn</span>
                      <span><i style={{ background: 'var(--bad)' }} />{near + breach} ticket sắp quá hạn hoặc quá hạn</span>
                    </div>
                  </div>
                </div>
              </section>
            </div>
            <section className="card">
              <div className="ch">
                <h2>Cần can thiệp</h2>
                <button type="button" className="btn sm" onClick={() => navigate('/manager/sla')}>Mở giám sát SLA</button>
              </div>
              {attention.length ? (
                <div className="tw" style={{ marginTop: 14 }}>
                  <table>
                    <thead><tr><th>Mã</th><th>Tiêu đề</th><th>Ưu tiên</th><th>Người xử lý</th><th>SLA</th></tr></thead>
                    <tbody>
                      {attention.map(({ t, s }) => {
                        const go = () => navigate(`/manager/tickets/${encodeURIComponent(ticketRef(t))}`)
                        const aid = t.currentAssigneeId ?? t.assigneeId
                        return (
                          <tr key={t.id ?? t.ticketNumber} tabIndex={0} onClick={go} onKeyDown={(e) => e.key === 'Enter' && go()}>
                            <td><button type="button" className="idlink" onClick={(e) => { e.stopPropagation(); go() }}>{t.ticketNumber ?? t.id}</button></td>
                            <td className="tt">{t.title}</td>
                            <td><PriorityBadge priority={t.priority} /></td>
                            <td className="nw">{t.assigneeName || (aid ? `Người dùng #${aid}` : 'Chưa phân công')}</td>
                            <td><SlaBadge ticket={t} /> <span className="sm muted">{s.left > 0 ? `còn ${dur(s.left)}` : `trễ ${dur(s.left)}`}</span></td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              ) : (
                <EmptyState icon="check" title="Không có ticket cần can thiệp">Mọi ticket đang mở đều trong hạn.</EmptyState>
              )}
            </section>
          </>
        )}
      </Async>
    </>
  )
}
