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
import { approveReopen, listItStaff, listReopenRequests, rejectReopen } from '@/services/referenceService'
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

// Duyệt mở lại: ghi chú tùy chọn; có thể chọn IT Staff mới, để trống thì giữ người xử lý cũ.
function ApproveDialog({ req, ticket, onClose, onDone }) {
  const staff = useAsync(listItStaff, [])
  const [note, setNote] = useState('')
  const [assigneeId, setAssigneeId] = useState('')
  const current = ticket?.assigneeName
  return (
    <Modal
      title="Phê duyệt mở lại" okText="Phê duyệt" okClass="ok" onClose={onClose}
      onOk={async () => {
        const body = {}
        if (note.trim()) body.reviewNote = note.trim()
        if (assigneeId) body.newAssigneeId = Number(assigneeId)
        try { await approveReopen(req.id, body) } catch (e) { throw new Error(apiError(e, 'Không duyệt được.')) }
        onDone(`Đã phê duyệt, ticket ${req.ticketNumber} được mở lại với thời hạn SLA mới.`)
      }}
    >
      <p className="muted">Ticket <b>{req.ticketNumber}</b> sẽ chuyển sang <b>Mở lại</b> và tính thời hạn xử lý SLA mới từ lúc duyệt.</p>
      <Field label="IT Staff xử lý" htmlFor="rsf" hint={staff.error || 'Để trống để giữ nguyên người xử lý trước đó.'}>
        <select className="sel" id="rsf" value={assigneeId} onChange={(e) => setAssigneeId(e.target.value)} disabled={staff.loading}>
          <option value="">{staff.loading ? 'Đang tải...' : `Giữ người xử lý cũ${current ? ` (${current})` : ''}`}</option>
          {(staff.data || []).map((u) => <option key={u.id} value={u.id}>{u.fullName}{u.department ? ` · ${u.department}` : ''}</option>)}
        </select>
      </Field>
      <Field label="Ghi chú duyệt" htmlFor="rvn" hint={`${note.length}/${LIMITS.reviewNoteMax} ký tự (không bắt buộc)`}>
        <textarea className="ta" id="rvn" style={{ minHeight: 64 }} maxLength={LIMITS.reviewNoteMax} value={note} onChange={(e) => setNote(e.target.value)} />
      </Field>
    </Modal>
  )
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
  const rejectLen = note.trim().length

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
      {dialog === 'ok' && <ApproveDialog req={req} ticket={ticket} onClose={() => setDialog(null)} onDone={(msg) => done(msg)} />}
      {dialog === 'no' && (
        <Modal title="Từ chối mở lại" okText="Từ chối" okClass="danger" onClose={() => setDialog(null)} onOk={async () => {
          if (note.trim().length < LIMITS.rejectNoteMin) throw new Error(`Lý do từ chối cần ít nhất ${LIMITS.rejectNoteMin} ký tự (hiện có ${rejectLen}).`)
          await wrap(() => rejectReopen(req.id, note.trim()), 'Không từ chối được.')
          done('Đã từ chối yêu cầu mở lại.', 'info')
        }}>
          <p className="muted">Ticket <b>{req.ticketNumber}</b> sẽ quay về trạng thái Đã giải quyết.</p>
          <Field label="Lý do từ chối" required htmlFor="rjn" hint={`${rejectLen}/${LIMITS.rejectNoteMin} ký tự tối thiểu`}><textarea className="ta" id="rjn" maxLength={LIMITS.reviewNoteMax} value={note} onChange={(e) => setNote(e.target.value)} placeholder="Giải thích để nhân viên hiểu quyết định" /></Field>
        </Modal>
      )}
    </>
  )
}
