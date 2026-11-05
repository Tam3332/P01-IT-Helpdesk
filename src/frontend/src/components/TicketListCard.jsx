import { useEffect, useState } from 'react'
import Icon from './Icon'
import Pagination from './Pagination'
import TicketTable from './TicketTable'
import { Async } from './StateViews'
import useAsync from '@/hooks/useAsync'
import useDebounced from '@/hooks/useDebounced'
import { LIMITS, PRIORITY_KEYS, STATUS_KEYS, PRIORITY, STATUS } from '@/config/domain'
import { listTickets } from '@/services/ticketService'
import { listCategories } from '@/services/referenceService'
import { useAuth } from '@/context/AuthContext'
import { ROLES } from '@/config/constants'

// Thẻ danh sách ticket: tìm kiếm, lọc, phân trang, đều gọi API (HD05, HD13).
// fixed: tham số lọc cố định (ví dụ chỉ ticket đang mở). title: tiêu đề thẻ.
export default function TicketListCard({ title, fixed = {}, statuses }) {
  const { user } = useAuth()
  const [q, setQ] = useState('')
  const [status, setStatus] = useState('')
  const [priority, setPriority] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [page, setPage] = useState(0)
  const search = useDebounced(q)
  const cats = useAsync(listCategories, [])
  const fixedKey = JSON.stringify(fixed)

  useEffect(() => setPage(0), [search, status, priority, categoryId, fixedKey])

  const data = useAsync(
    () => listTickets({ ...fixed, search: search.trim(), status, priority, categoryId, page, size: LIMITS.pageSize }),
    [search, status, priority, categoryId, page, fixedKey],
  )
  const statusOptions = statuses || STATUS_KEYS
  const clear = () => { setQ(''); setStatus(''); setPriority(''); setCategoryId('') }

  return (
    <section className="card">
      <div className="ch"><h2>{title}</h2></div>
      <div className="filters">
        <div className="sw">
          <Icon name="search" />
          <input className="in" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Tìm theo tiêu đề hoặc mô tả" aria-label="Tìm kiếm ticket" />
        </div>
        <select className="sel" value={status} onChange={(e) => setStatus(e.target.value)} aria-label="Lọc theo trạng thái">
          <option value="">Mọi trạng thái</option>
          {statusOptions.map((s) => <option key={s} value={s}>{STATUS[s].label}</option>)}
        </select>
        <select className="sel" value={priority} onChange={(e) => setPriority(e.target.value)} aria-label="Lọc theo mức ưu tiên">
          <option value="">Mọi mức ưu tiên</option>
          {PRIORITY_KEYS.map((p) => <option key={p} value={p}>{PRIORITY[p].label}</option>)}
        </select>
        <select className="sel" value={categoryId} onChange={(e) => setCategoryId(e.target.value)} aria-label="Lọc theo danh mục">
          <option value="">Mọi danh mục</option>
          {(cats.data || []).map((c) => <option key={c.id} value={c.id}>{c.categoryName}</option>)}
        </select>
      </div>
      <Async state={data}>
        {data.data && (
          <>
            <TicketTable tickets={data.data.content} categories={cats.data || []} showSla={user.role !== ROLES.EMPLOYEE} onClear={clear} />
            <Pagination page={data.data.page} totalPages={data.data.totalPages} totalElements={data.data.totalElements} size={data.data.size || LIMITS.pageSize} onPage={setPage} />
          </>
        )}
      </Async>
    </section>
  )
}
