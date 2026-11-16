import { useEffect, useRef, useState } from 'react'
import { useLocation } from 'react-router-dom'
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
import { t } from '@/i18n'

const COLS = [
  ['requester', 'Người yêu cầu'], ['category', 'Danh mục'], ['priority', 'Ưu tiên'],
  ['status', 'Trạng thái'], ['assignee', 'Người xử lý'], ['created', 'Ngày tạo'], ['sla', 'SLA'],
]
const read = (store, key, dflt) => {
  try {
    const v = JSON.parse(store.getItem(key))
    return v ?? dflt
  } catch {
    return dflt
  }
}
const write = (store, key, v) => {
  try {
    store.setItem(key, JSON.stringify(v))
  } catch {
    /* không lưu được thì bỏ qua */
  }
}

// Thẻ danh sách ticket: tìm kiếm, lọc, phân trang, đều gọi API (HD05, HD13).
// fixed: tham số lọc cố định (ví dụ chỉ ticket đang mở). title: tiêu đề thẻ.
export default function TicketListCard({ title, fixed = {}, statuses }) {
  const { user } = useAuth()
  const { pathname, search: query } = useLocation()
  // Bộ lọc/tìm kiếm/trang được nhớ theo vai trò và trang (sessionStorage); ?priority=… / ?status=… trên URL ưu tiên hơn.
  const fKey = `hd_filters_${user.role}_${pathname}`
  const [init] = useState(() => {
    const saved = read(sessionStorage, fKey, {})
    const qs = new URLSearchParams(query)
    const o = { q: '', status: '', priority: '', categoryId: '', page: 0, ...saved }
    const fromUrl = ['status', 'priority'].filter((k) => qs.get(k))
    fromUrl.forEach((k) => { o[k] = qs.get(k) })
    if (fromUrl.length) o.page = 0
    return o
  })
  const [q, setQ] = useState(init.q)
  const [status, setStatus] = useState(init.status)
  const [priority, setPriority] = useState(init.priority)
  const [categoryId, setCategoryId] = useState(init.categoryId)
  const [page, setPage] = useState(init.page)
  const first = useRef(true)
  const [hidden, setHidden] = useState(() => read(localStorage, `hd_cols_${user.role}_${pathname}`, []))
  const [colMenu, setColMenu] = useState(false)
  const colRef = useRef(null)
  const search = useDebounced(q)
  const cats = useAsync(listCategories, [])
  const fixedKey = JSON.stringify(fixed)

  useEffect(() => {
    if (first.current) { first.current = false; return }
    setPage(0)
  }, [search, status, priority, categoryId, fixedKey])
  useEffect(() => { write(sessionStorage, fKey, { q, status, priority, categoryId, page }) }, [fKey, q, status, priority, categoryId, page])
  useEffect(() => {
    if (!colMenu) return undefined
    const off = (e) => { if (!colRef.current?.contains(e.target)) setColMenu(false) }
    document.addEventListener('mousedown', off)
    return () => document.removeEventListener('mousedown', off)
  }, [colMenu])
  const toggleCol = (k) => {
    const next = hidden.includes(k) ? hidden.filter((x) => x !== k) : [...hidden, k]
    setHidden(next)
    write(localStorage, `hd_cols_${user.role}_${pathname}`, next)
  }
  const filtering = !!(q || status || priority || categoryId)

  const data = useAsync(
    () => listTickets({ ...fixed, search: search.trim(), status, priority, categoryId, page, size: LIMITS.pageSize }),
    [search, status, priority, categoryId, page, fixedKey],
  )
  const statusOptions = statuses || STATUS_KEYS
  const clear = () => { setQ(''); setStatus(''); setPriority(''); setCategoryId('') }

  return (
    <section className="card">
      <div className="ch">
        <h2>{title}</h2>
        <div className="acts" style={{ position: 'relative' }} ref={colRef}>
          {filtering && <button type="button" className="btn ghost sm" onClick={clear}><Icon name="x" />{t('Xóa bộ lọc')}</button>}
          <button type="button" className="btn sm" aria-haspopup="true" aria-expanded={colMenu} onClick={() => setColMenu((o) => !o)}><Icon name="sliders" />{t('Cột')}</button>
          {colMenu && (
            <div className="menu colmenu" role="group" aria-label={t('Cột hiển thị')}>
              {COLS.filter(([k]) => k !== 'sla' || user.role !== ROLES.EMPLOYEE).map(([k, label]) => (
                <label key={k} className="chk"><input type="checkbox" checked={!hidden.includes(k)} onChange={() => toggleCol(k)} /> {k === 'sla' ? label : t(label)}</label>
              ))}
            </div>
          )}
        </div>
      </div>
      <div className="filters">
        <div className={`sw${q ? ' has' : ''}`}>
          <Icon name="search" />
          <input className="in" value={q} onChange={(e) => setQ(e.target.value)} placeholder={t('Tìm theo tiêu đề hoặc mô tả')} aria-label={t('Tìm kiếm ticket')} />
          <button type="button" className="clr" aria-label={t('Xóa từ khóa')} onClick={() => setQ('')}><Icon name="x" /></button>
        </div>
        <select className="sel" value={status} onChange={(e) => setStatus(e.target.value)} aria-label={t('Lọc theo trạng thái')}>
          <option value="">{t('Mọi trạng thái')}</option>
          {statusOptions.map((s) => <option key={s} value={s}>{t(STATUS[s].label)}</option>)}
        </select>
        <select className="sel" value={priority} onChange={(e) => setPriority(e.target.value)} aria-label={t('Lọc theo mức ưu tiên')}>
          <option value="">{t('Mọi mức ưu tiên')}</option>
          {PRIORITY_KEYS.map((p) => <option key={p} value={p}>{t(PRIORITY[p].label)}</option>)}
        </select>
        <select className="sel" value={categoryId} onChange={(e) => setCategoryId(e.target.value)} aria-label={t('Lọc theo danh mục')}>
          <option value="">{t('Mọi danh mục')}</option>
          {(cats.data || []).map((c) => <option key={c.id} value={c.id}>{c.categoryName}</option>)}
        </select>
      </div>
      <Async state={data}>
        {data.data && (
          <>
            <TicketTable tickets={data.data.content} categories={cats.data || []} showSla={user.role !== ROLES.EMPLOYEE} onClear={clear} hidden={hidden} highlight={search} />
            <Pagination page={data.data.page} totalPages={data.data.totalPages} totalElements={data.data.totalElements} size={data.data.size || LIMITS.pageSize} onPage={setPage} />
          </>
        )}
      </Async>
    </section>
  )
}
