import { useRef, useState } from 'react'
import Icon from '@/components/Icon'
import { Async } from '@/components/StateViews'
import { validateFiles } from '@/components/FilePicker'
import { useToast } from '@/context/ToastContext'
import useAsync from '@/hooks/useAsync'
import { LIMITS, ALLOWED_EXT, STATUS, PRIORITY } from '@/config/domain'
import { apiError } from '@/services/apiClient'
import { addComment, downloadAttachment, getFeedback, listAttachments, listComments, listHistory, sendFeedback, uploadAttachment } from '@/services/ticketService'
import { initials, fmtDateTime } from '@/utils/format'
import { humanSize } from '@/utils/sla'

const PENDING_API = 'Backend chưa có API này. Mục sẽ hiện dữ liệu khi API được triển khai.'

export function CommentsCard({ ticketRef, nameOf, canComment, canInternal, closed, onChanged }) {
  const toast = useToast()
  const list = useAsync(() => listComments(ticketRef), [ticketRef])
  const [text, setText] = useState('')
  const [internal, setInternal] = useState(false)
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    const v = text.trim()
    if (!v) return toast('Vui lòng nhập nội dung bình luận.', 'err')
    if (v.length > LIMITS.comment) return toast(`Bình luận tối đa ${LIMITS.comment} ký tự.`, 'err')
    setBusy(true)
    try {
      await addComment(ticketRef, { commentText: v, isInternal: canInternal && internal })
      setText('')
      setInternal(false)
      toast('Đã gửi bình luận.')
      await list.reload()
      onChanged?.()
    } catch (err) {
      toast(apiError(err, 'Không gửi được bình luận.'), 'err')
    } finally {
      setBusy(false)
    }
  }

  const items = list.data || []
  return (
    <section className="card">
      <div className="ch"><h2>Bình luận ({items.length})</h2></div>
      <div className="cb">
        <Async state={list} notReadyText={PENDING_API}>
          {items.length ? (
            items.map((c) => {
              const nm = nameOf(c.authorId, c.authorName)
              return (
                <div className="cm" key={c.id}>
                  <div className="av">{initials(nm)}</div>
                  <div className="bub">
                    <div className="meta">
                      <b>{nm}</b>{c.authorRole ? ` · ${c.authorRole}` : ''} · {fmtDateTime(c.createdAt)}
                      {c.isInternal && <span className="badge b-x" style={{ marginLeft: 8 }}>Nội bộ</span>}
                    </div>
                    <span style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{c.commentText}</span>
                  </div>
                </div>
              )
            })
          ) : (
            <p className="muted">Chưa có bình luận nào.</p>
          )}
        </Async>
        {canComment ? (
          <form onSubmit={submit} style={{ marginTop: 18, display: 'flex', flexDirection: 'column', gap: 8 }}>
            <label className="lbl" htmlFor="cmtext">Thêm bình luận</label>
            <textarea className="ta" id="cmtext" style={{ minHeight: 76 }} maxLength={LIMITS.comment} value={text} onChange={(e) => setText(e.target.value)} placeholder="Nhập nội dung trao đổi" />
            {canInternal && (
              <label className="chk">
                <input type="checkbox" checked={internal} onChange={(e) => setInternal(e.target.checked)} /> Ghi chú nội bộ (nhân viên không thấy)
              </label>
            )}
            <div>
              <button className="btn pri sm" type="submit" disabled={busy}><Icon name="send" />{busy ? 'Đang gửi...' : 'Gửi bình luận'}</button>
            </div>
          </form>
        ) : (
          <p className="sm muted" style={{ marginTop: 16 }}>{closed ? 'Ticket đã đóng nên không thể bình luận thêm.' : 'Vai trò hiện tại không được bình luận trên ticket này.'}</p>
        )}
      </div>
    </section>
  )
}

