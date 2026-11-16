import { useEffect } from 'react'

// Gợn sóng khi bấm nút chính (.btn.pri): một listener toàn cục, tắt khi người dùng chọn giảm chuyển động.
export default function useRipple() {
  useEffect(() => {
    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return undefined
    const onDown = (e) => {
      const b = e.target.closest?.('.btn.pri')
      if (!b || b.disabled) return
      const r = b.getBoundingClientRect()
      const s = document.createElement('span')
      s.className = 'ripple'
      s.style.left = `${e.clientX - r.left}px`
      s.style.top = `${e.clientY - r.top}px`
      b.appendChild(s)
      setTimeout(() => s.remove(), 600)
    }
    document.addEventListener('pointerdown', onDown)
    return () => document.removeEventListener('pointerdown', onDown)
  }, [])
}
