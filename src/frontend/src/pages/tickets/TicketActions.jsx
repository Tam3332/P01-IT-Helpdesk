import { useState } from 'react'
import Modal, { Field } from '@/components/Modal'
import CustomSelect from '@/components/CustomSelect'
import useAsync from '@/hooks/useAsync'
import { LIMITS, PRIORITY, PRIORITY_KEYS, statusLabel } from '@/config/domain'
import { apiError } from '@/services/apiClient'
import { assignTicket, changeStatus, closeTicket, reopenTicket } from '@/services/ticketService'
import { listItStaff } from '@/services/referenceService'
import { fmtDateTime } from '@/utils/format'
import { t } from '@/i18n'

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
    <Modal title={t('Bắt đầu xử lý')} okText={t('Bắt đầu xử lý')} onClose={onClose} onOk={async () => { await guard(() => changeStatus(ref_, { status: 'IN_PROGRESS' }), t('Không đổi được trạng thái.')); onDone(t('Đã chuyển sang Đang xử lý.')) }}>
      <p>{t('Chuyển ticket {n} sang {s}?', { n: ticket.ticketNumber, s: statusLabel('IN_PROGRESS') })}</p>
    </Modal>
  )
}

export function ResolveDialog({ ticket, ref_, onClose, onDone }) {
  const [note, setNote] = useState('')
  const n = note.trim().length
  return (
    <Modal
      title={t('Sự cố đã được giải quyết?')} okText={t('Đã giải quyết')} cancelText={t('Chưa xong, quay lại')} okClass="ok" onClose={onClose}
      onOk={async () => {
        if (n < LIMITS.resolutionNoteMin) throw new Error(t('Giải pháp cần ít nhất {min} ký tự (hiện có {n}).', { min: LIMITS.resolutionNoteMin, n }))
        await guard(() => changeStatus(ref_, { status: 'RESOLVED', resolutionNote: note.trim() }), t('Không đổi được trạng thái.'))
        onDone(t('Ticket {n} đã chuyển sang {s}.', { n: ticket.ticketNumber, s: statusLabel('RESOLVED') }))
      }}
    >
      <p className="muted">{t('Nếu đã xong, ticket {n} sẽ chuyển sang {s} và gửi cho nhân viên xác nhận.', { n: ticket.ticketNumber, s: statusLabel('RESOLVED') })}</p>
      <Field label={t('Giải pháp đã áp dụng')} required htmlFor="sol" hint={t('{n}/{min} ký tự tối thiểu', { n, min: LIMITS.resolutionNoteMin })}>
        <textarea className="ta" id="sol" value={note} onChange={(e) => setNote(e.target.value)} placeholder={t('Mô tả nguyên nhân và cách khắc phục')} />
      </Field>
    </Modal>
  )
}

export function CloseDialog({ ticket, ref_, onClose, onDone }) {
  return (
    <Modal title={t('Đóng ticket')} okText={t('Đóng ticket')} okClass="ok" onClose={onClose} onOk={async () => { await guard(() => closeTicket(ref_), t('Không đóng được ticket.')); onDone(t('Đã đóng ticket {n}.', { n: ticket.ticketNumber })) }}>
      <p>{t('Xác nhận sự cố của ticket {n} đã được khắc phục? Ticket đóng sẽ không thể chỉnh sửa.', { n: ticket.ticketNumber })}</p>
    </Modal>
  )
}

export function ReopenDialog({ ticket, ref_, onClose, onDone }) {
  const [reason, setReason] = useState('')
  const n = reason.trim().length
  return (
    <Modal
      title={t('Yêu cầu mở lại ticket')} okText={t('Gửi yêu cầu')} onClose={onClose}
      onOk={async () => {
        if (n < LIMITS.reopenReasonMin) throw new Error(t('Lý do mở lại cần ít nhất {min} ký tự (hiện có {n}).', { min: LIMITS.reopenReasonMin, n }))
        await guard(() => reopenTicket(ref_, reason.trim()), t('Không gửi được yêu cầu.'))
        try {
          window.dispatchEvent(new Event('ticket_updated'))
        } catch { /* không ảnh hưởng luồng chính */ }
        onDone(t('Đã gửi yêu cầu mở lại cho IT Manager.'))
      }}
    >
      <p className="muted">{t('Ticket {n} được giải quyết lúc {d}. IT Manager sẽ xem xét yêu cầu của bạn.', { n: ticket.ticketNumber, d: fmtDateTime(ticket.resolvedAt) })}</p>
      <Field label={t('Lý do mở lại')} required htmlFor="rsn" hint={t('{n}/{min} ký tự tối thiểu', { n, min: LIMITS.reopenReasonMin })}>
        <textarea className="ta" id="rsn" value={reason} onChange={(e) => setReason(e.target.value)} placeholder={t('Mô tả vì sao sự cố chưa được giải quyết')} />
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
      title={current ? t('Phân công lại') : t('Phân công ticket')} okText={current ? t('Phân công lại') : t('Phân công')} onClose={onClose}
      onOk={async () => {
        if (!assigneeId) throw new Error(t('Vui lòng chọn người phụ trách.'))
        if (current && String(current) === assigneeId && priority === ticket.priority) throw new Error(t('Ticket đang giao cho người này. Hãy chọn người khác hoặc đổi mức ưu tiên.'))
        await guard(() => assignTicket(ref_, { assigneeId: Number(assigneeId), priority: priority || undefined, assignmentNote: note.trim() || undefined }), t('Không phân công được.'))
        onDone(t('Đã phân công ticket.'))
      }}
    >
      <p className="muted"><b>{ticket.ticketNumber}</b> · {ticket.title}</p>
      <Field label={t('Người phụ trách chính (chỉ IT Staff)')} htmlFor="stf" hint={staff.error || t('Mỗi ticket chỉ có một người phụ trách chính tại một thời điểm.')}>
        <CustomSelect
          value={assigneeId}
          onChange={setAssigneeId}
          placeholder={staff.loading ? t('Đang tải...') : t('Chọn IT Staff')}
          options={[
            { value: '', label: staff.loading ? t('Đang tải...') : t('Chọn IT Staff') },
            ...(staff.data || []).map((u) => ({ value: u.id, label: `${u.fullName}${u.department ? ` · ${u.department}` : ''}` })),
          ]}
          disabled={staff.loading}
          ariaLabel={t('Chọn IT Staff')}
        />
      </Field>
      <Field label={t('Mức ưu tiên chính thức')} htmlFor="pri">
        <CustomSelect
          value={priority}
          onChange={setPriority}
          options={PRIORITY_KEYS.map((p) => ({ value: p, label: t(PRIORITY[p].label) }))}
          ariaLabel={t('Mức ưu tiên chính thức')}
        />
      </Field>
      <Field label={t('Ghi chú phân công')} htmlFor="asn">
        <textarea className="ta" id="asn" style={{ minHeight: 64 }} value={note} onChange={(e) => setNote(e.target.value)} />
      </Field>
    </Modal>
  )
}
