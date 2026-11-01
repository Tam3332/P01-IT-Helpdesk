const p2 = (n) => String(n).padStart(2, '0')

export const fmtDateTime = (v) => {
  if (!v) return '—'
  const d = new Date(v)
  return `${p2(d.getDate())}/${p2(d.getMonth() + 1)}/${d.getFullYear()} ${p2(d.getHours())}:${p2(d.getMinutes())}`
}

export const fmtDate = (v) => fmtDateTime(v).slice(0, 10)

// Chữ cái đầu của hai từ cuối trong họ tên, dùng cho avatar ("Nguyễn Văn An" -> "VA")
export function initials(name = '') {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (!parts.length) return '?'
  return parts
    .slice(-2)
    .map((w) => w[0])
    .join('')
    .toUpperCase()
}