export function AttachmentsCard({ ticketRef, canUpload, onChanged }) {
  const toast = useToast()
  const list = useAsync(() => listAttachments(ticketRef), [ticketRef])
  const input = useRef(null)
  const [busy, setBusy] = useState(false)
  const items = list.data || []

  const pick = async (e) => {
    const { ok, errors } = validateFiles([...e.target.files], items.length)
    e.target.value = ''
    if (errors.length) toast(errors.join(' '), 'err')
    if (!ok.length) return
    setBusy(true)
    let added = 0
    for (const f of ok) {
      try {
        await uploadAttachment(ticketRef, f)
        added += 1
      } catch (err) {
        toast(`"${f.name}": ${apiError(err, 'tải lên thất bại')}`, 'err')
      }
    }
    setBusy(false)
    if (added) {
      toast(`Đã tải lên ${added} tệp.`)
      await list.reload()
      onChanged?.()
    }
  }
  const download = async (a) => {
    try {
      await downloadAttachment(ticketRef, a)
    } catch (err) {
      toast(apiError(err, 'Không tải được tệp.'), 'err')
    }
  }

  return (
    <section className="card">
      <div className="ch">
        <h2>Tệp đính kèm ({items.length})</h2>
        {canUpload && (
          <>
            <button type="button" className="btn sm" onClick={() => input.current?.click()} disabled={busy}><Icon name="upload" />{busy ? 'Đang tải...' : 'Tải tệp lên'}</button>
            <input type="file" multiple hidden ref={input} onChange={pick} />
          </>
        )}
      </div>
      <div className="cb">
        {canUpload && <p className="sm muted" style={{ marginBottom: 12 }}>Tối đa {LIMITS.maxFiles} tệp, mỗi tệp ≤ 10 MB ({ALLOWED_EXT.join(', ')}).</p>}
        <Async state={list} notReadyText={PENDING_API}>
          {items.length ? (
            <div style={{ display: 'grid', gap: 8 }}>
              {items.map((a) => (
                <div className="att" key={a.id}>
                  <Icon name="clip" />
                  <span>{a.fileName}</span>
                  <span className="sm muted" style={{ flex: 'none' }}>{humanSize(a.fileSize)}</span>
                  <button type="button" className="btn ghost sm" onClick={() => download(a)} aria-label={`Tải ${a.fileName}`}>Tải về</button>
                </div>
              ))}
            </div>
          ) : (
            <p className="muted">Ticket này không có tệp đính kèm.</p>
          )}
        </Async>
      </div>
    </section>
  )
}

const ACTION_LABEL = {
  CREATE: 'Tạo mới ticket',
  ASSIGN: 'Phân công xử lý',
  REASSIGN: 'Phân công lại',
  STATUS_CHANGE: 'Đổi trạng thái',
  COMMENT: 'Bình luận trao đổi',
  ATTACH_FILE: 'Đính kèm tệp tin',
  CLOSE: 'Xác nhận đóng ticket',
  REOPEN: 'Mở lại ticket',
}

const FIELD_LABEL = {
  status: 'Trạng thái',
  priority: 'Mức ưu tiên',
  assignee: 'Người xử lý',
  current_assignee_id: 'Người xử lý',
  currentAssigneeId: 'Người xử lý',
}

function historyText(h, nameOf) {
  const actionName = ACTION_LABEL[h.action] || h.action || 'Cập nhật'

  // Trường hợp bình luận hoặc đính kèm: không in ID kỹ thuật
  if (h.action === 'COMMENT' || h.fieldChanged === 'comment_id') {
    return 'Bình luận trao đổi'
  }
  if (h.action === 'ATTACH_FILE' || h.fieldChanged === 'attachment_id') {
    return 'Đính kèm tệp tin'
  }
  if (h.action === 'CREATE') {
    return 'Tạo mới ticket'
  }
  if (h.action === 'CLOSE') {
    return 'Xác nhận đóng ticket'
  }
  if (h.action === 'FEEDBACK' || h.fieldChanged === 'feedback') {
    return `Đánh giá chất lượng hỗ trợ (${h.newValue || 'Hoàn tất'})`
  }
  if (h.action === 'REOPEN') {
    return 'Yêu cầu mở lại ticket'
  }

  // Trường hợp phân công người xử lý: map ID thành tên người dùng
  if (h.fieldChanged === 'current_assignee_id' || h.fieldChanged === 'currentAssigneeId') {
    const oldName = h.oldValue && nameOf ? nameOf(Number(h.oldValue), h.oldValueLabel) : (h.oldValue ? `Người dùng #${h.oldValue}` : 'Chưa phân công')
    const newName = h.newValue && nameOf ? nameOf(Number(h.newValue), h.newValueLabel) : (h.newValue ? `Người dùng #${h.newValue}` : 'Chưa phân công')
    return `${actionName} · Người xử lý: ${oldName} → ${newName}`
  }

  // Trường hợp đổi trạng thái: hiển thị label tiếng Việt
  if (h.fieldChanged === 'status') {
    const oldSt = STATUS[h.oldValue]?.label || h.oldValue || '—'
    const newSt = STATUS[h.newValue]?.label || h.newValue || '—'
    return `Đổi trạng thái: ${oldSt} → ${newSt}`
  }

  // Trường hợp đổi mức ưu tiên: hiển thị label tiếng Việt
  if (h.fieldChanged === 'priority') {
    const oldPr = PRIORITY[h.oldValue]?.label || h.oldValue || '—'
    const newPr = PRIORITY[h.newValue]?.label || h.newValue || '—'
    return `Đổi mức ưu tiên: ${oldPr} → ${newPr}`
  }

  const field = FIELD_LABEL[h.fieldChanged] || h.fieldChanged
  const ch = field ? ` · ${field}: ${h.oldValue ?? '—'} → ${h.newValue ?? '—'}` : ''
  return actionName + ch
}

