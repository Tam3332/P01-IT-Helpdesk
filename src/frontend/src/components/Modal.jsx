import { useEffect, useRef, useState } from 'react'
import Icon from './Icon'

// Hộp thoại theo prototype. onOk có thể async; ném Error(message) để hiện lỗi trong hộp thoại,
// trả về false để giữ hộp thoại mở, còn lại coi như thành công và gọi onClose.
export default function Modal({ title, children, okText = 'Xác nhận', cancelText = 'Hủy', okClass = 'pri', wide, onOk, onClose }) {
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const box = useRef(null)

  useEffect(() => {
    const first = box.current?.querySelector('input,select,textarea')
    ;(first || box.current?.querySelector('[data-ok]'))?.focus()
    const onKey = (e) => e.key === 'Escape' && !busy && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose, busy])

  const submit = async () => {
    if (busy) return
    setErr('')
    setBusy(true)
    try {
      const r = await onOk?.()
      if (r !== false) onClose()
    } catch (e) {
      setErr(e?.message || 'Không thể thực hiện. Vui lòng thử lại.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="ov" onMouseDown={(e) => e.target === e.currentTarget && !busy && onClose()}>
      <div className={`modal ${wide ? 'wide' : ''}`} role="dialog" aria-modal="true" aria-label={title} ref={box}>
        <header>
          <h2>{title}</h2>
          <button type="button" className="iconbtn" aria-label="Đóng" onClick={onClose} disabled={busy}>
            <Icon name="x" />
          </button>
        </header>
        <div className="mb">
          {children}
          {err && (
            <div className="err" role="alert" style={{ color: 'var(--bad)', fontSize: 13, fontWeight: 500 }}>
              {err}
            </div>
          )}
        </div>
        <footer>
          <button type="button" className="btn" onClick={onClose} disabled={busy}>
            {cancelText}
          </button>
          <button type="button" data-ok className={`btn ${okClass}`} onClick={submit} disabled={busy}>
            {busy ? 'Đang xử lý...' : okText}
          </button>
        </footer>
      </div>
    </div>
  )
}

export const Field = ({ label, required, hint, error, children, htmlFor }) => (
  <div className={`field ${error ? 'has-err' : ''}`}>
    {label && (
      <label htmlFor={htmlFor}>
        {label}
        {required && <span style={{ color: 'var(--bad)' }}> *</span>}
      </label>
    )}
    {children}
    {hint && <span className="hint">{hint}</span>}
    {error && <span className="err">{error}</span>}
  </div>
)
