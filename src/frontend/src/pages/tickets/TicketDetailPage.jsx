import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Icon from '@/components/Icon'
import { PriorityBadge, SlaBadge, StatusBadge } from '@/components/Badges'
import { ErrorState, Spinner } from '@/components/StateViews'
import { useAuth } from '@/context/AuthContext'
import { useToast } from '@/context/ToastContext'
import useAsync from '@/hooks/useAsync'
import { LIFECYCLE, LIMITS, OPEN_STATUSES, PRIORITY, statusLabel } from '@/config/domain'
import { ROLES, ROLE_HOME } from '@/config/constants'
import { getTicket } from '@/services/ticketService'
import { listReopenRequests } from '@/services/referenceService'
import { apiError } from '@/services/apiClient'
import { fmtDateTime } from '@/utils/format'
import { avatarStyle, initials } from '@/utils/avatar'
import { dur, slaOf } from '@/utils/sla'
import { AttachmentsCard, CommentsCard, FeedbackCard, HistoryCard } from './TicketParts'
import { t as tr } from '@/i18n'
import { AssignDialog, CloseDialog, ReopenDialog, ResolveDialog, StartDialog } from './TicketActions'

// Vị trí trên thanh vòng đời: REOPEN_PENDING đứng ở "Đã giải quyết", REOPENED quay lại "Đã phân công"
const stepIndex = (s) => (s === 'REOPEN_PENDING' ? 3 : s === 'REOPENED' ? 1 : LIFECYCLE.indexOf(s))

