import { PRIORITY, SLA, STATUS } from '@/config/domain'
import { slaOf } from '@/utils/sla'
import { t } from '@/i18n'

export const StatusBadge = ({ status }) => <span className={`badge ${STATUS[status]?.badge || 'b-x'}`}>{t(STATUS[status]?.label || status || '—')}</span>
export const PriorityBadge = ({ priority }) => <span className={`badge ${PRIORITY[priority]?.badge || 'p-x'}`}>{t(PRIORITY[priority]?.label || priority || '—')}</span>
export function SlaBadge({ ticket }) {
  const s = slaOf(ticket)
  const d = SLA[s.key] || SLA.NORMAL
  return <span className={`badge ${d.badge}`}>{t(d.label)}</span>
}
export const RoleBadge = ({ label }) => <span className="badge b-Assigned">{label}</span>
