import { useEffect, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import { useAuth } from '@/context/AuthContext'
import { ROLE_HOME, ROLE_LABEL } from '@/config/constants'
import { NAV, findNavItem, ticketListItem } from '@/config/nav'
import { initials } from '@/utils/format'

// Khung chung: sidebar + topbar + vùng nội dung, y hệt prototype. Dưới 900px sidebar thành drawer.
export default function MainLayout() {
  const { user, logout } = useAuth()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
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

  const forbidden = pathname !== '/' && !pathname.startsWith(ROLE_HOME[user.role]) // route của vai trò khác
  const isDetail = /\/tickets\/(?!new$)[^/]+$/.test(pathname)
  const list = ticketListItem(user.role)

  return (
    <div className="shell">
      <div className={`veil ${open ? 'on' : ''}`} onClick={() => setOpen(false)} />
      <aside className={`sidebar ${open ? 'open' : ''}`} aria-label="Điều hướng">
        <div className="brand">
          <div className="logo">H</div>
          <div>
            <b>Helpdesk</b>
            <span>Hỗ trợ CNTT nội bộ</span>
          </div>
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
          <div className="who">
            <div className="av">{initials(user.fullName)}</div>
            <div style={{ minWidth: 0 }}>
              <b style={{ display: 'block', fontSize: 13 }}>{user.fullName}</b>
              <span className="sm muted" style={{ overflowWrap: 'anywhere' }}>
                {user.email}
              </span>
            </div>
          </div>
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
          <div className="crumb">
            <span className="root">Helpdesk</span>
            <span className="root">/</span>
            {isDetail && list ? (
              <>
                <Link to={list.path} style={{ color: 'inherit' }}>
                  {list.label}
                </Link>
                <span>/</span>
                <b>Chi tiết ticket</b>
              </>
            ) : (
              <>
                {current?.sub && (
                  <>
                    <span>Cài đặt hệ thống</span>
                    <span>/</span>
                  </>
                )}
                <b>{forbidden ? 'Không có quyền truy cập' : current?.label || 'Không tìm thấy'}</b>
              </>
            )}
          </div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
