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
import CountUp, { useCountUp } from '@/components/CountUp'
import { dur, slaOf } from '@/utils/sla'
import { t } from '@/i18n'

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
  const sl = open.map((x) => ({ tk: x, s: slaOf(x) }))
  const near = sl.filter((x) => x.s.key === 'NEAR_BREACH').length
  const breach = sl.filter((x) => x.s.key === 'BREACHED').length
  const unassigned = open.filter((x) => x.status === 'NEW').length
  const okCount = open.length - near - breach
  const pct = open.length ? Math.round((okCount / open.length) * 100) : 100
  const mx = Math.max(1, ...PRIORITY_KEYS.map((p) => open.filter((x) => x.priority === p).length))
  const donutPct = useCountUp(pct) // vòng SLA vẽ dần từ 0 tới giá trị thật
  const attention = sl.filter((x) => x.s.key !== 'NORMAL').sort((a, b) => b.s.pct - a.s.pct)

  return (
    <>
      <PageHeader title={t(item.label)} subtitle={t(item.subtitle)} />
      <Async state={d}>
        {d.data && (
          <>
            {d.data.pending > 0 && (
              <div className="banner info">
                <Icon name="refresh" />
                <div className="grow">{t('Có {n} yêu cầu mở lại đang chờ bạn xem xét.', { n: '\u0001' }).split('\u0001').flatMap((part, i) => (i ? [<b key="n">{d.data.pending}</b>, part] : [part]))}</div>
                <button type="button" className="lnk" onClick={() => navigate('/manager/reopen-requests')}>{t('Xem ngay')}</button>
              </div>
            )}
            <div className="kpis">
              <Kpi label={t('Ticket đang mở')} value={open.length} tone="primary" icon="ticket" to="/manager/tickets" />
              <Kpi label={t('Chưa phân công')} value={unassigned} tone="info" icon="inbox" to="/manager/tickets?status=NEW" />
              <Kpi label={t('Sắp quá hạn')} value={near} tone="warn" icon="clock" to="/manager/sla" />
              <Kpi label={t('Quá hạn')} value={breach} tone="bad" icon="alert" to="/manager/sla" />
            </div>
            <div className="grid2">
              <section className="card">
                <div className="ch"><h2>{t('Ticket đang mở theo mức ưu tiên')}</h2></div>
                <div className="cb">
                  <div className="bars">
                    {PRIORITY_KEYS.slice().reverse().map((p) => {
                      const n = open.filter((x) => x.priority === p).length
                      const go = () => navigate(`/manager/tickets?priority=${p}`)
                      return (
                        <div
                          className="bar clk" key={p} role="link" tabIndex={0}
                          data-tip={t('{n} ticket mở, mức {p}. Bấm để xem danh sách.', { n, p: t(PRIORITY[p].label) })}
                          onClick={go} onKeyDown={(e) => e.key === 'Enter' && go()}
                        >
                          <span>{t(PRIORITY[p].label)}</span>
                          <i><s style={{ width: `${(n / mx) * 100}%`, background: BAR[p] }} /></i>
                          <b className="num"><CountUp value={n} /></b>
                        </div>
                      )
                    })}
                  </div>
                </div>
              </section>
              <section className="card">
                <div className="ch"><h2>{t('Tình trạng SLA của ticket đang mở')}</h2></div>
                <div className="cb">
                  <div className="donutw">
                    <div className="donut" role="img" aria-label={t('{n}% ticket đang mở còn trong hạn', { n: pct })} data-tip={t('{a} trong hạn, {b} sắp quá hạn hoặc quá hạn', { a: okCount, b: near + breach })} tabIndex={0} style={{ background: `conic-gradient(var(--ok) ${donutPct}%,var(--bad-bg) 0)` }}><div>{donutPct}%<small>{t('trong hạn')}</small></div></div>
                    <div className="legend">
                      <span><i style={{ background: 'var(--ok)' }} />{t('{n} ticket trong hạn', { n: okCount })}</span>
                      <span><i style={{ background: 'var(--bad)' }} />{t('{n} ticket sắp quá hạn hoặc quá hạn', { n: near + breach })}</span>
                    </div>
                  </div>
                </div>
              </section>
            </div>
            <section className="card">
              <div className="ch">
                <h2>{t('Cần can thiệp')}</h2>
                <button type="button" className="btn sm" onClick={() => navigate('/manager/sla')}>{t('Mở giám sát SLA')}</button>
              </div>
              {attention.length ? (
                <div className="tw" style={{ marginTop: 14 }}>
                  <table>
                    <thead><tr><th>{t('Mã')}</th><th>{t('Tiêu đề')}</th><th>{t('Ưu tiên')}</th><th>{t('Người xử lý')}</th><th>SLA</th></tr></thead>
                    <tbody>
                      {attention.map(({ tk, s }) => {
                        const go = () => navigate(`/manager/tickets/${encodeURIComponent(ticketRef(tk))}`)
                        const aid = tk.currentAssigneeId ?? tk.assigneeId
                        return (
                          <tr key={tk.id ?? tk.ticketNumber} data-a tabIndex={0} onClick={go} onKeyDown={(e) => e.key === 'Enter' && go()}>
                            <td><button type="button" className="idlink" onClick={(e) => { e.stopPropagation(); go() }}>{tk.ticketNumber ?? tk.id}</button></td>
                            <td className="tt">{tk.title}</td>
                            <td><PriorityBadge priority={tk.priority} /></td>
                            <td className="nw">{tk.assigneeName || (aid ? t('Người dùng #{id}', { id: aid }) : t('Chưa phân công'))}</td>
                            <td><SlaBadge ticket={tk} /> <span className="sm muted">{s.left > 0 ? t('còn {d}', { d: dur(s.left) }) : t('trễ {d}', { d: dur(s.left) })}</span></td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              ) : (
                <EmptyState icon="check" title={t('Không có ticket cần can thiệp')}>{t('Mọi ticket đang mở đều trong hạn.')}</EmptyState>
              )}
            </section>
          </>
        )}
      </Async>
    </>
  )
}
