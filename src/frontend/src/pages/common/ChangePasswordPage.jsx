import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import { Field } from '@/components/Modal'
import { useAuth } from '@/context/AuthContext'
import { useToast } from '@/context/ToastContext'
import { LIMITS } from '@/config/domain'
import { PASSWORD_LABEL, profilePath } from '@/config/nav'
import { apiError, httpStatus } from '@/services/apiClient'
import { changePassword } from '@/services/authService'

const EMPTY = { current: '', next: '', confirm: '' }

function messageFor(err) {
  const status = httpStatus(err)
  if (status === 404 || status === 405) return 'Máy chủ chưa hỗ trợ đổi mật khẩu. Vui lòng liên hệ quản trị viên.'
  if (status === 401) return 'Mật khẩu hiện tại không đúng.'
  return apiError(err, 'Không đổi được mật khẩu. Vui lòng thử lại.')
}

// Gợi ý độ mạnh (chỉ để tham khảo, không chặn gửi): 0..4
function strength(pw) {
  if (!pw) return 0
  let s = 0
  if (pw.length >= LIMITS.passwordMin) s++
  if (pw.length >= 12) s++
  if (/[a-z]/.test(pw) && /[A-Z]/.test(pw)) s++
  if (/\d/.test(pw) && /[^A-Za-z0-9]/.test(pw)) s++
  return s
}
const LEVELS = ['', 'Yếu', 'Trung bình', 'Khá', 'Mạnh']

function PasswordInput({ id, label, value, onChange, auto, error, hint, children }) {
  const [show, setShow] = useState(false)
  return (
    <Field label={label} required htmlFor={id} error={error} hint={hint}>
      <div className="pw">
        <input className="in" id={id} type={show ? 'text' : 'password'} autoComplete={auto} value={value} onChange={onChange} maxLength={LIMITS.passwordMax} aria-invalid={!!error} />
        <button type="button" onClick={() => setShow((s) => !s)} aria-label={show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}>
          <Icon name={show ? 'eyeoff' : 'eye'} />
        </button>
      </div>
      {children}
    </Field>
  )
}

// Đổi mật khẩu: POST /auth/change-password (tạm, chờ backend chốt)
export default function ChangePasswordPage() {
  const { user, reloadUser } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [form, setForm] = useState(EMPTY)
  const [errors, setErrors] = useState({})
  const [formError, setFormError] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  const rules = [
    [`Ít nhất ${LIMITS.passwordMin} ký tự`, form.next.length >= LIMITS.passwordMin],
    ['Khác mật khẩu hiện tại', !!form.next && form.next !== form.current],
    ['Nhập lại khớp mật khẩu mới', !!form.confirm && form.confirm === form.next],
  ]
  const level = strength(form.next)

  const validate = () => {
    const e = {}
    if (!form.current) e.current = 'Vui lòng nhập mật khẩu hiện tại.'
    if (!form.next) e.next = 'Vui lòng nhập mật khẩu mới.'
    else if (form.next.length < LIMITS.passwordMin) e.next = `Mật khẩu mới cần ít nhất ${LIMITS.passwordMin} ký tự.`
    else if (form.next === form.current) e.next = 'Mật khẩu mới phải khác mật khẩu hiện tại.'
    if (!form.confirm) e.confirm = 'Vui lòng nhập lại mật khẩu mới.'
    else if (form.next && form.confirm !== form.next) e.confirm = 'Mật khẩu nhập lại không khớp.'
    return e
  }

  const submit = async (ev) => {
    ev.preventDefault()
    if (busy) return
    const e = validate()
    setErrors(e)
    setFormError('')
    if (Object.keys(e).length) return
    setBusy(true)
    try {
      await changePassword(form.current, form.next)
      toast('Đã đổi mật khẩu.')
      try {
        await reloadUser() // gỡ yêu cầu bắt buộc đổi mật khẩu nếu có
      } catch {
        /* không ảnh hưởng kết quả đổi mật khẩu */
      }
      navigate(profilePath(user.role), { replace: true })
    } catch (err) {
      setFormError(messageFor(err))
      setBusy(false)
    }
  }

  return (
    <div className="pw-page">
      <PageHeader title={PASSWORD_LABEL} subtitle="Chọn mật khẩu mới cho tài khoản của bạn." />
      {user.mustChangePassword && (
        <div className="banner" role="alert">
          <Icon name="alert" />
          <div className="grow">Quản trị viên vừa cấp lại mật khẩu cho bạn. Hãy đặt mật khẩu mới để tiếp tục sử dụng hệ thống.</div>
        </div>
      )}
      <form className="card pw-card" onSubmit={submit} noValidate>
        <div className="pw-head">
          <span className="pf-ico lg"><Icon name="lock" /></span>
          <div>
            <b>{user.fullName}</b>
            <span className="muted sm">{user.email}</span>
          </div>
        </div>
        {formError && (
          <div className="banner bad" role="alert">
            <Icon name="alert" />
            <div className="grow">{formError}</div>
          </div>
        )}
        <PasswordInput id="pf-current" label="Mật khẩu hiện tại" auto="current-password" value={form.current} onChange={set('current')} error={errors.current} />
        <hr className="pw-sep" />
        <PasswordInput id="pf-next" label="Mật khẩu mới" auto="new-password" value={form.next} onChange={set('next')} error={errors.next}>
          <div className="pw-meter" aria-live="polite">
            <div className={`pw-segs l${level}`}>
              <i /><i /><i /><i />
            </div>
            <span className="sm muted">{form.next ? `Độ mạnh: ${LEVELS[level] || 'Yếu'}` : 'Mật khẩu dài và đa dạng ký tự sẽ khó đoán hơn.'}</span>
          </div>
        </PasswordInput>
        <PasswordInput id="pf-confirm" label="Nhập lại mật khẩu mới" auto="new-password" value={form.confirm} onChange={set('confirm')} error={errors.confirm} />
        <ul className="pw-rules">
          {rules.map(([text, ok]) => (
            <li key={text} className={ok ? 'ok' : ''}>
              <Icon name={ok ? 'check' : 'info'} />
              {text}
            </li>
          ))}
        </ul>
        <div className="pw-acts">
          {!user.mustChangePassword && (
            <button type="button" className="btn" onClick={() => navigate(profilePath(user.role))} disabled={busy}>
              Hủy
            </button>
          )}
          <button className="btn pri" type="submit" disabled={busy}>
            {busy ? 'Đang xử lý...' : 'Lưu mật khẩu mới'}
          </button>
        </div>
      </form>
    </div>
  )
}
