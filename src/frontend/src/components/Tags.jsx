import { Tag } from 'antd'
import { STATUS_META, PRIORITY_META, SLA_META } from '@/config/constants'

const make = (META) => function MetaTag({ value }) {
  const m = META[value]
  return m ? <Tag color={m.color}>{m.label}</Tag> : null
}
export const StatusTag = make(STATUS_META)
export const PriorityTag = make(PRIORITY_META)
export const SlaTag = make(SLA_META)
