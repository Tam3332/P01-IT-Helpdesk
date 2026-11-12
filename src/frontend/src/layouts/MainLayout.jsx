import { Fragment, useEffect, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'

import NotificationBell from '@/components/NotificationBell'
import SessionWarning from '@/components/SessionWarning'
import { useAuth } from '@/context/AuthContext'
import { ROLE_HOME, ROLE_LABEL } from '@/config/constants'
import { NAV, PASSWORD_LABEL, PROFILE_LABEL, findNavItem, passwordPath, profilePath, ticketListItem } from '@/config/nav'
import { initials } from '@/utils/format'

// Khung chung: sidebar + topbar + vùng nội dung, y hệt prototype. Dưới 900px sidebar thành drawer.
export default function MainLayout() {
  const { user, logout } = useAuth()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  // Menu bên trái: ghim cố định (mặc định) hoặc ẩn đi và mở dạng ngăn trượt bằng nút ☰; nhớ lựa chọn của người dùng
  const [pinned, setPinned] = useState(() => {
    try {
      return localStorage.getItem('hd_sidebar') !== 'hidden'
    } catch {
      return true
    }
  })
  const togglePinned = () => {
    const next = !pinned
    setPinned(next)
    setOpen(false)
    try {
      localStorage.setItem('hd_sidebar', next ? 'pinned' : 'hidden')
    } catch {
      /* không lưu được thì chỉ áp dụng cho lần xem này */
    }
  }
  const items = NAV[user.role] || []
  const current = findNavItem(user.role, pathname)

  // Đổi trang thì đóng drawer; khoá cuộn nền khi drawer mở
  useEffect(() => setOpen(false), [pathname])
  useEffect(() => {
    document.body.style.overflow = open ? 'hidden' : ''
    return () => {
      document.body.style.overflow = ''
    }
  }, [open])
  useEffect(() => {
    if (!open) return
    const onKey = (e) => e.key === 'Escape' && setOpen(false)
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open])

  const profile = profilePath(user.role)
  const password = passwordPath(user.role)
  const isProfile = pathname === profile
  const isPassword = pathname === password

  // Admin vừa cấp lại mật khẩu: buộc đổi mật khẩu trước khi dùng tiếp (cờ do backend gửi, hiện chưa có)
  useEffect(() => {
    if (user.mustChangePassword && !isPassword) navigate(password, { replace: true })
  }, [user.mustChangePassword, isPassword, password, navigate])

  const forbidden = pathname !== '/' && !pathname.startsWith(ROLE_HOME[user.role]) // route của vai trò khác
  const isDetail = /\/tickets\/(?!new$)[^/]+$/.test(pathname)
  const list = ticketListItem(user.role)

  // Đường dẫn breadcrumb: mọi cấp trừ cấp cuối đều bấm được
  const home = ROLE_HOME[user.role]
  const crumbs = [{ label: 'Helpdesk', to: home }]
  if (forbidden) crumbs.push({ label: 'Không có quyền truy cập' })
  else if (isPassword) crumbs.push({ label: PROFILE_LABEL, to: profile }, { label: PASSWORD_LABEL })
  else if (isProfile) crumbs.push({ label: PROFILE_LABEL })
  else if (isDetail && list) crumbs.push({ label: list.label, to: list.path }, { label: 'Chi tiết ticket' })
  else if (current) {
    if (current.sub) crumbs.push({ label: 'Cài đặt hệ thống', to: '/admin/settings' })
    if (pathname !== current.path) crumbs.push({ label: current.label, to: current.path }, { label: 'Chi tiết' })
    else crumbs.push({ label: current.label })
  } else crumbs.push({ label: 'Không tìm thấy' })

  // Quay lại trang trước; nếu mở thẳng bằng đường dẫn thì lên cấp trên
  const goBack = () => (window.history.state?.idx > 0 ? navigate(-1) : navigate(crumbs[crumbs.length - 2]?.to || home))

  return (
    <div className={`shell ${pinned ? '' : 'nosb'}`}>
      <div className={`veil ${open ? 'on' : ''}`} onClick={() => setOpen(false)} />
      <aside className={`sidebar ${open ? 'open' : ''}`} aria-label="Điều hướng">
        <div className="brand">
          <div className="logo">H</div>
          <div>
            <b>Helpdesk</b>
            <span>Hỗ trợ CNTT nội bộ</span>
          </div>
          <button type="button" className="iconbtn sbtoggle" aria-label={pinned ? 'Ẩn menu' : 'Luôn hiện menu'} title={pinned ? 'Ẩn menu' : 'Luôn hiện menu'} onClick={togglePinned}>
            <Icon name={pinned ? 'chevL' : 'chevR'} />
          </button>
        </div>
        <div className="rolechip">
          <Icon name="shield" />
          <div>
            <small>Vai trò</small>
            <b>{ROLE_LABEL[user.role]}</b>
          </div>
        </div>
        <div className="navlabel">Điều hướng</div>
        <nav className="nav">
          {items.map((i) => (
            <button
              key={i.path}
              type="button"
              className={i.sub ? 'sub' : ''}
              aria-current={current?.path === i.path ? 'page' : undefined}
              onClick={() => navigate(i.path)}
            >
              <Icon name={i.icon} />
              {i.label}
            </button>
          ))}
        </nav>
        <div className="sbfoot">
          <button type="button" className="who" aria-current={isProfile || isPassword ? 'page' : undefined} title={PROFILE_LABEL} onClick={() => navigate(profile)}>
            <div className="av">{initials(user.fullName)}</div>
            <div style={{ minWidth: 0 }}>
              <b style={{ display: 'block', fontSize: 13 }}>{user.fullName}</b>
              <span className="sm muted" style={{ overflowWrap: 'anywhere' }}>
                {user.email}
              </span>
            </div>
          </button>
          <button type="button" className="btn block sm" onClick={logout}>
            <Icon name="logout" />
            Đăng xuất
          </button>
        </div>
      </aside>

      <div className="main">
        <header className="topbar">
          <button type="button" className="iconbtn burger" aria-label="Mở menu" onClick={() => setOpen(true)}>
            <Icon name="menu" />
          </button>
          {pathname !== home && (
            <button type="button" className="iconbtn" aria-label="Quay lại" title="Quay lại" onClick={goBack}>
              <Icon name="back" />
            </button>
          )}
          <nav className="crumb" aria-label="Đường dẫn">
            {crumbs.map((c, i) =>
              i < crumbs.length - 1 ? (
                <Fragment key={c.label}>
                  <Link className="crumb-link" to={c.to}>
                    {c.label}
                  </Link>
                  <span className="sep" aria-hidden="true">/</span>
                </Fragment>
              ) : (
                <b key={c.label} aria-current="page">
                  {c.label}
                </b>
              ),
            )}
          </nav>
          <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center' }}>
            <NotificationBell />
          </div>
        </header>
        <SessionWarning />
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
