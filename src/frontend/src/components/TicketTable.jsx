import { useNavigate } from 'react-router-dom'
import { PriorityBadge, SlaBadge, StatusBadge } from './Badges'
import { EmptyState } from './StateViews'
import { ROLE_HOME } from '@/config/constants'
import { ticketRef } from '@/services/ticketService'
import { useAuth } from '@/context/AuthContext'
import { fmtDate } from '@/utils/format'

const who = (name, id) => name || (id != null ? `Người dùng #${id}` : null)

// Bảng ticket dùng chung. categories: [{id, categoryName}] để đổi categoryId -> tên khi backend chỉ trả id.
export default function TicketTable({ tickets, categories = [], showSla, onClear }) {
  const { user } = useAuth()
  const navigate = useNavigate()
  const base = `${ROLE_HOME[user.role]}/tickets`
  const catName = (t) => t.categoryName || categories.find((c) => c.id === t.categoryId)?.categoryName || (t.categoryId != null ? `#${t.categoryId}` : '—')

  if (!tickets.length) {
    return (
      <EmptyState title="Không tìm thấy ticket phù hợp" action={onClear && <button type="button" className="btn sm" onClick={onClear}>Xóa bộ lọc</button>}>
        Thử đổi từ khóa hoặc bỏ bớt bộ lọc.
      </EmptyState>
    )
  }
  const go = (t) => navigate(`${base}/${encodeURIComponent(ticketRef(t))}`)
  return (
    <div className="tw">
      <table>
        <thead>
          <tr>
            <th>Mã</th>
            <th>Tiêu đề</th>
            <th>Người yêu cầu</th>
            <th>Danh mục</th>
            <th>Ưu tiên</th>
            <th>Trạng thái</th>
            <th>Người xử lý</th>
            <th>Ngày tạo</th>
            {showSla && <th>SLA</th>}
          </tr>
        </thead>
        <tbody>
          {tickets.map((t) => {
            const a = who(t.assigneeName, t.currentAssigneeId ?? t.assigneeId)
            return (
              <tr key={t.id ?? ticketRef(t)} tabIndex={0} onClick={() => go(t)} onKeyDown={(e) => e.key === 'Enter' && go(t)}>
                <td>
                  <button type="button" className="idlink" onClick={(e) => { e.stopPropagation(); go(t) }}>
                    {t.ticketNumber ?? t.id}
                  </button>
                </td>
                <td className="tt">{t.title}</td>
                <td className="cat">{who(t.requesterName, t.requesterId) || '—'}</td>
                <td className="cat">{catName(t)}</td>
                <td><PriorityBadge priority={t.priority} /></td>
                <td><StatusBadge status={t.status} /></td>
                <td className={`cat ${a ? '' : 'faint'}`}>{a || 'Chưa phân công'}</td>
                <td className="nw num">{fmtDate(t.createdAt)}</td>
                {showSla && <td><SlaBadge ticket={t} /></td>}
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
