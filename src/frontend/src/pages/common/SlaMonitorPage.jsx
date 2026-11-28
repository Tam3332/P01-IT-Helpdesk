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
import { getSlaMonitor } from '@/services/dashboardService'
import { fmtDateTime } from '@/utils/format'
import { dur, slaOf } from '@/utils/sla'
import { t as tr } from '@/i18n'

// Giám sát SLA: gọi API backend GET /dashboard/sla-monitor (HD12, HD14), fallback sang fetchOpenTickets nếu cần
async function loadSlaData() {
  try {
    const items = await getSlaMonitor()
    if (items && items.length > 0) {
      return items.map((item) => ({
        id: item.ticketId,
        ticketNumber: item.ticketNumber,
        title: item.title,
        priority: item.priority,
        status: item.status,
        resolutionDeadline: item.resolutionDeadline,
        assigneeName: item.assigneeName,
        currentAssigneeId: item.currentAssigneeId,
        isSlaBreached: item.isSlaBreached,
        slaLabel: item.slaLabel,
        createdAt: item.createdAt,
      }))
    }
  } catch (e) {
    console.warn('Fallback sang fetchOpenTickets:', e)
  }
  return fetchOpenTickets()
}

export default function SlaMonitorPage() {
  const { user } = useAuth()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const item = findNavItem(user.role, pathname)
  const open = useAsync(loadSlaData, [])
  const [filter, setFilter] = useState('all')

  // Backend đã giới hạn: Staff chỉ nhận ticket được giao cho mình
  const mine = (open.data || [])
  const rows = mine.map((t) => ({ t, s: slaOf(t) })).sort((a, b) => b.s.pct - a.s.pct)
  const count = (k) => rows.filter((x) => x.s.key === k).length
  const shown = rows.filter((x) => filter === 'all' || x.s.key === filter)
  const seg = [
    ['all', tr('Tất cả ({n})', { n: rows.length })],
    ['NORMAL', tr('Bình thường ({n})', { n: count('NORMAL') })],
    ['NEAR_BREACH', tr('Sắp quá hạn ({n})', { n: count('NEAR_BREACH') })],
    ['BREACHED', tr('Quá hạn ({n})', { n: count('BREACHED') })],
  ]
  const go = (t) => navigate(`${ROLE_HOME[user.role]}/tickets/${encodeURIComponent(ticketRef(t))}`)

  return (
    <>
      <PageHeader title={tr(item.label)} subtitle={tr(item.subtitle)} />
      <Async state={open}>
        <div className="kpis">
          <Kpi label={tr('Bình thường')} value={count('NORMAL')} tone="ok" icon="check" />
          <Kpi label={tr('Sắp quá hạn (≥ 80% thời hạn)')} value={count('NEAR_BREACH')} tone="warn" icon="clock" />
          <Kpi label={tr('Quá hạn (> 100%)')} value={count('BREACHED')} tone="bad" icon="alert" />
        </div>
        <section className="card">
          <div className="ch">
            <h2>{tr('Ticket đang mở')}</h2>
            <div className="seg" role="group" aria-label={tr('Lọc theo tình trạng SLA')}>
              {seg.map(([k, n]) => <button key={k} type="button" aria-pressed={filter === k} onClick={() => setFilter(k)}>{n}</button>)}
            </div>
          </div>
          {shown.length ? (
            <div className="tw" style={{ marginTop: 14 }}>
              <table>
                <thead><tr><th>{tr('Mã')}</th><th>{tr('Tiêu đề')}</th><th>{tr('Ưu tiên')}</th><th>{tr('Người xử lý')}</th><th>{tr('Hạn xử lý')}</th><th>SLA</th></tr></thead>
                <tbody>
                  {shown.map(({ t, s }) => (
                    <tr key={t.id ?? t.ticketNumber} tabIndex={0} onClick={() => go(t)} onKeyDown={(e) => e.key === 'Enter' && go(t)}>
                      <td><button type="button" className="idlink" onClick={(e) => { e.stopPropagation(); go(t) }}>{t.ticketNumber ?? t.id}</button></td>
                      <td className="tt">{t.title}</td>
                      <td><PriorityBadge priority={t.priority} /></td>
                      <td className="nw">{t.assigneeName || ((t.currentAssigneeId ?? t.assigneeId) ? tr('Người dùng #{id}', { id: t.currentAssigneeId ?? t.assigneeId }) : t.status === 'NEW' ? tr('Chưa phân công') : '—')}</td>
                      <td className="nw num">{fmtDateTime(t.resolutionDeadline)}<div className="sm muted">{s.left > 0 ? tr('còn {d}', { d: dur(s.left) }) : tr('trễ {d}', { d: dur(s.left) })}</div></td>
                      <td><span className={`badge ${SLA[s.key]?.badge}`}>{tr(SLA[s.key]?.label)}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState icon="check" title={tr('Không có ticket ở mục này')} />
          )}
        </section>
      </Async>
    </>
  )
}