export default function TicketDetailPage() {
  const { ticketId } = useParams()
  const ref_ = decodeURIComponent(ticketId)
  const { user } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const role = user.role
  const base = `${ROLE_HOME[role]}/tickets`
  const detail = useAsync(() => getTicket(ref_), [ref_])
  const [dialog, setDialog] = useState(null)
  const [copied, setCopied] = useState(false)
  const [tick, setTick] = useState(0) // tăng để làm mới lịch sử sau mỗi thay đổi
  const t = detail.data

  const back = (
    <div>
      <button type="button" className="btn ghost sm" onClick={() => navigate(base)}><Icon name="back" />{tr('Quay lại danh sách')}</button>
    </div>
  )

  if (detail.loading && !t) return <>{back}<Spinner /></>
  if (detail.error && !t) {
    // 404 có thể là ticket không tồn tại hoặc cơ chế chống IDOR (backend che sự tồn tại)
    const msg = detail.status === 404 ? tr('Không tìm thấy ticket hoặc bạn không có quyền xem ticket này.') : detail.error
    return <>{back}<section className="card"><ErrorState message={msg} status={detail.status} onRetry={detail.status === 404 ? undefined : detail.reload} /></section></>
  }

  const afterAction = async (msg) => {
    toast(msg)
    setTick((n) => n + 1)
    await detail.reload()
  }
  const open = (name) => setDialog(name)
  const closeDlg = () => setDialog(null)

  const nameOf = (id, given) => {
    if (given) return given
    if (id != null && id === t.requesterId) return t.requesterName || tr('Người dùng #{id}', { id })
    const aId = t.currentAssigneeId ?? t.assigneeId
    if (id != null && id === aId) return t.assigneeName || tr('Người dùng #{id}', { id })
    return id != null ? tr('Người dùng #{id}', { id }) : '—'
  }

  const mine = (t.currentAssigneeId ?? t.assigneeId) === user.id // ticket đang giao cho người đăng nhập
  const s = slaOf(t)
  const si = stepIndex(t.status)
  const isOpen = OPEN_STATUSES.includes(t.status)
  const resolvedMs = t.resolvedAt ? Date.now() - new Date(t.resolvedAt).getTime() : null
  const withinReopen = resolvedMs != null && resolvedMs <= LIMITS.reopenWindowHours * 3600e3

  let actions = null
  let note = null
  if (role === ROLES.EMPLOYEE) {
    if (t.status === 'RESOLVED') {
      actions = (
        <>
          <button type="button" className="btn ok" onClick={() => open('close')}><Icon name="check" />{tr('Yêu cầu đóng ticket')}</button>
          {withinReopen && <button type="button" className="btn" onClick={() => open('reopen')}><Icon name="refresh" />{tr('Yêu cầu mở lại')}</button>}
        </>
      )
      if (!withinReopen) note = <div className="banner"><Icon name="info" /><div className="grow">{tr('Đã quá 7 ngày kể từ khi ticket được giải quyết nên không thể yêu cầu mở lại. Bạn có thể đóng ticket hoặc tạo ticket mới.')}</div></div>
    }
    if (t.status === 'REOPEN_PENDING') note = <div className="banner info"><Icon name="clock" /><div className="grow">{tr('Yêu cầu mở lại của bạn đang chờ IT Manager xem xét.')}</div></div>
  }
  if (role === ROLES.STAFF && mine) {
    if (t.status === 'ASSIGNED' || t.status === 'REOPENED') actions = <button type="button" className="btn pri" onClick={() => open('start')}>{tr('Bắt đầu xử lý')}</button>
    if (t.status === 'IN_PROGRESS') actions = <button type="button" className="btn ok" onClick={() => open('resolve')}><Icon name="check" />{tr('Hoàn tất xử lý')}</button>
  }
  if (role === ROLES.MANAGER) {
    if (isOpen) actions = <button type="button" className="btn pri" onClick={() => open('assign')}><Icon name="users" />{t.currentAssigneeId ?? t.assigneeId ? tr('Phân công lại') : tr('Phân công')}</button>
    if (t.status === 'RESOLVED') actions = <button type="button" className="btn ok" onClick={() => open('close')}><Icon name="check" />{tr('Đóng ticket')}</button>
    if (t.status === 'REOPEN_PENDING') {
      actions = (
        <button
          type="button" className="btn pri"
          onClick={async () => {
            try {
              const list = await listReopenRequests('PENDING')
              const r = list.find((x) => x.ticketId === t.id || x.ticketNumber === t.ticketNumber)
              if (!r) return toast(tr('Không tìm thấy yêu cầu mở lại đang chờ của ticket này.'), 'err')
              navigate(`/manager/reopen-requests/${r.id}`)
            } catch (e) {
              toast(apiError(e), 'err')
            }
          }}
        >
          <Icon name="refresh" />{tr('Xem xét yêu cầu mở lại')}
        </button>
      )
    }
  }

  if (role === ROLES.ADMIN) note = <div className="banner info"><Icon name="shield" /><div className="grow">{tr('Quản trị viên chỉ xem ticket (chế độ chỉ đọc), không thao tác được.')}</div></div>

  const involved = role === ROLES.EMPLOYEE || role === ROLES.STAFF || role === ROLES.MANAGER
  const canWrite = involved && (role !== ROLES.STAFF || mine)
  const canComment = t.status !== 'CLOSED' && canWrite
  const canUpload = t.status !== 'CLOSED' && canWrite
  const assigneeLabel = t.assigneeName || (t.currentAssigneeId ?? t.assigneeId ? tr('Người dùng #{id}', { id: t.currentAssigneeId ?? t.assigneeId }) : tr('Chưa phân công'))
  const copyId = async () => {
    try {
      await navigator.clipboard.writeText(t.ticketNumber)
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch {
      toast(tr('Không sao chép được.'), 'err')
    }
  }
  const person = (name) => <span className="who"><span className="av xs" style={avatarStyle(name)}>{initials(name)}</span>{name}</span>
  const dialogProps = { ticket: t, ref_, onClose: closeDlg, onDone: afterAction }

  return (
    <>
      {back}
      <div className="stickbar">
        <span className="num sid">{t.ticketNumber}</span>
        <button type="button" className={`btn ghost sm${copied ? ' copied' : ''}`} onClick={copyId} aria-label={tr('Sao chép mã ticket')}>
          {copied && <Icon name="check" />}{copied ? tr('Đã chép') : tr('Sao chép mã')}
        </button>
        {actions && <div className="acts">{actions}</div>}
      </div>
      <div className="phead">
        <div>
          {t.reopenCount ? <div className="sm muted" style={{ fontWeight: 600 }}>{tr('Đã mở lại {n} lần', { n: t.reopenCount })}</div> : null}
          <h1>{t.title}</h1>
          <div style={{ display: 'flex', gap: 8, marginTop: 8, flexWrap: 'wrap' }}>
            <StatusBadge status={t.status} /><PriorityBadge priority={t.priority} /><SlaBadge ticket={t} />
          </div>
        </div>
      </div>
      {note}
      <section className="card">
        <div className="cb">
          <ol className="steps" aria-label={tr('Vòng đời ticket')}>
            {LIFECYCLE.map((k, i) => (
              <li key={k} className={i < si ? 'done' : i === si ? 'cur' : ''} aria-current={i === si ? 'step' : undefined}>
                <b>{i < si ? <Icon name="check" /> : i + 1}</b>{statusLabel(k)}
              </li>
            ))}
          </ol>
        </div>
      </section>

      <div className="dgrid">
        <div className="stack">
          <section className="card">
            <div className="ch"><h2>{tr('Mô tả')}</h2></div>
            <div className="cb">
              <p style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{t.description}</p>
              {t.resolutionNote && <div className="banner ok" style={{ marginTop: 16 }}><Icon name="check" /><div className="grow"><b>{tr('Giải pháp:')}</b> {t.resolutionNote}</div></div>}
            </div>
          </section>
          <CommentsCard ticketRef={ref_} nameOf={nameOf} canComment={canComment} canInternal={role !== ROLES.EMPLOYEE} closed={t.status === 'CLOSED'} onChanged={() => setTick((n) => n + 1)} />
          <AttachmentsCard ticketRef={ref_} canUpload={canUpload} onChanged={() => setTick((n) => n + 1)} />
          {role === ROLES.EMPLOYEE && t.requesterId === user.id && t.status === 'CLOSED' && <FeedbackCard ticketRef={ref_} />}
        </div>
        <div className="stack">
          <section className="card">
            <div className="ch"><h2>{tr('Thông tin')}</h2></div>
            <div className="cb">
              <dl className="dl">
                <dt>{tr('Người yêu cầu')}</dt><dd>{person(t.requesterName || nameOf(t.requesterId))}{t.requesterDepartment ? ` · ${t.requesterDepartment}` : ''}</dd>
                <dt>{tr('Người xử lý')}</dt><dd>{t.assigneeName || (t.currentAssigneeId ?? t.assigneeId) ? person(assigneeLabel) : assigneeLabel}</dd>
                <dt>{tr('Danh mục')}</dt><dd>{t.categoryName || (t.categoryId != null ? `#${t.categoryId}` : '—')}</dd>
                <dt>{tr('Mức ảnh hưởng')}</dt><dd>{tr(PRIORITY[t.impactLevel]?.label || t.impactLevel || '—')}</dd>
                <dt>{tr('Ngày tạo')}</dt><dd className="num">{fmtDateTime(t.createdAt)}</dd>
                <dt>{tr('Hạn xử lý')}</dt>
                <dd className="num">
                  {fmtDateTime(t.resolutionDeadline)}
                  <div className="sm muted" style={s.key === 'BREACHED' ? { color: 'var(--bad)', fontWeight: 600 } : undefined}>
                    {s.done ? (s.key === 'DONE' ? tr('Hoàn tất đúng hạn') : tr('Hoàn tất trễ hạn')) : s.left > 0 ? tr('Còn {d}', { d: dur(s.left) }) : tr('Quá hạn {d}', { d: dur(s.left) })}
                  </div>
                </dd>
              </dl>
            </div>
          </section>
          <HistoryCard ticketRef={ref_} nameOf={nameOf} refreshKey={tick} />
        </div>
      </div>

      {dialog === 'start' && <StartDialog {...dialogProps} />}
      {dialog === 'resolve' && <ResolveDialog {...dialogProps} />}
      {dialog === 'close' && <CloseDialog {...dialogProps} />}
      {dialog === 'reopen' && <ReopenDialog {...dialogProps} />}
      {dialog === 'assign' && <AssignDialog {...dialogProps} />}
    </>
  )
}
