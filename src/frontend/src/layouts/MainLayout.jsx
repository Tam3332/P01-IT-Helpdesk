import { Fragment, Suspense, useEffect, useRef, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import Loading from '@/components/Loading'

import NotificationBell from '@/components/NotificationBell'
import PrefsToggle from '@/components/PrefsToggle'
import { t } from '@/i18n'
import SessionWarning from '@/components/SessionWarning'
import { useAuth } from '@/context/AuthContext'
import { ROLE_HOME, ROLE_LABEL } from '@/config/constants'
import { NAV, PASSWORD_LABEL, PROFILE_LABEL, findNavItem, passwordPath, profilePath, ticketListItem } from '@/config/nav'
import { avatarStyle, initials } from '@/utils/avatar'
import useDocTitle from '@/hooks/useDocTitle'
import useErrorFeedback from '@/hooks/useErrorFeedback'
import useRipple from '@/hooks/useRipple'
import useTableLabels from '@/hooks/useTableLabels'

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
  if (forbidden) crumbs.push({ label: t('Không có quyền truy cập') })
  else if (isPassword) crumbs.push({ label: t(PROFILE_LABEL), to: profile }, { label: t(PASSWORD_LABEL) })
  else if (isProfile) crumbs.push({ label: t(PROFILE_LABEL) })
  else if (isDetail && list) crumbs.push({ label: t(list.label), to: list.path }, { label: t('Chi tiết ticket') })
  else if (current) {
    if (current.sub) crumbs.push({ label: t('Cài đặt hệ thống'), to: '/admin/settings' })
    if (pathname !== current.path) crumbs.push({ label: t(current.label), to: current.path }, { label: t('Chi tiết') })
    else crumbs.push({ label: t(current.label) })
  } else crumbs.push({ label: t('Không tìm thấy') })

  // Quay lại trang trước; nếu mở thẳng bằng đường dẫn thì lên cấp trên
  useDocTitle(crumbs[crumbs.length - 1].label)
  useRipple()
  const contentRef = useRef(null)
  useErrorFeedback(contentRef)
  useTableLabels(contentRef)

  const goBack = () => (window.history.state?.idx > 0 ? navigate(-1) : navigate(crumbs[crumbs.length - 2]?.to || home))

  return (
    <div className={`shell ${pinned ? '' : 'nosb'}`}>
      <a className="skip" href="#main">{t('Bỏ qua điều hướng')}</a>
      <div className={`veil ${open ? 'on' : ''}`} onClick={() => setOpen(false)} />
      <aside className={`sidebar ${open ? 'open' : ''}`} aria-label={t('Điều hướng')}>
        <div className="brand">
          <div className="logo">H</div>
          <div>
            <b>Helpdesk</b>
            <span>{t('Hỗ trợ CNTT nội bộ')}</span>
          </div>
          <button type="button" className="iconbtn sbtoggle" aria-label={pinned ? t('Ẩn menu') : t('Luôn hiện menu')} title={pinned ? t('Ẩn menu') : t('Luôn hiện menu')} onClick={togglePinned}>
            <Icon name={pinned ? 'chevL' : 'chevR'} />
          </button>
        </div>
        <div className="rolechip">
          <Icon name="shield" />
          <div>
            <small>{t('Vai trò')}</small>
            <b>{t(ROLE_LABEL[user.role])}</b>
          </div>
        </div>
        <div className="navlabel">{t('Điều hướng')}</div>
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
              {t(i.label)}
            </button>
          ))}
        </nav>
        <div className="sbfoot">
          <button type="button" className="who" aria-current={isProfile || isPassword ? 'page' : undefined} title={t(PROFILE_LABEL)} onClick={() => navigate(profile)}>
            <div className="av" style={user.avatarUrl ? { padding: 0, overflow: 'hidden' } : avatarStyle(user.fullName)}>
              {user.avatarUrl ? (
                <img src={user.avatarUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%' }} />
              ) : (
                initials(user.fullName)
              )}
            </div>
            <div style={{ minWidth: 0 }}>
              <b style={{ display: 'block', fontSize: 13 }}>{user.fullName}</b>
              <span className="sm muted" style={{ overflowWrap: 'anywhere' }}>
                {user.email}
              </span>
            </div>
          </button>
          <button type="button" className="btn block sm" onClick={logout}>
            <Icon name="logout" />
            {t('Đăng xuất')}
          </button>
        </div>
      </aside>

      <div className="main">
        <header className="topbar">
          <button type="button" className="iconbtn burger" aria-label={t('Mở menu')} onClick={() => setOpen(true)}>
            <Icon name="menu" />
          </button>
          {pathname !== home && (
            <button type="button" className="iconbtn" aria-label={t('Quay lại')} title={t('Quay lại')} onClick={goBack}>
              <Icon name="back" />
            </button>
          )}
          <nav className="crumb" aria-label={t('Đường dẫn')}>
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
            <PrefsToggle />
            <NotificationBell />
          </div>
        </header>
        <SessionWarning />
        <main className="content" id="main" tabIndex={-1} ref={contentRef}>
          <Suspense fallback={<Loading />}>
            <Outlet />
          </Suspense>
        </main>
      </div>
    </div>
  )
}
