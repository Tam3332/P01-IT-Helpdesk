import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Icon from './Icon'
import { StatusBadge } from './Badges'
import { useAuth } from '@/context/AuthContext'
import { ROLE_HOME } from '@/config/constants'
import { getNotifications } from '@/services/notificationService'
import { fmtDateTime } from '@/utils/format'
import { useToast } from '@/context/ToastContext'
import { t } from '@/i18n'

export default function NotificationBell() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const toast = useToast()
  const [open, setOpen] = useState(false)
  const [items, setItems] = useState([])
  const [readIds, setReadIds] = useState(() => {
    try {
      const stored = localStorage.getItem(`read_notifs_${user?.id}`)
      return stored ? JSON.parse(stored) : []
    } catch {
      return []
    }
  })
  const popoverRef = useRef(null)

  const fetchItems = async () => {
    if (!user) return
    const data = await getNotifications(15)
    setItems(data || [])
  }

  useEffect(() => {
    fetchItems()
    const timer = setInterval(fetchItems, 30000) // Polling nhẹ mỗi 30s
    return () => clearInterval(timer)
  }, [user])

  // Đóng khi click ngoài
  useEffect(() => {
    if (!open) return
    const handleClickOutside = (e) => {
      if (popoverRef.current && !popoverRef.current.contains(e.target)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [open])

  const unreadCount = items.filter((i) => !readIds.includes(i.id)).length

  const markAsRead = (id) => {
    if (!readIds.includes(id)) {
      const next = [...readIds, id]
      setReadIds(next)
      try {
        localStorage.setItem(`read_notifs_${user?.id}`, JSON.stringify(next))
      } catch { /* bỏ qua */ }
    }
  }

  const saveRead = (ids) => {
    setReadIds(ids)
    try {
      localStorage.setItem(`read_notifs_${user?.id}`, JSON.stringify(ids))
    } catch { /* bỏ qua */ }
  }

  const markAllAsRead = () => {
    const before = readIds
    saveRead(Array.from(new Set([...readIds, ...items.map((i) => i.id)])))
    toast(t('Đã đánh dấu đọc tất cả.'), 'ok', { undo: () => saveRead(before) })
  }

  // Nhóm theo ngày: Hôm nay / Hôm qua / ngày cụ thể (items đã sắp mới nhất trước)
  const dayLabel = (v) => {
    const d = new Date(v)
    const diff = Math.round((new Date().setHours(0, 0, 0, 0) - new Date(d).setHours(0, 0, 0, 0)) / 864e5)
    return diff === 0 ? t('Hôm nay') : diff === 1 ? t('Hôm qua') : fmtDateTime(v).slice(0, 10)
  }
  const groups = []
  items.forEach((it) => {
    const label = dayLabel(it.timestamp)
    const g = groups[groups.length - 1]
    if (g && g.label === label) g.list.push(it)
    else groups.push({ label, list: [it] })
  })

  const handleClickItem = (item) => {
    markAsRead(item.id)
    setOpen(false)
    const base = `${ROLE_HOME[user.role]}/tickets`
    navigate(`${base}/${encodeURIComponent(item.ticketNumber)}`)
  }

  return (
    <div className="notif-wrapper" ref={popoverRef} style={{ position: 'relative' }}>
      <button
        type="button"
        className="iconbtn"
        aria-label={t('Thông báo')}
        title={t('Thông báo cập nhật vé')}
        onClick={() => setOpen((prev) => !prev)}
        style={{
          position: 'relative',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: 38,
          height: 38,
          borderRadius: 10,
          background: open ? 'var(--surface2)' : 'var(--surface)',
          border: '1px solid var(--border)',
          cursor: 'pointer',
          color: 'var(--text)',
          transition: 'all 0.2s',
        }}
      >
        <Icon name="bell" />
        {unreadCount > 0 && (
          <span
            style={{
              position: 'absolute',
              top: -4,
              right: -4,
              minWidth: 18,
              height: 18,
              padding: '0 4px',
              fontSize: 11,
              fontWeight: 700,
              lineHeight: '18px',
              textAlign: 'center',
              borderRadius: 9,
              background: '#ef4444',
              color: '#fff',
              boxShadow: '0 0 0 2px var(--surface)',
            }}
          >
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div
          className="notif-popover"
          style={{
            position: 'absolute',
            top: 'calc(100% + 8px)',
            right: 0,
            width: 340,
            maxWidth: '90vw',
            maxHeight: 460,
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 12,
            boxShadow: 'var(--shadow-lg)',
            display: 'flex',
            flexDirection: 'column',
            zIndex: 1000,
            overflow: 'hidden',
          }}
        >
          <div
            style={{
              padding: '12px 16px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              borderBottom: '1px solid var(--border)',
              background: 'var(--surface2)',
            }}
          >
            <div style={{ fontWeight: 600, fontSize: 14, color: 'var(--text)' }}>
              {t('Thông báo ({n} mới)', { n: unreadCount })}
            </div>
            {unreadCount > 0 && (
              <button
                type="button"
                onClick={markAllAsRead}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: 12,
                  color: 'var(--primary-t)',
                  cursor: 'pointer',
                  padding: 0,
                }}
              >
                {t('Đã đọc tất cả')}
              </button>
            )}
          </div>

          <div style={{ flex: 1, overflowY: 'auto', padding: '6px 0' }}>
            {items.length === 0 ? (
              <div
                style={{
                  padding: 24,
                  textAlign: 'center',
                  fontSize: 13,
                  color: 'var(--faint)',
                }}
              >
                {t('Chưa có thông báo nào')}
              </div>
            ) : (
              groups.map((g) => (
                <div key={g.label}>
                  <div className="notif-day">{g.label}</div>
                  {g.list.map((item) => {
                const isUnread = !readIds.includes(item.id)
                return (
                  <div
                    key={item.id}
                    onClick={() => handleClickItem(item)}
                    style={{
                      padding: '10px 16px',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: 4,
                      cursor: 'pointer',
                      borderBottom: '1px solid var(--border)',
                      background: isUnread ? 'var(--primary-soft)' : 'transparent',
                      transition: 'background 0.15s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = isUnread
                        ? 'var(--primary-soft)'
                        : 'var(--surface2)'
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = isUnread
                        ? 'var(--primary-soft)'
                        : 'transparent'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
                      <span
                        style={{
                          fontSize: 12,
                          fontWeight: 700,
                          color: 'var(--primary-t)',
                          fontFamily: 'var(--mono)',
                        }}
                      >
                        {item.ticketNumber}
                      </span>
                      <StatusBadge status={item.status} />
                    </div>
                    <div
                      style={{
                        fontSize: 13,
                        fontWeight: isUnread ? 600 : 400,
                        color: isUnread ? 'var(--text)' : 'var(--muted)',
                        lineHeight: 1.4,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical',
                      }}
                    >
                      {item.title}
                    </div>
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'flex-end',
                        fontSize: 11,
                        color: 'var(--faint)',
                        marginTop: 2,
                      }}
                    >
                      <span>{fmtDateTime(item.timestamp)}</span>
                    </div>
                  </div>
                )
                  })}
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  )
}
