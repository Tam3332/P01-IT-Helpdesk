import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import { useAuth } from '@/context/AuthContext'
import { ROLE_HOME } from '@/config/constants'
import PrefsToggle from '@/components/PrefsToggle'
import { apiError } from '@/services/apiClient'
import { t } from '@/i18n'
import useErrorFeedback from '@/hooks/useErrorFeedback'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function messageFor(err) {
  const status = err?.response?.status
  if (!err?.response) return t('Không kết nối được máy chủ. Vui lòng kiểm tra mạng và thử lại.')
  if (status === 401) return t('Email hoặc mật khẩu không đúng.')
  if (status === 429) return apiError(err)
  if (status === 403) return apiError(err, t('Tài khoản đã bị vô hiệu hoá. Vui lòng liên hệ quản trị viên.'))
  return apiError(err, t('Đăng nhập không thành công. Vui lòng thử lại.'))
}

const STEPS = [
  { k: 'new', get label() { return t('Mới') } },
  { k: 'assigned', get label() { return t('Đã phân công') } },
  { k: 'doing', get label() { return t('Đang xử lý') } },
  { k: 'done', get label() { return t('Đã giải quyết|trạng thái') } },
]

// Hình minh họa: một ticket đi qua các trạng thái. Không có đoạn chữ giới thiệu.
function TicketJourney() {
  const [i, setI] = useState(0)
  useEffect(() => {
    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) { setI(3); return undefined }
    const id = setInterval(() => setI((v) => (v + 1) % (STEPS.length + 1)), 1800)
    return () => clearInterval(id)
  }, [])
  const at = Math.min(i, STEPS.length - 1)
  return (
    <div className="tj">
      <div className="tj-card" data-s={STEPS[at].k}>
        <div className="tj-top">
          <b>HD-2026-0017</b>
          <span className="tj-badge" key={STEPS[at].k}>{STEPS[at].label}</span>
        </div>
        <div className="tj-lines"><i /><i /><i /></div>
        <ol className="tj-track" style={{ '--p': at / (STEPS.length - 1) }}>
          {STEPS.map((s, n) => (
            <li key={s.k} className={n < at ? 'is-past' : n === at ? 'is-now' : ''}>
              <span className="tj-dot">{n < at || (n === at && at === 3) ? <Icon name="check" /> : null}</span>
              <em>{s.label}</em>
            </li>
          ))}
        </ol>
      </div>
      <div className="tj-ghost g1" /><div className="tj-ghost g2" />
    </div>
  )
}

export default function LoginPage() {
  const { login, notice } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [show, setShow] = useState(false)
  const [errors, setErrors] = useState({})
  const [formError, setFormError] = useState('')
  const [busy, setBusy] = useState(false)
  useErrorFeedback()

  const submit = async (e) => {
    e.preventDefault()
    if (busy) return
    const next = {}
    if (!email.trim()) next.email = t('Vui lòng nhập email.')
    else if (!EMAIL_RE.test(email.trim())) next.email = t('Email không đúng định dạng.')
    if (!password) next.password = t('Vui lòng nhập mật khẩu.')
    setErrors(next)
    setFormError('')
    if (Object.keys(next).length) return

    setBusy(true)
    try {
      const user = await login(email.trim(), password)
      const from = location.state?.from
      // Chỉ quay lại trang cũ nếu nó thuộc khu vực của vai trò này
      const home = ROLE_HOME[user.role]
      navigate(from && from.startsWith(home) ? from : home, { replace: true })
    } catch (err) {
      setFormError(err?.local ? err.message : messageFor(err))
      setBusy(false)
    }
  }

  return (
    <div className="login2">
      <PrefsToggle />
      <section className="lform">
        <form onSubmit={submit} noValidate>
          <div className="brand" style={{ padding: 0, border: 0 }}>
            <div className="logo">H</div>
            <div>
              <b>Helpdesk</b>
              <span>{t('Hệ thống yêu cầu hỗ trợ IT nội bộ')}</span>
            </div>
          </div>

          <div className="lhead">
            <h1>{t('Đăng nhập')}</h1>
            <p>{t('Dùng email công ty của bạn.')}</p>
          </div>

          {notice && !formError && (
            <div className="banner info" role="status">
              <Icon name="info" />
              <div className="grow">{notice}</div>
            </div>
          )}
          {formError && (
            <div className="banner bad" role="alert">
              <Icon name="alert" />
              <div className="grow">{formError}</div>
            </div>
          )}

          <div className={`field ${errors.email ? 'has-err' : ''}`}>
            <label htmlFor="l-email">Email</label>
            <input
              className="in"
              id="l-email"
              type="email"
              autoComplete="username"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder={t('ten@congty.vn')}
              aria-invalid={!!errors.email}
            />
            {errors.email && <span className="err">{errors.email}</span>}
          </div>

          <div className={`field ${errors.password ? 'has-err' : ''}`}>
            <label htmlFor="l-pw">{t('Mật khẩu')}</label>
            <div className="pw">
              <input
                className="in"
                id="l-pw"
                type={show ? 'text' : 'password'}
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder={t('Nhập mật khẩu')}
                aria-invalid={!!errors.password}
              />
              <button type="button" onClick={() => setShow((v) => !v)} aria-label={show ? t('Ẩn mật khẩu') : t('Hiện mật khẩu')}>
                <Icon name={show ? 'eyeoff' : 'eye'} />
              </button>
            </div>
            {errors.password && <span className="err">{errors.password}</span>}
          </div>

          <button className="btn pri block lgo" type="submit" disabled={busy}>
            {busy ? t('Đang đăng nhập...') : t('Đăng nhập')}
          </button>
          <p className="lnote">{t('Quên mật khẩu? Liên hệ quản trị viên để được cấp lại.')}</p>

        </form>
      </section>

      <aside className="lpanel" aria-hidden="true">
        <TicketJourney />
      </aside>
    </div>
  )
}
