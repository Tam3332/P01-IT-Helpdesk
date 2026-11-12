import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import { RoleBadge } from '@/components/Badges'
import { useAuth } from '@/context/AuthContext'
import { ROLE_LABEL, STORAGE_KEYS } from '@/config/constants'
import { passwordPath } from '@/config/nav'
import { fmtDateTime, initials } from '@/utils/format'
import { getTokenExpiryMs } from '@/utils/jwt'

const SESSION_MS = 15 * 60 * 1000 // access token sống 15 phút (ADR-02)

// Thời gian còn lại của phiên, cập nhật mỗi 15 giây
function useSessionLeft() {
  const exp = getTokenExpiryMs(sessionStorage.getItem(STORAGE_KEYS.token) || '')
  const [now, setNow] = useState(Date.now())
  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), 15000)
    return () => clearInterval(t)
  }, [])
  return exp ? { exp, left: Math.max(0, exp - now) } : null
}

const leftText = (ms) => {
  const m = Math.ceil(ms / 60000)
  return m <= 1 ? 'Còn dưới 1 phút' : `Còn ${m} phút`
}

// Hồ sơ cá nhân dùng chung cho 4 vai trò. Dữ liệu lấy từ /auth/me, chỉ đọc.
export default function ProfilePage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const session = useSessionLeft()
  const role = ROLE_LABEL[user.role] || user.role
  const locked = user.isActive === false

  const rows = [
    ['user', 'Họ và tên', user.fullName],
    ['user', 'Tên đăng nhập', user.username || '—'],
    ['mail', 'Email', user.email],
    ['building', 'Phòng ban', user.department || '—'],
    ['shield', 'Vai trò', role],
  ]

  return (
    <div className="pf">
      <section className="pf-hero card">
        <div className="pf-av" aria-hidden="true">{initials(user.fullName)}</div>
        <div className="pf-id">
          <h1>{user.fullName}</h1>
          <p>{user.email}</p>
          <div className="pf-tags">
            <RoleBadge label={role} />
            <span className={`badge ${locked ? 'b-x' : 'b-Resolved'}`}>{locked ? 'Đã bị khóa' : 'Đang hoạt động'}</span>
          </div>
        </div>
        <button type="button" className="btn pri" onClick={() => navigate(passwordPath(user.role))}>
          <Icon name="lock" />
          Đổi mật khẩu
        </button>
      </section>

      {user.mustChangePassword && (
        <div className="banner" role="alert">
          <Icon name="alert" />
          <div className="grow">Quản trị viên vừa cấp lại mật khẩu cho bạn. Hãy đặt mật khẩu mới để tiếp tục sử dụng hệ thống.</div>
          <button type="button" className="btn sm" onClick={() => navigate(passwordPath(user.role))}>Đặt mật khẩu mới</button>
        </div>
      )}

      <div className="pf-cols">
        <section className="card">
          <div className="ch">
            <h2>Thông tin tài khoản</h2>
          </div>
          <ul className="pf-rows">
            {rows.map(([icon, label, value]) => (
              <li key={label}>
                <span className="pf-ico"><Icon name={icon} /></span>
                <span className="pf-k">{label}</span>
                <span className="pf-v">{value}</span>
              </li>
            ))}
          </ul>
          <p className="pf-note">Thông tin này do quản trị viên quản lý. Cần chỉnh sửa, hãy liên hệ quản trị viên.</p>
        </section>

        <section className="card">
          <div className="ch">
            <h2>Phiên đăng nhập</h2>
          </div>
          <div className="cb pf-session">
            {session ? (
              <>
                <div className="pf-left">
                  <b>{leftText(session.left)}</b>
                  <span className="muted sm">Hết hạn lúc {fmtDateTime(session.exp)}</span>
                </div>
                <div className="pf-bar" role="progressbar" aria-label="Thời gian còn lại của phiên" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(Math.min(1, session.left / SESSION_MS) * 100)}>
                  <i style={{ width: `${Math.min(1, session.left / SESSION_MS) * 100}%` }} />
                </div>
              </>
            ) : (
              <span className="muted">Không đọc được thời hạn phiên.</span>
            )}
            <p className="muted sm" style={{ margin: 0 }}>Phiên tự kết thúc khi hết hạn. Đóng tab cũng sẽ đăng xuất bạn.</p>
            <button type="button" className="btn" onClick={logout}>
              <Icon name="logout" />
              Đăng xuất
            </button>
          </div>
        </section>
      </div>
    </div>
  )
}
