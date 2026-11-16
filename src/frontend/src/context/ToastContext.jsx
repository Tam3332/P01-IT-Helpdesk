import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react'
import Icon from '@/components/Icon'
import { t } from '@/i18n'

const ToastContext = createContext(() => {})

let seq = 0

// toast('Đã lưu') | toast('Có lỗi', 'err') | toast('Đã đọc tất cả', 'ok', { undo: () => ... })
// Tự tắt sau 4 giây (lỗi: 6 giây, có Hoàn tác: 6 giây). Rê chuột vào thì dừng đếm; thanh dưới cho thấy thời gian còn lại.
export function ToastProvider({ children }) {
  const [items, setItems] = useState([])
  const timers = useRef({})

  const close = useCallback((id) => {
    clearTimeout(timers.current[id])
    delete timers.current[id]
    setItems((l) => l.filter((x) => x.id !== id))
  }, [])

  const arm = useCallback((id, ms) => {
    clearTimeout(timers.current[id])
    timers.current[id] = setTimeout(() => close(id), ms)
  }, [close])

  const toast = useCallback((msg, type = 'ok', opts = {}) => {
    const id = ++seq
    const ms = type === 'err' || opts.undo ? 6000 : 4000
    setItems((l) => [...l.slice(-3), { id, msg, type, undo: opts.undo, ms }])
    arm(id, ms)
  }, [arm])

  const value = useMemo(() => toast, [toast])
  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toasts" role="status" aria-live="polite">
        {items.map((x) => (
          <div
            key={x.id}
            className={`toast ${x.type}`}
            onMouseEnter={() => clearTimeout(timers.current[x.id])}
            onMouseLeave={() => arm(x.id, 1800)}
          >
            <Icon name={x.type === 'err' ? 'alert' : x.type === 'ok' ? 'check' : 'info'} />
            <span>{x.msg}</span>
            {x.undo && (
              <button
                type="button"
                className="toast-undo"
                onClick={() => {
                  x.undo()
                  close(x.id)
                }}
              >
                {t('Hoàn tác')}
              </button>
            )}
            <i className="toast-bar" style={{ animationDuration: `${x.ms}ms` }} />
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => useContext(ToastContext)
