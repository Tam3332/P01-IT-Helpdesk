import { useState } from 'react'
import Modal, { Field } from '@/components/Modal'
import useAsync from '@/hooks/useAsync'
import { LIMITS, PRIORITY, PRIORITY_KEYS, statusLabel } from '@/config/domain'
import { apiError } from '@/services/apiClient'
import { assignTicket, changeStatus, closeTicket, reopenTicket } from '@/services/ticketService'
import { listItStaff } from '@/services/referenceService'
import { fmtDateTime } from '@/utils/format'

// Mỗi hàm bọc lỗi API thành Error(message) để Modal hiển thị ngay trong hộp thoại.
const guard = async (fn, fallback) => {
  try {
    return await fn()
  } catch (e) {
    throw new Error(apiError(e, fallback))
  }
}

export function StartDialog({ ticket, ref_, onClose, onDone }) {
  return (
    <Modal title="Bắt đầu xử lý" okText="Bắt đầu xử lý" onClose={onClose} onOk={async () => { await guard(() => changeStatus(ref_, { status: 'IN_PROGRESS' }), 'Không đổi được trạng thái.'); onDone('Đã chuyển sang Đang xử lý.') }}>
      <p>Chuyển ticket <b>{ticket.ticketNumber}</b> sang <b>{statusLabel('IN_PROGRESS')}</b>?</p>
    </Modal>
  )
}

export function ResolveDialog({ ticket, ref_, onClose, onDone }) {
  const [note, setNote] = useState('')
  const n = note.trim().length
  return (
    <Modal
      title="Sự cố đã được giải quyết?" okText="Đã giải quyết" cancelText="Chưa xong, quay lại" okClass="ok" onClose={onClose}
      onOk={async () => {
        if (n < LIMITS.resolutionNoteMin) throw new Error(`Giải pháp cần ít nhất ${LIMITS.resolutionNoteMin} ký tự (hiện có ${n}).`)
        await guard(() => changeStatus(ref_, { status: 'RESOLVED', resolutionNote: note.trim() }), 'Không đổi được trạng thái.')
        onDone(`Ticket ${ticket.ticketNumber} đã chuyển sang ${statusLabel('RESOLVED')}.`)
      }}
    >
      <p className="muted">Nếu đã xong, ticket <b>{ticket.ticketNumber}</b> sẽ chuyển sang <b>{statusLabel('RESOLVED')}</b> và gửi cho nhân viên xác nhận.</p>
      <Field label="Giải pháp đã áp dụng" required htmlFor="sol" hint={`${n}/${LIMITS.resolutionNoteMin} ký tự tối thiểu`}>
        <textarea className="ta" id="sol" value={note} onChange={(e) => setNote(e.target.value)} placeholder="Mô tả nguyên nhân và cách khắc phục" />
      </Field>
    </Modal>
  )
}

export function CloseDialog({ ticket, ref_, onClose, onDone }) {
  return (
    <Modal title="Đóng ticket" okText="Đóng ticket" okClass="ok" onClose={onClose} onOk={async () => { await guard(() => closeTicket(ref_), 'Không đóng được ticket.'); onDone(`Đã đóng ticket ${ticket.ticketNumber}.`) }}>
      <p>Xác nhận sự cố của ticket <b>{ticket.ticketNumber}</b> đã được khắc phục? Ticket đóng sẽ không thể chỉnh sửa.</p>
    </Modal>
  )
}

export function ReopenDialog({ ticket, ref_, onClose, onDone }) {
  const [reason, setReason] = useState('')
  const n = reason.trim().length
  return (
    <Modal
      title="Yêu cầu mở lại ticket" okText="Gửi yêu cầu" onClose={onClose}
      onOk={async () => {
        if (n < LIMITS.reopenReasonMin) throw new Error(`Lý do mở lại cần ít nhất ${LIMITS.reopenReasonMin} ký tự (hiện có ${n}).`)
        await guard(() => reopenTicket(ref_, reason.trim()), 'Không gửi được yêu cầu.')
        onDone('Đã gửi yêu cầu mở lại cho IT Manager.')
      }}
    >
      <p className="muted">Ticket <b>{ticket.ticketNumber}</b> được giải quyết lúc {fmtDateTime(ticket.resolvedAt)}. IT Manager sẽ xem xét yêu cầu của bạn.</p>
      <Field label="Lý do mở lại" required htmlFor="rsn" hint={`${n}/${LIMITS.reopenReasonMin} ký tự tối thiểu`}>
        <textarea className="ta" id="rsn" value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Mô tả vì sao sự cố chưa được giải quyết" />
      </Field>
    </Modal>
  )
}

export function AssignDialog({ ticket, ref_, onClose, onDone }) {
  const staff = useAsync(listItStaff, [])
  const current = ticket.currentAssigneeId ?? ticket.assigneeId
  const [assigneeId, setAssigneeId] = useState(current ? String(current) : '')
  const [priority, setPriority] = useState(ticket.priority || '')
  const [note, setNote] = useState('')
  return (
    <Modal
      title={current ? 'Phân công lại' : 'Phân công ticket'} okText={current ? 'Phân công lại' : 'Phân công'} onClose={onClose}
      onOk={async () => {
        if (!assigneeId) throw new Error('Vui lòng chọn người phụ trách.')
        if (current && String(current) === assigneeId && priority === ticket.priority) throw new Error('Ticket đang giao cho người này. Hãy chọn người khác hoặc đổi mức ưu tiên.')
        await guard(() => assignTicket(ref_, { assigneeId: Number(assigneeId), priority: priority || undefined, assignmentNote: note.trim() || undefined }), 'Không phân công được.')
        onDone('Đã phân công ticket.')
      }}
    >
      <p className="muted"><b>{ticket.ticketNumber}</b> · {ticket.title}</p>
      <Field label="Người phụ trách chính (chỉ IT Staff)" htmlFor="stf" hint={staff.error || 'Mỗi ticket chỉ có một người phụ trách chính tại một thời điểm.'}>
        <select className="sel" id="stf" value={assigneeId} onChange={(e) => setAssigneeId(e.target.value)} disabled={staff.loading}>
          <option value="">{staff.loading ? 'Đang tải...' : 'Chọn IT Staff'}</option>
          {(staff.data || []).map((u) => <option key={u.id} value={u.id}>{u.fullName}{u.department ? ` · ${u.department}` : ''}</option>)}
        </select>
      </Field>
      <Field label="Mức ưu tiên chính thức" htmlFor="pri">
        <select className="sel" id="pri" value={priority} onChange={(e) => setPriority(e.target.value)}>
          {PRIORITY_KEYS.map((p) => <option key={p} value={p}>{PRIORITY[p].label}</option>)}
        </select>
      </Field>
      <Field label="Ghi chú phân công" htmlFor="asn">
        <textarea className="ta" id="asn" style={{ minHeight: 64 }} value={note} onChange={(e) => setNote(e.target.value)} />
      </Field>
    </Modal>
  )
}
