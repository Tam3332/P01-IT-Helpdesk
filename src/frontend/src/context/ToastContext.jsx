import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import Icon from '@/components/Icon'

const ToastContext = createContext(() => {})

let seq = 0

// toast('Đã lưu') hoặc toast('Có lỗi', 'err'). Tự tắt sau 3,5 giây (lỗi: 5 giây).
export function ToastProvider({ children }) {
  const [items, setItems] = useState([])

  const toast = useCallback((msg, type = 'ok') => {
    const id = ++seq
    setItems((l) => [...l, { id, msg, type }])
    setTimeout(() => setItems((l) => l.filter((t) => t.id !== id)), type === 'err' ? 5000 : 3500)
  }, [])

  const value = useMemo(() => toast, [toast])
  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toasts" role="status" aria-live="polite">
        {items.map((t) => (
          <div key={t.id} className={`toast ${t.type}`}>
            <Icon name={t.type === 'err' ? 'alert' : t.type === 'ok' ? 'check' : 'info'} />
            <span>{t.msg}</span>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => useContext(ToastContext)
