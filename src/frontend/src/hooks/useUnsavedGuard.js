import { useEffect } from 'react'
import { t } from '@/i18n'

// Khi dirty = true: đóng tab / tải lại bị trình duyệt hỏi, bấm menu/breadcrumb/nút topbar thì hỏi xác nhận trước khi rời trang.
// Không chặn nút đổi ngôn ngữ, đổi giao diện, chuông thông báo (không rời trang).
const LEAVE = '.sidebar a, .sidebar button, .topbar a, .topbar button, .crumb'
const STAY = '.prefs, .notif-wrapper, .burger'

export default function useUnsavedGuard(dirty) {
  useEffect(() => {
    if (!dirty) return undefined
    const beforeUnload = (e) => {
      e.preventDefault()
      e.returnValue = ''
    }
    const onClick = (e) => {
      const el = e.target.closest?.(LEAVE)
      if (!el || el.closest(STAY)) return
      if (!window.confirm(t('Bạn có nội dung chưa gửi. Rời trang?'))) {
        e.preventDefault()
        e.stopPropagation()
      }
    }
    window.addEventListener('beforeunload', beforeUnload)
    document.addEventListener('click', onClick, true)
    return () => {
      window.removeEventListener('beforeunload', beforeUnload)
      document.removeEventListener('click', onClick, true)
    }
  }, [dirty])
}
