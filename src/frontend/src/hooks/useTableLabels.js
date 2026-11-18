import { useEffect } from 'react'

// Gắn data-l (nhãn cột) vào từng ô của bảng để CSS dưới 640px hiển thị mỗi dòng thành một thẻ.
// Chạy trên cả bảng render sau (MutationObserver), không đụng vào nội dung bảng.
export default function useTableLabels(rootRef) {
  useEffect(() => {
    const root = rootRef.current
    if (!root) return undefined
    const label = () => {
      root.querySelectorAll('table').forEach((tb) => {
        const heads = [...tb.querySelectorAll('thead th')].map((h) => h.textContent.trim())
        tb.querySelectorAll('tbody tr').forEach((tr) => {
          ;[...tr.children].forEach((td, i) => {
            if (heads[i] && td.dataset.l !== heads[i]) td.dataset.l = heads[i]
          })
        })
      })
    }
    label()
    const mo = new MutationObserver(label)
    mo.observe(root, { childList: true, subtree: true, characterData: true })
    return () => mo.disconnect()
  }, [rootRef])
}
