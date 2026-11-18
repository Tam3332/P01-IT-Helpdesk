import { useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import { Kpi } from '@/components/Kpi'
import useAsync from '@/hooks/useAsync'
import { PRIORITY_KEYS, STATUS_KEYS } from '@/config/domain'
import { NAV } from '@/config/nav'
import { ROLES } from '@/config/constants'
import { adminListCategories, adminListUsers, listAuditLogs } from '@/services/referenceService'
import { listAllTickets } from '@/services/ticketService'
import { t } from '@/i18n'

// Mỗi số liệu tải riêng: API nào lỗi chỉ hiện "—" ở ô đó.
const settle = (p) => p.then((v) => v).catch(() => null)
const load = async () => {
  const [usersRes, cats, logs, ticketsList] = await Promise.all([
    settle(adminListUsers({ page: 0, size: 200 })),
    settle(adminListCategories().then((r) => r.length)),
    settle(listAuditLogs({ page: 0, size: 1 }).then((r) => r.totalElements)),
    settle(listAllTickets({}, { pageSize: 100, maxPages: 10 })),
  ])
  const userList = usersRes?.content || []
  const totalUsers = usersRes?.totalElements ?? userList.length
  const tickets = ticketsList || []
  return { users: totalUsers, userList, cats, logs, tickets }
}

function DonutChart({ total, totalLabel, segments }) {
  const sum = segments.reduce((acc, s) => acc + (s.count || 0), 0)

  let gradient = 'var(--surface2)'
  if (sum > 0) {
    let acc = 0
    const stops = []
    segments.forEach((s) => {
      if (!s.count) return
      const from = acc
      const to = acc + (s.count / sum) * 100
      stops.push(`${s.color} ${from.toFixed(1)}% ${to.toFixed(1)}%`)
      acc = to
    })
    if (stops.length > 0) {
      gradient = `conic-gradient(${stops.join(', ')})`
    }
  }

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 36, flexWrap: 'wrap' }}>
      <div
        style={{
          width: 200,
          height: 200,
          borderRadius: '50%',
          background: gradient,
          display: 'grid',
          placeItems: 'center',
          flex: 'none',
          boxShadow: '0 4px 14px rgba(0,0,0,0.22)',
        }}
      >
        <div
          style={{
            width: 136,
            height: 136,
            borderRadius: '50%',
            background: 'var(--surface)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center',
            padding: 6,
          }}
        >
          <b style={{ fontSize: 36, lineHeight: 1.1, fontVariantNumeric: 'tabular-nums' }}>{total}</b>
          <small style={{ fontSize: 14, color: 'var(--muted)', marginTop: 4 }}>{totalLabel}</small>
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 12, flex: 1, minWidth: 220 }}>
        {segments.map((s) => {
          const pct = sum > 0 ? ((s.count / sum) * 100).toFixed(1) : '0.0'
          return (
            <div
              key={s.label}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: 14,
                fontSize: 16,
                paddingBottom: 8,
                borderBottom: '1px solid var(--border)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <span
                  style={{
                    width: 14,
                    height: 14,
                    borderRadius: '50%',
                    background: s.color,
                    flex: 'none',
                  }}
                />
                <span style={{ fontWeight: 500 }}>{s.label}</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <b style={{ fontSize: 16 }}>{s.count}</b>
                <span className="sm muted" style={{ fontSize: 14 }}>({pct}%)</span>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export default function AdminOverview() {
  const item = NAV[ROLES.ADMIN][0]
  const navigate = useNavigate()
  const d = useAsync(load, [])
  const v = d.data || {}

  const userCounts = {
    EMPLOYEE: 0,
    IT_STAFF: 0,
    IT_MANAGER: 0,
    ADMIN: 0,
  }
  ;(v.userList || []).forEach((u) => {
    const r = u.role === 'STAFF' ? 'IT_STAFF' : u.role === 'MANAGER' ? 'IT_MANAGER' : u.role
    if (userCounts[r] != null) userCounts[r] += 1
  })

  const priorityCounts = {
    LOW: 0,
    MEDIUM: 0,
    HIGH: 0,
    CRITICAL: 0,
  }
  ;(v.tickets || []).forEach((tk) => {
    if (priorityCounts[tk.priority] != null) priorityCounts[tk.priority] += 1
  })

  const userSegments = [
    { label: t('Nhân viên'), count: userCounts.EMPLOYEE, color: 'var(--ok)' },
    { label: t('IT Staff'), count: userCounts.IT_STAFF, color: 'var(--info)' },
    { label: t('IT Manager'), count: userCounts.IT_MANAGER, color: 'var(--primary)' },
    { label: t('Quản trị viên'), count: userCounts.ADMIN, color: 'var(--vio)' },
  ]

  const ticketSegments = [
    { label: t('Thấp'), count: priorityCounts.LOW, color: 'var(--neu)' },
    { label: t('Trung bình'), count: priorityCounts.MEDIUM, color: 'var(--info)' },
    { label: t('Cao'), count: priorityCounts.HIGH, color: 'var(--warn)' },
    { label: t('Nghiêm trọng'), count: priorityCounts.CRITICAL, color: 'var(--bad)' },
  ]

  return (
    <>
      <PageHeader title={t(item.label)} subtitle={t(item.subtitle)} />
      <div className="kpis" style={{ gridTemplateColumns: 'repeat(auto-fit,minmax(200px,1fr))' }}>
        <Kpi label={t('Người dùng')} value={d.loading ? '…' : v.users} tone="primary" icon="users" />
        <Kpi label={t('Danh mục')} value={d.loading ? '…' : v.cats} tone="info" icon="tag" />
        <Kpi label={t('Mức ưu tiên')} value={PRIORITY_KEYS.length} tone="info" icon="flag" />
        <Kpi label={t('Trạng thái')} value={STATUS_KEYS.length} tone="vio" icon="layers" />
        <Kpi label={t('Bản ghi nhật ký')} value={d.loading ? '…' : v.logs} tone="neu" icon="file" />
      </div>
      <div className="banner info"><Icon name="shield" /><div className="grow">{t('Quản trị viên không xử lý ticket (phân tách nhiệm vụ): chỉ xem toàn bộ ticket ở chế độ chỉ đọc, quản lý người dùng, cấu hình hệ thống và xem nhật ký.')}</div></div>
      <section className="card">
        <div className="ch"><h2>{t('Truy cập nhanh')}</h2></div>
        <div className="cb">
          <div className="acts">
            <button type="button" className="btn pri" onClick={() => navigate('/admin/users')}><Icon name="users" />{t('Người dùng')}</button>
            <button type="button" className="btn" onClick={() => navigate('/admin/settings')}><Icon name="sliders" />{t('Cài đặt hệ thống')}</button>
            <button type="button" className="btn" onClick={() => navigate('/admin/audit-logs')}><Icon name="file" />{t('Nhật ký hệ thống')}</button>
          </div>
        </div>
      </section>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: 20 }}>
        <section className="card">
          <div className="ch"><h2>{t('Người dùng theo vai trò')}</h2></div>
          <div className="cb" style={{ padding: 28 }}>
            <DonutChart
              total={d.loading ? '…' : (v.users ?? 0)}
              totalLabel={t('người dùng')}
              segments={userSegments}
            />
          </div>
        </section>
        <section className="card">
          <div className="ch"><h2>{t('Ticket: {n} ticket', { n: v.tickets?.length ?? 0 })}</h2></div>
          <div className="cb" style={{ padding: 28 }}>
            <DonutChart
              total={d.loading ? '…' : (v.tickets?.length ?? 0)}
              totalLabel={t('ticket')}
              segments={ticketSegments}
            />
          </div>
        </section>
      </div>
    </>
  )
}
