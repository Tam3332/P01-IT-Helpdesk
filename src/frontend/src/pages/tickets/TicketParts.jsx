import { useRef, useState } from 'react'
import Icon from '@/components/Icon'
import { Async } from '@/components/StateViews'
import { validateFiles } from '@/components/FilePicker'
import { useToast } from '@/context/ToastContext'
import useAsync from '@/hooks/useAsync'
import { LIMITS, ALLOWED_EXT, STATUS, PRIORITY } from '@/config/domain'
import { apiError } from '@/services/apiClient'
import { addComment, downloadAttachment, getFeedback, listAttachments, listComments, listHistory, sendFeedback, uploadAttachment } from '@/services/ticketService'
import { fmtDateTime } from '@/utils/format'
import { avatarStyle, initials } from '@/utils/avatar'
import useUnsavedGuard from '@/hooks/useUnsavedGuard'
import { humanSize } from '@/utils/sla'
import { t } from '@/i18n'

const pendingApi = () => t('Backend chưa có API này. Mục sẽ hiện dữ liệu khi API được triển khai.')

export function CommentsCard({ ticketRef, nameOf, canComment, canInternal, closed, onChanged }) {
  const toast = useToast()
  const list = useAsync(() => listComments(ticketRef), [ticketRef])
  const [text, setText] = useState('')
  const [internal, setInternal] = useState(false)
  const [busy, setBusy] = useState(false)
  useUnsavedGuard(!!text.trim())

  const submit = async (e) => {
    e.preventDefault()
    const v = text.trim()
    if (!v) return toast(t('Vui lòng nhập nội dung bình luận.'), 'err')
    if (v.length > LIMITS.comment) return toast(t('Bình luận tối đa {n} ký tự.', { n: LIMITS.comment }), 'err')
    setBusy(true)
    try {
      await addComment(ticketRef, { commentText: v, isInternal: canInternal && internal })
      setText('')
      setInternal(false)
      toast(t('Đã gửi bình luận.'))
      await list.reload()
      onChanged?.()
    } catch (err) {
      toast(apiError(err, t('Không gửi được bình luận.')), 'err')
    } finally {
      setBusy(false)
    }
  }

  const items = list.data || []
  return (
    <section className="card">
      <div className="ch"><h2>{t('Bình luận ({n})', { n: items.length })}</h2></div>
      <div className="cb">
        <Async state={list} notReadyText={pendingApi()}>
          {items.length ? (
            items.map((c) => {
              const nm = nameOf(c.authorId, c.authorName)
              return (
                <div className="cm" key={c.id}>
                  <div className="av" style={avatarStyle(nm)}>{initials(nm)}</div>
                  <div className="bub">
                    <div className="meta">
                      <b>{nm}</b>{c.authorRole ? ` · ${c.authorRole}` : ''} · {fmtDateTime(c.createdAt)}
                      {c.isInternal && <span className="badge b-x" style={{ marginLeft: 8 }}>{t('Nội bộ')}</span>}
                    </div>
                    <span style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{c.commentText}</span>
                  </div>
                </div>
              )
            })
          ) : (
            <p className="muted">{t('Chưa có bình luận nào.')}</p>
          )}
        </Async>
        {canComment ? (
          <form onSubmit={submit} style={{ marginTop: 18, display: 'flex', flexDirection: 'column', gap: 8 }}>
            <label className="lbl" htmlFor="cmtext">{t('Thêm bình luận')}</label>
            <textarea className="ta" id="cmtext" style={{ minHeight: 76 }} maxLength={LIMITS.comment} value={text} onChange={(e) => setText(e.target.value)} placeholder={t('Nhập nội dung trao đổi')} />
            {canInternal && (
              <label className="chk">
                <input type="checkbox" checked={internal} onChange={(e) => setInternal(e.target.checked)} /> {t('Ghi chú nội bộ (nhân viên không thấy)')}
              </label>
            )}
            <div>
              <button className="btn pri sm" type="submit" disabled={busy} aria-busy={busy}><Icon name="send" />{busy ? t('Đang gửi...') : t('Gửi bình luận')}</button>
            </div>
          </form>
        ) : (
          <p className="sm muted" style={{ marginTop: 16 }}>{closed ? t('Ticket đã đóng nên không thể bình luận thêm.') : t('Vai trò hiện tại không được bình luận trên ticket này.')}</p>
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
        toast(`"${f.name}": ${apiError(err, t('tải lên thất bại'))}`, 'err')
      }
    }
    setBusy(false)
    if (added) {
      toast(t('Đã tải lên {n} tệp.', { n: added }))
      await list.reload()
      onChanged?.()
    }
  }
  const download = async (a) => {
    try {
      await downloadAttachment(ticketRef, a)
    } catch (err) {
      toast(apiError(err, t('Không tải được tệp.')), 'err')
    }
  }

  return (
    <section className="card">
      <div className="ch">
        <h2>{t('Tệp đính kèm ({n})', { n: items.length })}</h2>
        {canUpload && (
          <>
            <button type="button" className="btn sm" onClick={() => input.current?.click()} disabled={busy}><Icon name="upload" />{busy ? t('Đang tải...') : t('Tải tệp lên')}</button>
            <input type="file" multiple hidden ref={input} onChange={pick} />
          </>
        )}
      </div>
      <div className="cb">
        {canUpload && <p className="sm muted" style={{ marginBottom: 12 }}>{t('Tối đa {n} tệp, mỗi tệp ≤ 10 MB ({ext}).', { n: LIMITS.maxFiles, ext: ALLOWED_EXT.join(', ') })}</p>}
        <Async state={list} notReadyText={pendingApi()}>
          {items.length ? (
            <div style={{ display: 'grid', gap: 8 }}>
              {items.map((a) => (
                <div className="att" key={a.id}>
                  <Icon name="clip" />
                  <span>{a.fileName}</span>
                  <span className="sm muted" style={{ flex: 'none' }}>{humanSize(a.fileSize)}</span>
                  <button type="button" className="btn ghost sm" onClick={() => download(a)} aria-label={t('Tải {name}', { name: a.fileName })}>{t('Tải về')}</button>
                </div>
              ))}
            </div>
          ) : (
            <p className="muted">{t('Ticket này không có tệp đính kèm.')}</p>
          )}
        </Async>
      </div>
    </section>
  )
}