export function HistoryCard({ ticketRef, nameOf, refreshKey }) {
  const list = useAsync(() => listHistory(ticketRef), [ticketRef, refreshKey])
  const items = (list.data || []).slice().sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
  return (
    <section className="card">
      <div className="ch"><h2>Lịch sử xử lý</h2></div>
      <div className="cb">
        <Async state={list} notReadyText={PENDING_API}>
          {items.length ? (
            <ul className="tl">
              {items.map((h) => (
                <li key={h.id}>
                  <i />
                  <div>
                    <div style={{ fontWeight: 600 }}>{historyText(h, nameOf)}</div>
                    {h.reasonOrNote && <div className="sm muted" style={{ overflowWrap: 'anywhere' }}>{h.reasonOrNote}</div>}
                    <div className="sm muted">{nameOf(h.actorId, h.actorName)} · {fmtDateTime(h.timestamp)}</div>
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <p className="muted">Chưa có lịch sử.</p>
          )}
        </Async>
      </div>
    </section>
  )
}

export function FeedbackCard({ ticketRef }) {
  const toast = useToast()
  const fb = useAsync(() => getFeedback(ticketRef), [ticketRef])
  const [stars, setStars] = useState(0)
  const [comment, setComment] = useState('')
  const [busy, setBusy] = useState(false)

  // Kiểm tra dữ liệu feedback thực tế: phải có ratingStars mới là đã đánh giá
  const feedback = fb.data && typeof fb.data === 'object' && fb.data.ratingStars != null ? fb.data : null

  const submit = async (e) => {
    e.preventDefault()
    if (stars < 1) return toast('Vui lòng chọn số sao đánh giá.', 'err')
    setBusy(true)
    try {
      await sendFeedback(ticketRef, { ratingStars: stars, feedbackComment: comment.trim() || undefined })
      toast('Cảm ơn bạn đã gửi đánh giá chất lượng hỗ trợ!')
      await fb.reload()
    } catch (err) {
      toast(apiError(err, 'Không gửi được đánh giá.'), 'err')
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="card">
      <div className="ch"><h2>Đánh giá chất lượng hỗ trợ</h2></div>
      <div className="cb">
        <Async state={fb} notReadyText="Đang tải đánh giá...">
          {feedback ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <div aria-label={`${feedback.ratingStars} trên 5 sao`} style={{ fontSize: 24, color: '#f59e0b', letterSpacing: 4 }}>
                {'★'.repeat(feedback.ratingStars)}{'☆'.repeat(5 - feedback.ratingStars)}
              </div>
              {feedback.feedbackComment ? (
                <p style={{ marginTop: 6, overflowWrap: 'anywhere', color: '#cbd5e1', fontStyle: 'italic' }}>
                  "{feedback.feedbackComment}"
                </p>
              ) : (
                <p className="sm muted" style={{ marginTop: 4 }}>Bạn đã đánh giá {feedback.ratingStars}/5 sao.</p>
              )}
            </div>
          ) : (
            <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <p className="sm muted" style={{ margin: 0 }}>
                Vui lòng đánh giá mức độ hài lòng về chất lượng hỗ trợ của kỹ thuật viên:
              </p>
              <div role="radiogroup" aria-label="Số sao" style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                {[1, 2, 3, 4, 5].map((n) => (
                  <button
                    key={n}
                    type="button"
                    role="radio"
                    aria-checked={stars === n}
                    aria-label={`${n} sao`}
                    onClick={() => setStars(n)}
                    style={{
                      border: 0,
                      background: 'none',
                      fontSize: 32,
                      color: n <= stars ? '#f59e0b' : '#475569',
                      cursor: 'pointer',
                      padding: 2,
                      lineHeight: 1,
                      transition: 'transform 0.1s, color 0.15s',
                    }}
                    onMouseEnter={(e) => { e.currentTarget.style.transform = 'scale(1.2)' }}
                    onMouseLeave={(e) => { e.currentTarget.style.transform = 'scale(1)' }}
                  >
                    {n <= stars ? '★' : '☆'}
                  </button>
                ))}
                {stars > 0 && (
                  <span style={{ fontSize: 13, color: '#94a3b8', marginLeft: 8 }}>
                    ({stars}/5 sao)
                  </span>
                )}
              </div>
              <textarea
                className="ta"
                maxLength={LIMITS.feedbackComment}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Nhận xét thêm về mức độ hài lòng (không bắt buộc)..."
                rows={3}
              />
              <div>
                <button className="btn pri sm" type="submit" disabled={busy || stars === 0}>
                  {busy ? 'Đang gửi...' : 'Gửi đánh giá'}
                </button>
              </div>
            </form>
          )}
        </Async>
      </div>
    </section>
  )
}
