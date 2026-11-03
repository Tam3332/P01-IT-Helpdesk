import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import PageHeader from '@/components/PageHeader'
import { Kpi } from '@/components/Kpi'
import { PriorityBadge } from '@/components/Badges'
import { Async, EmptyState } from '@/components/StateViews'
import { useAuth } from '@/context/AuthContext'
import useAsync from '@/hooks/useAsync'
import { SLA } from '@/config/domain'
import { ROLE_HOME } from '@/config/constants'
import { findNavItem } from '@/config/nav'
import { fetchOpenTickets, ticketRef } from '@/services/ticketService'
import { fmtDateTime } from '@/utils/format'
import { dur, slaOf } from '@/utils/sla'

// Giám sát SLA: tính từ danh sách ticket đang mở (slaLabel do backend, thiếu thì tính từ hạn).
// Khi API GET /dashboard/sla-monitor (HD14) sẵn sàng có thể thay nguồn dữ liệu ở đây.
export default function SlaMonitorPage() {
  const { user } = useAuth()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const item = findNavItem(user.role, pathname)
  const open = useAsync(fetchOpenTickets, [])
  const [filter, setFilter] = useState('all')

  const rows = (open.data || []).map((t) => ({ t, s: slaOf(t) })).sort((a, b) => b.s.pct - a.s.pct)
  const count = (k) => rows.filter((x) => x.s.key === k).length
  const shown = rows.filter((x) => filter === 'all' || x.s.key === filter)
  const seg = [
    ['all', `Tất cả (${rows.length})`],
    ['NORMAL', `Bình thường (${count('NORMAL')})`],
    ['NEAR_BREACH', `Sắp quá hạn (${count('NEAR_BREACH')})`],
    ['BREACHED', `Quá hạn (${count('BREACHED')})`],
  ]
  const go = (t) => navigate(`${ROLE_HOME[user.role]}/tickets/${encodeURIComponent(ticketRef(t))}`)

  return (
    <>
      <PageHeader title={item.label} subtitle={item.subtitle} />
      <Async state={open}>
        <div className="kpis">
          <Kpi label="Bình thường" value={count('NORMAL')} tone="ok" icon="check" />
          <Kpi label="Sắp quá hạn (≥ 80% thời hạn)" value={count('NEAR_BREACH')} tone="warn" icon="clock" />
          <Kpi label="Quá hạn (> 100%)" value={count('BREACHED')} tone="bad" icon="alert" />
        </div>
        <section className="card">
          <div className="ch">
            <h2>Ticket đang mở</h2>
            <div className="seg" role="group" aria-label="Lọc theo tình trạng SLA">
              {seg.map(([k, n]) => <button key={k} type="button" aria-pressed={filter === k} onClick={() => setFilter(k)}>{n}</button>)}
            </div>
          </div>
          {shown.length ? (
            <div className="tw" style={{ marginTop: 14 }}>
              <table>
                <thead><tr><th>Mã</th><th>Tiêu đề</th><th>Ưu tiên</th><th>Người xử lý</th><th>Hạn xử lý</th><th>Tiến độ thời hạn</th><th>SLA</th></tr></thead>
                <tbody>
                  {shown.map(({ t, s }) => (
                    <tr key={t.id ?? t.ticketNumber} tabIndex={0} onClick={() => go(t)} onKeyDown={(e) => e.key === 'Enter' && go(t)}>
                      <td><button type="button" className="idlink" onClick={(e) => { e.stopPropagation(); go(t) }}>{t.ticketNumber ?? t.id}</button></td>
                      <td className="tt">{t.title}</td>
                      <td><PriorityBadge priority={t.priority} /></td>
                      <td className="nw">{t.assigneeName || ((t.currentAssigneeId ?? t.assigneeId) ? `Người dùng #${t.currentAssigneeId ?? t.assigneeId}` : 'Chưa phân công')}</td>
                      <td className="nw num">{fmtDateTime(t.resolutionDeadline)}<div className="sm muted">{s.left > 0 ? `còn ${dur(s.left)}` : `trễ ${dur(s.left)}`}</div></td>
                      <td style={{ minWidth: 150 }}>
                        <div className="prog" role="img" aria-label={`${Math.round(s.pct)}% thời hạn đã dùng`}><s style={{ width: `${Math.min(100, s.pct)}%`, background: SLA[s.key]?.color }} /></div>
                        <span className="sm muted num">{Math.round(s.pct)}%</span>
                      </td>
                      <td><span className={`badge ${SLA[s.key]?.badge}`}>{SLA[s.key]?.label}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState icon="check" title="Không có ticket ở mục này" />
          )}
        </section>
      </Async>
    </>
  )
}
