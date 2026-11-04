import { useNavigate } from 'react-router-dom'
import PageHeader from '@/components/PageHeader'
import { Async, EmptyState } from '@/components/StateViews'
import useAsync from '@/hooks/useAsync'
import { NAV } from '@/config/nav'
import { ROLES } from '@/config/constants'
import { listReopenRequests } from '@/services/referenceService'
import { fmtDateTime } from '@/utils/format'

const load = async () => {
  const [pending, approved, rejected] = await Promise.all(['PENDING', 'APPROVED', 'REJECTED'].map((s) => listReopenRequests(s).catch(() => [])))
  const byDate = (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
  return { pending: pending.sort(byDate), done: [...approved, ...rejected].sort(byDate) }
}

export default function ReopenRequestsPage() {
  const item = NAV[ROLES.MANAGER][3]
  const navigate = useNavigate()
  const d = useAsync(load, [])
  const open = (r) => navigate(`/manager/reopen-requests/${r.id}`)
  return (
    <>
      <PageHeader title={item.label} subtitle={item.subtitle} />
      <Async state={d}>
        {d.data && (
          <>
            <section className="card">
              <div className="ch"><h2>Đang chờ xem xét ({d.data.pending.length})</h2></div>
              {d.data.pending.length ? (
                <div className="tw">
                  <table>
                    <thead><tr><th>Ticket</th><th>Người yêu cầu</th><th>Lý do</th><th>Gửi lúc</th><th /></tr></thead>
                    <tbody>
                      {d.data.pending.map((r) => (
                        <tr key={r.id} tabIndex={0} onClick={() => open(r)} onKeyDown={(e) => e.key === 'Enter' && open(r)}>
                          <td className="nw"><button type="button" className="idlink" onClick={(e) => { e.stopPropagation(); open(r) }}>{r.ticketNumber ?? `#${r.ticketId}`}</button></td>
                          <td className="nw">{r.requesterName || `Người dùng #${r.requesterId}`}</td>
                          <td className="tt" style={{ fontWeight: 400, color: 'var(--muted)', minWidth: 240 }}>{r.reopenReason}</td>
                          <td className="nw num">{fmtDateTime(r.createdAt)}</td>
                          <td className="nw"><button type="button" className="btn sm" onClick={(e) => { e.stopPropagation(); open(r) }}>Xem xét</button></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <EmptyState icon="check" title="Không có yêu cầu mở lại nào đang chờ">Yêu cầu mới từ nhân viên sẽ hiện ở đây.</EmptyState>
              )}
            </section>
            {d.data.done.length > 0 && (
              <section className="card">
                <div className="ch"><h2>Đã xử lý</h2></div>
                <div className="tw">
                  <table>
                    <thead><tr><th>Ticket</th><th>Người yêu cầu</th><th>Lý do</th><th>Quyết định</th><th>Ghi chú</th></tr></thead>
                    <tbody>
                      {d.data.done.map((r) => (
                        <tr key={r.id} tabIndex={0} onClick={() => open(r)} onKeyDown={(e) => e.key === 'Enter' && open(r)}>
                          <td className="nw"><b>{r.ticketNumber ?? `#${r.ticketId}`}</b></td>
                          <td className="nw">{r.requesterName || `Người dùng #${r.requesterId}`}</td>
                          <td className="tt">{r.reopenReason}</td>
                          <td><span className={`badge ${r.status === 'APPROVED' ? 's-done' : 's-breach'}`}>{r.status === 'APPROVED' ? 'Đã duyệt' : 'Đã từ chối'}</span><div className="sm muted">{fmtDateTime(r.reviewedAt)}</div></td>
                          <td>{r.reviewNote || ''}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            )}
          </>
        )}
      </Async>
    </>
  )
}
