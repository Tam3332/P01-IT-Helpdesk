import { useEffect, useRef, useState } from 'react'

// Số đếm lên trong 700ms khi hiện lần đầu hoặc khi giá trị đổi. Đổi ngôn ngữ không làm đếm lại
// (component không bị mount lại, effect chỉ phụ thuộc vào giá trị). Tôn trọng prefers-reduced-motion.
export function useCountUp(value, ms = 700) {
  const isNum = typeof value === 'number' && Number.isFinite(value)
  const reduced = typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
  const [shown, setShown] = useState(isNum && !reduced ? 0 : value)
  const from = useRef(0)
  useEffect(() => {
    if (!isNum || reduced) {
      setShown(value)
      return undefined
    }
    const start = performance.now()
    const a = from.current
    let raf
    const tick = (now) => {
      const p = Math.min(1, (now - start) / ms)
      const v = a + (value - a) * (1 - (1 - p) ** 3)
      from.current = v
      setShown(p < 1 ? v : value)
      if (p < 1) raf = requestAnimationFrame(tick)
    }
    raf = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(raf)
  }, [value, isNum, reduced, ms])
  return isNum ? Math.round(shown) : shown
}

export default function CountUp({ value }) {
  const v = useCountUp(value)
  return <>{v ?? '—'}</>
}
