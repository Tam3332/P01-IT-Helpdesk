import { useEffect, useState } from 'react'
import Icon from '@/components/Icon'
import { STORAGE_KEYS } from '@/config/constants'
import { getTokenExpiryMs } from '@/utils/jwt'

const WARN_MS = 2 * 60 * 1000 // báo trước 2 phút để kịp lưu việc đang làm

const mmss = (ms) => {
  const s = Math.max(0, Math.ceil(ms / 1000))
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`
}

// Cảnh báo phiên sắp hết hạn. Hết hạn thì AuthContext tự đăng xuất; ở đây chỉ nhắc người dùng.
export default function SessionWarning() {
  const [now, setNow] = useState(Date.now())
  const [dismissed, setDismissed] = useState(null) // hạn của phiên mà người dùng đã bấm "Đã hiểu"

  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(t)
  }, [])

  const exp = getTokenExpiryMs(sessionStorage.getItem(STORAGE_KEYS.token) || '')
  const left = exp ? exp - now : Infinity
  if (!exp || left > WARN_MS || left <= 0 || dismissed === exp) return null

  return (
    <div className="sesswarn" role="alert">
      <Icon name="clock" />
      <div className="grow">
        <b>Phiên đăng nhập sắp hết hạn</b>
        <span>
          Còn <time aria-live="off">{mmss(left)}</time>. Hãy lưu công việc đang làm, sau đó bạn sẽ được đưa về trang đăng nhập.
        </span>
      </div>
      <button type="button" className="btn sm" onClick={() => setDismissed(exp)}>
        Đã hiểu
      </button>
    </div>
  )
}
