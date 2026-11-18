import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import Modal, { Field } from '@/components/Modal'
import CustomSelect from '@/components/CustomSelect'
import { Async } from '@/components/StateViews'
import { useToast } from '@/context/ToastContext'
import useAsync from '@/hooks/useAsync'
import { LIMITS, PRIORITY, STATUS } from '@/config/domain'
import { apiError } from '@/services/apiClient'
import { getTicket, listHistory } from '@/services/ticketService'
import { approveReopen, listItStaff, listReopenRequests, rejectReopen } from '@/services/referenceService'
import { fmtDateTime } from '@/utils/format'
import useUnsavedGuard from '@/hooks/useUnsavedGuard'
import { t } from '@/i18n'

async function load(id) {
  let req = null
  for (const s of ['PENDING', 'APPROVED', 'REJECTED']) {
    const list = await listReopenRequests(s)
    req = list.find((r) => String(r.id) === String(id))
    if (req) break
  }
  if (!req) {
    const e = new Error('not found')
    e.response = { status: 404, data: { message: t('Không tìm thấy yêu cầu mở lại.') } }
    throw e
  }
  const ref = req.ticketNumber ?? req.ticketId
  const [ticket, history] = await Promise.all([getTicket(ref).catch(() => null), listHistory(ref).catch(() => [])])
  return { req, ticket, history }
}

function formatHistoryEntry(h) {
  let title = ''
  let note = h.reasonOrNote || ''

  if (h.action === 'CREATE') {
    title = t('Tạo mới ticket')
    note = ''
  } else if (h.action === 'ASSIGN' || h.fieldChanged === 'current_assignee_id') {
    title = t('Phân công xử lý')
  } else if (h.action === 'REASSIGN') {
    title = t('Phân công lại')
  } else if (h.action === 'REOPEN_REQUEST' || h.action === 'REOPEN') {
    title = t('Yêu cầu mở lại ticket')
  } else if (h.action === 'REOPEN_APPROVE') {
    title = t('Đồng ý mở lại ticket')
  } else if (h.action === 'REOPEN_REJECT') {
    title = t('Từ chối yêu cầu mở lại')
    if (note.startsWith('Từ chối yêu cầu mở lại: ')) {
      note = note.replace('Từ chối yêu cầu mở lại: ', '')
    }
  } else if (h.action === 'CLOSE') {
    title = t('Xác nhận đóng ticket')
  } else if (h.fieldChanged === 'status') {
    if (h.oldValue === 'REOPEN_PENDING' && h.newValue === 'RESOLVED') {
      title = t('Từ chối yêu cầu mở lại')
      if (note.startsWith('Từ chối yêu cầu mở lại: ')) {
        note = note.replace('Từ chối yêu cầu mở lại: ', '')
      }
    } else if (h.oldValue === 'REOPEN_PENDING' && h.newValue === 'REOPENED') {
      title = t('Đồng ý mở lại ticket')
    } else if (h.oldValue === 'RESOLVED' && h.newValue === 'REOPEN_PENDING') {
      title = t('Yêu cầu mở lại ticket')
    } else {
      const oldSt = t(STATUS[h.oldValue]?.label || h.oldValue || '—')
      const newSt = t(STATUS[h.newValue]?.label || h.newValue || '—')
      title = `${t('Đổi trạng thái')}: ${oldSt} → ${newSt}`
    }
  } else if (h.fieldChanged === 'priority') {
    const oldPr = t(PRIORITY[h.oldValue]?.label || h.oldValue || '—')
    const newPr = t(PRIORITY[h.newValue]?.label || h.newValue || '—')
    title = `${t('Đổi mức ưu tiên')}: ${oldPr} → ${newPr}`
  } else {
    title = t(h.action || 'Cập nhật')
  }

  // Dịch chuỗi note nếu là chuỗi hệ thống tự sinh
  if (note === 'Bắt đầu tiếp nhận xử lý ticket') {
    note = t('Bắt đầu tiếp nhận xử lý ticket')
  }

  return (
    <div>
      <div style={{ fontWeight: 600 }}>{title}</div>
      {note && (
        <div className="sm muted" style={{ marginTop: 2, overflowWrap: 'anywhere' }}>
          {note}
        </div>
      )}
    </div>
  )
}

