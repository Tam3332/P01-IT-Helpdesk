import { SLA_THRESHOLDS } from '@/config/constants'

// elapsedHours / limitHours -> trạng thái SLA
export function getSlaState(elapsedHours, limitHours) {
  if (!limitHours) return 'NORMAL'
  const ratio = elapsedHours / limitHours
  if (ratio > SLA_THRESHOLDS.BREACH) return 'BREACHED'
  if (ratio >= SLA_THRESHOLDS.NEAR) return 'NEAR_BREACH'
  return 'NORMAL'
}
