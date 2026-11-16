import { useNavigate } from 'react-router-dom'
import { PriorityBadge, SlaBadge, StatusBadge } from './Badges'
import { EmptyState } from './StateViews'
import { ROLE_HOME } from '@/config/constants'
import { ticketRef } from '@/services/ticketService'
import { useAuth } from '@/context/AuthContext'
import { fmtDate } from '@/utils/format'
import { t } from '@/i18n'

// Tô sáng từ khóa tìm kiếm trong văn bản của người dùng (giữ nguyên nội dung, chỉ bọc <mark>).
export function Marked({ text, q }) {
  const k = (q || '').trim()
  if (!k || !text) return text
  const esc = k.replace(/[.*+?^$(){}|[\]\\]/g, '\\$&')
  const parts = String(text).split(new RegExp(`(${esc})`, 'ig'))
  return parts.map((p, i) => (i % 2 ? <mark key={i}>{p}</mark> : p))
}

const who = (name, id) => name || (id != null ? t('Người dùng #{id}', { id }) : null)

// Bảng ticket dùng chung. categories: [{id, categoryName}] để đổi categoryId -> tên khi backend chỉ trả id.
// hidden: các cột người dùng đã ẩn (Mã và Tiêu đề luôn hiện). highlight: từ khóa tìm kiếm để tô sáng trong tiêu đề.
export default function TicketTable({ tickets, categories = [], showSla, onClear, hidden = [], highlight = '' }) {
  const { user } = useAuth()
  const navigate = useNavigate()
  const base = `${ROLE_HOME[user.role]}/tickets`
  const catName = (tk) => tk.categoryName || categories.find((c) => c.id === tk.categoryId)?.categoryName || (tk.categoryId != null ? `#${tk.categoryId}` : '—')

  if (!tickets.length) {
    return (
      <EmptyState title={t('Không tìm thấy ticket phù hợp')} action={onClear && <button type="button" className="btn sm" onClick={onClear}>{t('Xóa bộ lọc')}</button>}>
        {t('Thử đổi từ khóa hoặc bỏ bớt bộ lọc.')}
      </EmptyState>
    )
  }
  const show = (k) => !hidden.includes(k)
  const go = (tk) => navigate(`${base}/${encodeURIComponent(ticketRef(tk))}`)
  return (
    <div className="tw">
      <table>
        <thead>
          <tr>
            <th>{t('Mã')}</th>
            <th>{t('Tiêu đề')}</th>
            {show('requester') && <th>{t('Người yêu cầu')}</th>}
            {show('category') && <th>{t('Danh mục')}</th>}
            {show('priority') && <th>{t('Ưu tiên')}</th>}
            {show('status') && <th>{t('Trạng thái')}</th>}
            {show('assignee') && <th>{t('Người xử lý')}</th>}
            {show('created') && <th>{t('Ngày tạo')}</th>}
            {showSla && show('sla') && <th>SLA</th>}
          </tr>
        </thead>
        <tbody>
          {tickets.map((tk) => {
            const a = who(tk.assigneeName, tk.currentAssigneeId ?? tk.assigneeId)
            return (
              <tr key={tk.id ?? ticketRef(tk)} data-a tabIndex={0} onClick={() => go(tk)} onKeyDown={(e) => e.key === 'Enter' && go(tk)}>
                <td>
                  <button type="button" className="idlink" onClick={(e) => { e.stopPropagation(); go(tk) }}>
                    {tk.ticketNumber ?? tk.id}
                  </button>
                </td>
                <td className="tt"><Marked text={tk.title} q={highlight} /></td>
                {show('requester') && <td className="cat">{who(tk.requesterName, tk.requesterId) || '—'}</td>}
                {show('category') && <td className="cat">{catName(tk)}</td>}
                {show('priority') && <td><PriorityBadge priority={tk.priority} /></td>}
                {show('status') && <td><StatusBadge status={tk.status} /></td>}
                {show('assignee') && <td className={`cat ${a ? '' : 'faint'}`}>{a || (tk.status === 'NEW' ? t('Chưa phân công') : '—')}</td>}
                {show('created') && <td className="nw num">{fmtDate(tk.createdAt)}</td>}
                {showSla && show('sla') && <td><SlaBadge ticket={tk} /></td>}
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
