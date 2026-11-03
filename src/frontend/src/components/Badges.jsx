import { PRIORITY, SLA, STATUS } from '@/config/domain'
import { slaOf } from '@/utils/sla'

export const StatusBadge = ({ status }) => <span className={`badge ${STATUS[status]?.badge || 'b-x'}`}>{STATUS[status]?.label || status || '—'}</span>
export const PriorityBadge = ({ priority }) => <span className={`badge ${PRIORITY[priority]?.badge || 'p-x'}`}>{PRIORITY[priority]?.label || priority || '—'}</span>
export function SlaBadge({ ticket }) {
  const s = slaOf(ticket)
  const d = SLA[s.key] || SLA.NORMAL
  return <span className={`badge ${d.badge}`}>{d.label}</span>
}
export const RoleBadge = ({ label }) => <span className="badge b-Assigned">{label}</span>
