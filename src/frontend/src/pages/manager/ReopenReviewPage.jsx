import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import Modal, { Field } from '@/components/Modal'
import { Async } from '@/components/StateViews'
import { useToast } from '@/context/ToastContext'
import useAsync from '@/hooks/useAsync'
import { LIMITS } from '@/config/domain'
import { apiError } from '@/services/apiClient'
import { getTicket, listHistory } from '@/services/ticketService'
import { approveReopen, listReopenRequests, rejectReopen } from '@/services/referenceService'
import { fmtDateTime } from '@/utils/format'

async function load(id) {
  let req = null
  for (const s of ['PENDING', 'APPROVED', 'REJECTED']) {
    const list = await listReopenRequests(s)
    req = list.find((r) => String(r.id) === String(id))
    if (req) break
  }
  if (!req) {
    const e = new Error('not found')
    e.response = { status: 404, data: { message: 'Không tìm thấy yêu cầu mở lại.' } }
    throw e
  }
  const ref = req.ticketNumber ?? req.ticketId
  const [ticket, history] = await Promise.all([getTicket(ref).catch(() => null), listHistory(ref).catch(() => [])])
  return { req, ticket, history }
}

export default function ReopenReviewPage() {
  const { requestId } = useParams()
  const navigate = useNavigate()
  const toast = useToast()
  const d = useAsync(() => load(requestId), [requestId])
  const [dialog, setDialog] = useState(null)
  const [note, setNote] = useState('')

  const list = '/manager/reopen-requests'
  const back = <div><button type="button" className="btn ghost sm" onClick={() => navigate(list)}><Icon name="back" />Quay lại danh sách yêu cầu</button></div>
  if (d.loading && !d.data) return <>{back}<Async state={d} /></>
  if (!d.data) return <>{back}<Async state={d} /></>

  const { req, ticket, history } = d.data
  const pending = req.status === 'PENDING'
  const done = (msg, type) => { toast(msg, type); navigate(list) }
  const wrap = async (fn, fallback) => { try { return await fn() } catch (e) { throw new Error(apiError(e, fallback)) } }

  return (
    <>
      {back}
      <PageHeader title="Xem xét yêu cầu mở lại" subtitle="Đối chiếu lý do và lịch sử trước khi quyết định." />
      {!pending && <div className="banner info"><Icon name="info" /><div className="grow">Yêu cầu này đã được xử lý: {req.status === 'APPROVED' ? 'đã duyệt' : 'đã từ chối'}.</div></div>}
      <section className="card">
        <div className="ch">
          <div>
            <h2>{req.ticketNumber} · {ticket?.title || ''}</h2>
            <span className="sm muted">{req.requesterName || `Người dùng #${req.requesterId}`} gửi lúc {fmtDateTime(req.createdAt)}{ticket?.resolvedAt ? ` · giải quyết lúc ${fmtDateTime(ticket.resolvedAt)}` : ''}</span>
          </div>
          {ticket && <button type="button" className="btn sm" onClick={() => navigate(`/manager/tickets/${encodeURIComponent(req.ticketNumber ?? req.ticketId)}`)}>Mở ticket</button>}
        </div>
        <div className="cb">
          <div className="grid2" style={{ gap: 24 }}>
            <div>
              <div className="lbl">Lý do mở lại</div>
              <p style={{ marginTop: 6, background: 'var(--surface2)', padding: '10px 12px', borderRadius: 10, overflowWrap: 'anywhere' }}>{req.reopenReason}</p>
              <div className="lbl" style={{ marginTop: 16 }}>Giải pháp trước đó</div>
              <p className="muted" style={{ marginTop: 4 }}>{ticket?.resolutionNote || '—'}</p>
              <div className="lbl" style={{ marginTop: 16 }}>Mô tả ban đầu</div>
              <p className="muted" style={{ marginTop: 4, overflowWrap: 'anywhere' }}>{ticket?.description || '—'}</p>
              {req.reviewNote && <><div className="lbl" style={{ marginTop: 16 }}>Ghi chú của người duyệt</div><p className="muted" style={{ marginTop: 4 }}>{req.reviewNote}</p></>}
            </div>
            <div>
              <div className="lbl">Lịch sử xử lý</div>
              <ul className="tl" style={{ marginTop: 12 }}>
                {history.slice().sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp)).map((h) => (
                  <li key={h.id}><i /><div><div>{h.action}{h.reasonOrNote ? `: ${h.reasonOrNote}` : ''}</div><div className="sm muted">{fmtDateTime(h.timestamp)}</div></div></li>
                ))}
                {!history.length && <li><i /><div className="muted">Chưa có lịch sử.</div></li>}
              </ul>
            </div>
          </div>
        </div>
      </section>
      {pending && (
        <div className="acts">
          <button type="button" className="btn ok" onClick={() => setDialog('ok')}><Icon name="check" />Phê duyệt mở lại</button>
          <button type="button" className="btn danger" onClick={() => setDialog('no')}><Icon name="x" />Từ chối</button>
        </div>
      )}
      {dialog === 'ok' && (
        <Modal title="Phê duyệt mở lại" okText="Phê duyệt" okClass="ok" onClose={() => setDialog(null)} onOk={async () => { await wrap(() => approveReopen(req.id), 'Không duyệt được.'); done(`Đã phê duyệt, ticket ${req.ticketNumber} được mở lại. Hãy phân công người xử lý.`) }}>
          <p className="muted">Ticket <b>{req.ticketNumber}</b> sẽ chuyển sang <b>Mở lại</b> với thời hạn SLA mới tính từ lúc duyệt. Sau đó bạn phân công người xử lý ở trang chi tiết ticket.</p>
        </Modal>
      )}
      {dialog === 'no' && (
        <Modal title="Từ chối mở lại" okText="Từ chối" okClass="danger" onClose={() => setDialog(null)} onOk={async () => {
          if (note.trim().length < LIMITS.rejectNoteMin) throw new Error(`Vui lòng nhập lý do từ chối (ít nhất ${LIMITS.rejectNoteMin} ký tự).`)
          await wrap(() => rejectReopen(req.id, note.trim()), 'Không từ chối được.')
          done('Đã từ chối yêu cầu mở lại.', 'info')
        }}>
          <p className="muted">Ticket <b>{req.ticketNumber}</b> giữ nguyên trạng thái Đã giải quyết.</p>
          <Field label="Lý do từ chối" required htmlFor="rjn"><textarea className="ta" id="rjn" value={note} onChange={(e) => setNote(e.target.value)} placeholder="Giải thích để nhân viên hiểu quyết định" /></Field>
        </Modal>
      )}
    </>
  )
}
