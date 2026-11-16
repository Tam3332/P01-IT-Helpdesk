import { useEffect } from 'react'

// Khi một thông báo lỗi (.err) vừa xuất hiện: rung nhẹ, đưa focus về ô lỗi đầu tiên, rung máy (nếu có).
// Tôn trọng prefers-reduced-motion. Chỉ phản ứng với node mới thêm, không chạy lại khi đổi ngôn ngữ.
export default function useErrorFeedback(rootRef) {
  useEffect(() => {
    const root = rootRef?.current || document.body
    const reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    const mo = new MutationObserver((muts) => {
      for (const m of muts)
        for (const n of m.addedNodes) {
          if (n.nodeType !== 1) continue
          const err = n.matches?.('.err') ? n : n.querySelector?.('.err')
          if (!err) continue
          if (!reduced) {
            err.classList.add('shake')
            setTimeout(() => err.classList.remove('shake'), 350)
          }
          root.querySelector('.has-err .in, .has-err .sel, .has-err .ta, [aria-invalid="true"]')?.focus?.()
          try {
            navigator.vibrate?.(20)
          } catch {
            /* thiết bị không hỗ trợ */
          }
          return
        }
    })
    mo.observe(root, { childList: true, subtree: true })
    return () => mo.disconnect()
  }, [rootRef])
}