const actionLabel = (a) => ({
  CREATE: t('Tạo mới ticket'),
  ASSIGN: t('Phân công xử lý'),
  REASSIGN: t('Phân công lại'),
  STATUS_CHANGE: t('Đổi trạng thái'),
  COMMENT: t('Bình luận trao đổi'),
  ATTACH_FILE: t('Đính kèm tệp tin'),
  CLOSE: t('Xác nhận đóng ticket'),
  REOPEN: t('Mở lại ticket'),
  SLA_BREACH: t('Hệ thống cảnh báo vi phạm SLA'),
}[a])

const fieldLabel = (f) => ({
  status: t('Trạng thái'),
  priority: t('Mức ưu tiên'),
  assignee: t('Người xử lý'),
  current_assignee_id: t('Người xử lý'),
  currentAssigneeId: t('Người xử lý'),
  is_sla_breached: t('Vi phạm cam kết SLA'),
}[f])

function historyText(h, nameOf) {
  if (h.action === 'SLA_BREACH' || h.fieldChanged === 'is_sla_breached') {
    return t('Hệ thống cảnh báo vi phạm SLA')
  }

  const actionName = actionLabel(h.action) || h.action || t('Cập nhật')

  // Trường hợp bình luận hoặc đính kèm: không in ID kỹ thuật
  if (h.action === 'COMMENT' || h.fieldChanged === 'comment_id') {
    return t('Bình luận trao đổi')
  }
  if (h.action === 'ATTACH_FILE' || h.fieldChanged === 'attachment_id') {
    return t('Đính kèm tệp tin')
  }
  if (h.action === 'CREATE') {
    return t('Tạo mới ticket')
  }
  if (h.action === 'CLOSE') {
    return t('Xác nhận đóng ticket')
  }
  if (h.action === 'FEEDBACK' || h.fieldChanged === 'feedback') {
    return t('Đánh giá chất lượng hỗ trợ ({v})', { v: h.newValue || t('Hoàn tất') })
  }
  if (h.action === 'REOPEN' || h.action === 'REOPEN_REQUEST') {
    return t('Yêu cầu mở lại ticket')
  }
  if (h.action === 'REOPEN_APPROVE') {
    return t('Đồng ý mở lại ticket')
  }
  if (h.action === 'REOPEN_REJECT') {
    return t('Từ chối yêu cầu mở lại')
  }

  // Trường hợp phân công người xử lý: map ID thành tên người dùng gọn gàng
  if (h.fieldChanged === 'current_assignee_id' || h.fieldChanged === 'currentAssigneeId') {
    const oldName = h.oldValue && nameOf ? nameOf(Number(h.oldValue), h.oldValueLabel) : (h.oldValue ? t('Người dùng #{id}', { id: h.oldValue }) : null)
    const newName = h.newValue && nameOf ? nameOf(Number(h.newValue), h.newValueLabel) : (h.newValue ? t('Người dùng #{id}', { id: h.newValue }) : t('Chưa phân công'))
    if (!oldName || h.action === 'ASSIGN') {
      return t('Phân công cho {name}', { name: newName })
    }
    return t('Phân công lại: {old} → {new}', { old: oldName, new: newName })
  }

  // Trường hợp đổi trạng thái: hiển thị label tiếng Việt
  if (h.fieldChanged === 'status') {
    if (h.oldValue === 'REOPEN_PENDING' && h.newValue === 'RESOLVED') {
      return t('Từ chối yêu cầu mở lại')
    }
    if (h.oldValue === 'REOPEN_PENDING' && h.newValue === 'REOPENED') {
      return t('Đồng ý mở lại ticket')
    }
    if (h.oldValue === 'RESOLVED' && h.newValue === 'REOPEN_PENDING') {
      return t('Yêu cầu mở lại ticket')
    }
    const oldSt = t(STATUS[h.oldValue]?.label || h.oldValue || '—')
    const newSt = t(STATUS[h.newValue]?.label || h.newValue || '—')
    return `${t('Đổi trạng thái')}: ${oldSt} → ${newSt}`
  }

  // Trường hợp đổi mức ưu tiên: hiển thị label tiếng Việt
  if (h.fieldChanged === 'priority') {
    const oldPr = t(PRIORITY[h.oldValue]?.label || h.oldValue || '—')
    const newPr = t(PRIORITY[h.newValue]?.label || h.newValue || '—')
    return `${t('Đổi mức ưu tiên')}: ${oldPr} → ${newPr}`
  }

  const field = fieldLabel(h.fieldChanged) || h.fieldChanged
  const ch = field ? ` · ${field}: ${h.oldValue ?? '—'} → ${h.newValue ?? '—'}` : ''
  return actionName + ch
}