// Duyệt mở lại: ghi chú tùy chọn; có thể chọn IT Staff mới, để trống thì giữ người xử lý cũ.
function ApproveDialog({ req, ticket, onClose, onDone }) {
  const staff = useAsync(listItStaff, [])
  const [note, setNote] = useState('')
  const [assigneeId, setAssigneeId] = useState('')
  const current = ticket?.assigneeName
  return (
    <Modal
      title={t('Phê duyệt mở lại')} okText={t('Phê duyệt')} okClass="ok" onClose={onClose}
      onOk={async () => {
        const body = {}
        if (note.trim()) body.reviewNote = note.trim()
        if (assigneeId) body.newAssigneeId = Number(assigneeId)
        try { await approveReopen(req.id, body) } catch (e) { throw new Error(apiError(e, t('Không duyệt được.'))) }
        onDone(t('Đã phê duyệt, ticket {n} được mở lại với thời hạn SLA mới.', { n: req.ticketNumber }))
      }}
    >
      <p className="muted">{t('Ticket {n} sẽ chuyển sang {s} và tính thời hạn xử lý SLA mới từ lúc duyệt.', { n: '\uE000', s: '\uE001' }).split(/[\uE000\uE001]/).flatMap((part, i) => (i === 1 ? [<b key="n">{req.ticketNumber}</b>, part] : i === 2 ? [<b key="s">{t('Mở lại')}</b>, part] : [part]))}</p>
      <Field label={t('IT Staff xử lý')} htmlFor="rsf" hint={staff.error || t('Để trống để giữ nguyên người xử lý trước đó.')}>
        <CustomSelect
          value={assigneeId}
          onChange={setAssigneeId}
          placeholder={staff.loading ? t('Đang tải...') : `${t('Giữ người xử lý cũ')}${current ? ` (${current})` : ''}`}
          options={[
            { value: '', label: staff.loading ? t('Đang tải...') : `${t('Giữ người xử lý cũ')}${current ? ` (${current})` : ''}` },
            ...(staff.data || []).map((u) => ({ value: u.id, label: `${u.fullName}${u.department ? ` · ${u.department}` : ''}` })),
          ]}
          disabled={staff.loading}
          ariaLabel={t('IT Staff xử lý')}
        />
      </Field>
      <Field label={t('Ghi chú duyệt')} htmlFor="rvn" hint={t('{n}/{max} ký tự (không bắt buộc)', { n: note.length, max: LIMITS.reviewNoteMax })}>
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
  useUnsavedGuard(dialog === 'no' && !!note.trim())

  const list = '/manager/reopen-requests'
  const back = <div><button type="button" className="btn ghost sm" onClick={() => navigate(list)}><Icon name="back" />{t('Quay lại danh sách yêu cầu')}</button></div>
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
      <PageHeader title={t('Xem xét yêu cầu mở lại')} subtitle={t('Đối chiếu lý do và lịch sử trước khi quyết định.')} />
      {!pending && <div className="banner info"><Icon name="info" /><div className="grow">{t('Yêu cầu này đã được xử lý: {r}.', { r: req.status === 'APPROVED' ? t('đã duyệt') : t('đã từ chối') })}</div></div>}
      <section className="card">
        <div className="ch">
          <div>
            <h2>{req.ticketNumber} · {ticket?.title || ''}</h2>
            <span className="sm muted">{t('{u} gửi lúc {d}', { u: req.requesterName || t('Người dùng #{id}', { id: req.requesterId }), d: fmtDateTime(req.createdAt) })}{ticket?.resolvedAt ? ` · ${t('giải quyết lúc {d}', { d: fmtDateTime(ticket.resolvedAt) })}` : ''}</span>
          </div>
          {ticket && <button type="button" className="btn sm" onClick={() => navigate(`/manager/tickets/${encodeURIComponent(req.ticketNumber ?? req.ticketId)}`)}>{t('Mở ticket')}</button>}
        </div>
        <div className="cb">
          <div className="grid2" style={{ gap: 24 }}>
            <div>
              <div className="lbl">{t('Lý do mở lại')}</div>
              <p style={{ marginTop: 6, background: 'var(--surface2)', padding: '10px 12px', borderRadius: 10, overflowWrap: 'anywhere' }}>{req.reopenReason}</p>
              <div className="lbl" style={{ marginTop: 16 }}>{t('Giải pháp trước đó')}</div>
              <p className="muted" style={{ marginTop: 4 }}>{ticket?.resolutionNote || '—'}</p>
              <div className="lbl" style={{ marginTop: 16 }}>{t('Mô tả ban đầu')}</div>
              <p className="muted" style={{ marginTop: 4, overflowWrap: 'anywhere' }}>{ticket?.description || '—'}</p>
              {req.reviewNote && <><div className="lbl" style={{ marginTop: 16 }}>{t('Ghi chú của người duyệt')}</div><p className="muted" style={{ marginTop: 4 }}>{req.reviewNote}</p></>}
            </div>
            <div>
              <div className="lbl">{t('Lịch sử xử lý')}</div>
              <ul className="tl" style={{ marginTop: 12 }}>
                {history.slice().sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp)).map((h) => (
                  <li key={h.id}>
                    <i />
                    <div>
                      {formatHistoryEntry(h)}
                      <div className="sm muted">{fmtDateTime(h.timestamp)}</div>
                    </div>
                  </li>
                ))}
                {!history.length && <li><i /><div className="muted">{t('Chưa có lịch sử.')}</div></li>}
              </ul>
            </div>
          </div>
        </div>
      </section>
      {pending && (
        <div className="acts">
          <button type="button" className="btn ok" onClick={() => setDialog('ok')}><Icon name="check" />{t('Phê duyệt mở lại')}</button>
          <button type="button" className="btn danger" onClick={() => setDialog('no')}><Icon name="x" />{t('Từ chối')}</button>
        </div>
      )}
      {dialog === 'ok' && <ApproveDialog req={req} ticket={ticket} onClose={() => setDialog(null)} onDone={(msg) => done(msg)} />}
      {dialog === 'no' && (
        <Modal title={t('Từ chối mở lại')} okText={t('Từ chối')} okClass="danger" onClose={() => setDialog(null)} onOk={async () => {
          if (note.trim().length < LIMITS.rejectNoteMin) throw new Error(t('Lý do từ chối cần ít nhất {min} ký tự (hiện có {n}).', { min: LIMITS.rejectNoteMin, n: rejectLen }))
          await wrap(() => rejectReopen(req.id, note.trim()), t('Không từ chối được.'))
          done(t('Đã từ chối yêu cầu mở lại.'), 'info')
        }}>
          <p className="muted">{t('Ticket {n} sẽ quay về trạng thái Đã giải quyết.', { n: '\uE000' }).split('\uE000').flatMap((part, i) => (i ? [<b key="n">{req.ticketNumber}</b>, part] : [part]))}</p>
          <Field label={t('Lý do từ chối')} required htmlFor="rjn" hint={t('{n}/{min} ký tự tối thiểu', { n: rejectLen, min: LIMITS.rejectNoteMin })}><textarea className="ta" id="rjn" maxLength={LIMITS.reviewNoteMax} value={note} onChange={(e) => setNote(e.target.value)} placeholder={t('Giải thích để nhân viên hiểu quyết định')} /></Field>
        </Modal>
      )}
    </>
  )
}
