import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Icon from './Icon'
import { StatusBadge } from './Badges'
import { useAuth } from '@/context/AuthContext'
import { ROLE_HOME } from '@/config/constants'
import { getNotifications } from '@/services/notificationService'
import { fmtDateTime } from '@/utils/format'

export default function NotificationBell() {
  const { user } = useAuth()
  const navigate = useNavigate()
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
      } catch {}
    }
  }

  const markAllAsRead = () => {
    const allIds = items.map((i) => i.id)
    const next = Array.from(new Set([...readIds, ...allIds]))
    setReadIds(next)
    try {
      localStorage.setItem(`read_notifs_${user?.id}`, JSON.stringify(next))
    } catch {}
  }

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
        aria-label="Thông báo"
        title="Thông báo cập nhật vé"
        onClick={() => setOpen((prev) => !prev)}
        style={{
          position: 'relative',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: 38,
          height: 38,
          borderRadius: '50%',
          background: open ? 'rgba(255, 255, 255, 0.1)' : 'transparent',
          border: '1px solid rgba(255, 255, 255, 0.12)',
          cursor: 'pointer',
          color: 'var(--text, #e2e8f0)',
          transition: 'all 0.2s',
        }}
      >
        <Icon name="bell" />
        {unreadCount > 0 && (
          <span
            style={{
              position: 'absolute',
              top: 2,
              right: 2,
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
              boxShadow: '0 0 0 2px #0f172a',
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
            background: '#1e293b',
            border: '1px solid #334155',
            borderRadius: 12,
            boxShadow: '0 12px 32px rgba(0, 0, 0, 0.45)',
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
              borderBottom: '1px solid #334155',
              background: 'rgba(15, 23, 42, 0.6)',
            }}
          >
            <div style={{ fontWeight: 600, fontSize: 14, color: '#f8fafc' }}>
              Thông báo ({unreadCount} mới)
            </div>
            {unreadCount > 0 && (
              <button
                type="button"
                onClick={markAllAsRead}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: 12,
                  color: '#38bdf8',
                  cursor: 'pointer',
                  padding: 0,
                }}
              >
                Đã đọc tất cả
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
                  color: '#94a3b8',
                }}
              >
                Chưa có thông báo nào
              </div>
            ) : (
              items.map((item) => {
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
                      borderBottom: '1px solid rgba(51, 65, 85, 0.4)',
                      background: isUnread ? 'rgba(56, 189, 248, 0.08)' : 'transparent',
                      transition: 'background 0.15s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = isUnread
                        ? 'rgba(56, 189, 248, 0.14)'
                        : 'rgba(255, 255, 255, 0.04)'
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = isUnread
                        ? 'rgba(56, 189, 248, 0.08)'
                        : 'transparent'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
                      <span
                        style={{
                          fontSize: 12,
                          fontWeight: 700,
                          color: '#38bdf8',
                          fontFamily: 'monospace',
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
                        color: isUnread ? '#f8fafc' : '#cbd5e1',
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
                        color: '#64748b',
                        marginTop: 2,
                      }}
                    >
                      <span>{fmtDateTime(item.timestamp)}</span>
                    </div>
                  </div>
                )
              })
            )}
          </div>
        </div>
      )}
    </div>
  )
}