export function HistoryCard({ ticketRef, nameOf, refreshKey }) {
  const list = useAsync(() => listHistory(ticketRef), [ticketRef, refreshKey])
  const items = (list.data || []).slice().sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp))
  return (
    <section className="card">
      <div className="ch"><h2>{t('Lịch sử xử lý')}</h2></div>
      <div className="cb">
        <Async state={list} notReadyText={pendingApi()}>
          {items.length ? (
            <ul className="tl">
              {items.map((h) => (
                <li key={h.id}>
                  <i />
                  <div>
                    <div style={{ fontWeight: 600 }}>{historyText(h, nameOf)}</div>
                    {h.reasonOrNote && h.reasonOrNote !== 'Tạo mới ticket' && h.action !== 'CREATE' && (
                      <div className="sm muted" style={{ overflowWrap: 'anywhere' }}>{h.reasonOrNote}</div>
                    )}
                    {h.action === 'SLA_BREACH' || h.fieldChanged === 'is_sla_breached' ? (
                      <div className="sm muted who">
                        <span className="av xs" style={{ background: 'var(--bad-bg)', color: 'var(--bad)', display: 'inline-grid', placeItems: 'center', fontWeight: 700 }}>⚙</span>
                        {t('Hệ thống (Tự động)')} · {fmtDateTime(h.timestamp)}
                      </div>
                    ) : (
                      <div className="sm muted who"><span className="av xs" style={avatarStyle(nameOf(h.actorId, h.actorName))}>{initials(nameOf(h.actorId, h.actorName))}</span>{nameOf(h.actorId, h.actorName)} · {fmtDateTime(h.timestamp)}</div>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <p className="muted">{t('Chưa có lịch sử.')}</p>
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
    if (stars < 1) return toast(t('Vui lòng chọn số sao đánh giá.'), 'err')
    setBusy(true)
    try {
      await sendFeedback(ticketRef, { ratingStars: stars, feedbackComment: comment.trim() || undefined })
      toast(t('Cảm ơn bạn đã gửi đánh giá chất lượng hỗ trợ!'))
      await fb.reload()
    } catch (err) {
      toast(apiError(err, t('Không gửi được đánh giá.')), 'err')
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="card">
      <div className="ch"><h2>{t('Đánh giá chất lượng hỗ trợ')}</h2></div>
      <div className="cb">
        <Async state={fb} notReadyText={t('Đang tải đánh giá...')}>
          {feedback ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <div aria-label={t('{n} trên 5 sao', { n: feedback.ratingStars })} style={{ fontSize: 24, color: '#f59e0b', letterSpacing: 4 }}>
                {'★'.repeat(feedback.ratingStars)}{'☆'.repeat(5 - feedback.ratingStars)}
              </div>
              {feedback.feedbackComment ? (
                <p style={{ marginTop: 6, overflowWrap: 'anywhere', color: '#cbd5e1', fontStyle: 'italic' }}>
                  "{feedback.feedbackComment}"
                </p>
              ) : (
                <p className="sm muted" style={{ marginTop: 4 }}>{t('Bạn đã đánh giá {n}/5 sao.', { n: feedback.ratingStars })}</p>
              )}
            </div>
          ) : (
            <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <p className="sm muted" style={{ margin: 0 }}>
                {t('Vui lòng đánh giá mức độ hài lòng về chất lượng hỗ trợ của kỹ thuật viên:')}
              </p>
              <div role="radiogroup" aria-label={t('Số sao')} style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                {[1, 2, 3, 4, 5].map((n) => (
                  <button
                    key={n}
                    type="button"
                    role="radio"
                    aria-checked={stars === n}
                    aria-label={t('{n} sao', { n })}
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
                    {t('({n}/5 sao)', { n: stars })}
                  </span>
                )}
              </div>
              <textarea
                className="ta"
                maxLength={LIMITS.feedbackComment}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder={t('Nhận xét thêm về mức độ hài lòng (không bắt buộc)...')}
                rows={3}
              />
              <div>
                <button className="btn pri sm" type="submit" disabled={busy || stars === 0} aria-busy={busy}>
                  {busy ? t('Đang gửi...') : t('Gửi đánh giá')}
                </button>
              </div>
            </form>
          )}
        </Async>
      </div>
    </section>
  )
}
