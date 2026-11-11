import { OPEN_STATUSES } from '@/config/domain'

// Trả { key, pct, left, done } cho một ticket.
// Ưu tiên nhãn slaLabel do backend tính (SDD: Normal < 80%, Near 80–100%, Breached > 100%); thiếu thì tự tính từ hạn.
export function slaOf(t, nowMs = Date.now()) {
  const created = t.createdAt ? new Date(t.createdAt).getTime() : null
  const due = t.resolutionDeadline ? new Date(t.resolutionDeadline).getTime() : null
  const finishedAt = t.resolvedAt || t.closedAt
  if (!OPEN_STATUSES.includes(t.status) && (t.status === 'RESOLVED' || t.status === 'CLOSED' || t.status === 'REOPEN_PENDING')) {
    if (due && finishedAt) {
      const ok = new Date(finishedAt).getTime() <= due
      return { key: ok ? 'DONE' : 'LATE', pct: 100, left: 0, done: true }
    }
    if (t.isSlaBreached) {
      return { key: 'LATE', pct: 100, left: 0, done: true }
    }
    return { key: 'DONE', pct: 100, left: 0, done: true }
  }
  let pct = 0
  if (created && due && due > created) pct = ((nowMs - created) / (due - created)) * 100
  let key = t.slaLabel
  if (!key) key = t.isSlaBreached || pct > 100 ? 'BREACHED' : pct >= 80 ? 'NEAR_BREACH' : 'NORMAL'
  return { key, pct, left: due ? due - nowMs : 0, done: false }
}

// 90 phút -> "1 giờ 30 phút"; từ 48 giờ trở lên -> "2 ngày 3 giờ"
export function dur(ms) {
  const m = Math.max(0, Math.round(Math.abs(ms) / 60000))
  const h = Math.floor(m / 60)
  const mm = m % 60
  if (h >= 48) return `${Math.floor(h / 24)} ngày ${h % 24} giờ`
  return h > 0 ? `${h} giờ ${mm} phút` : `${mm} phút`
}

export const humanSize = (b = 0) => (b > 1048576 ? `${(b / 1048576).toFixed(1)} MB` : `${Math.max(1, Math.round(b / 1024))} KB`)
