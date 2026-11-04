import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import { useAuth } from '@/context/AuthContext'
import { ROLE_HOME } from '@/config/constants'
import { apiError } from '@/services/apiClient'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function messageFor(err) {
  const status = err?.response?.status
  if (!err?.response) return 'Không kết nối được máy chủ. Vui lòng kiểm tra mạng và thử lại.'
  if (status === 401) return 'Email hoặc mật khẩu không đúng.'
  if (status === 429) return 'Bạn đã thử quá nhiều lần hoặc tài khoản đang bị khoá tạm thời. Vui lòng thử lại sau 15 phút.'
  if (status === 403) return apiError(err, 'Tài khoản đã bị vô hiệu hoá. Vui lòng liên hệ quản trị viên.')
  return apiError(err, 'Đăng nhập không thành công. Vui lòng thử lại.')
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

  const submit = async (e) => {
    e.preventDefault()
    if (busy) return
    const next = {}
    if (!email.trim()) next.email = 'Vui lòng nhập email.'
    else if (!EMAIL_RE.test(email.trim())) next.email = 'Email không đúng định dạng.'
    if (!password) next.password = 'Vui lòng nhập mật khẩu.'
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
      setFormError(err?.message && !err.response && err.message.startsWith('Tài khoản') ? err.message : messageFor(err))
      setBusy(false)
    }
  }

  return (
    <div className="login">
      <form className="card lcard" onSubmit={submit} noValidate>
        <div className="brand" style={{ padding: 0, border: 0 }}>
          <div className="logo">H</div>
          <div>
            <b>Helpdesk</b>
            <span>Hệ thống yêu cầu hỗ trợ IT nội bộ</span>
          </div>
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
            placeholder="ten@congty.vn"
            aria-invalid={!!errors.email}
          />
          {errors.email && <span className="err">{errors.email}</span>}
        </div>

        <div className={`field ${errors.password ? 'has-err' : ''}`}>
          <label htmlFor="l-pw">Mật khẩu</label>
          <div className="pw">
            <input
              className="in"
              id="l-pw"
              type={show ? 'text' : 'password'}
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Nhập mật khẩu"
              aria-invalid={!!errors.password}
            />
            <button type="button" onClick={() => setShow((s) => !s)} aria-label={show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}>
              <Icon name={show ? 'eyeoff' : 'eye'} />
            </button>
          </div>
          {errors.password && <span className="err">{errors.password}</span>}
        </div>

        <button className="btn pri block" type="submit" disabled={busy}>
          {busy ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>
      </form>
    </div>
  )
}
