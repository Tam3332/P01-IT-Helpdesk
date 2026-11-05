export default function Pagination({ page, totalPages, totalElements, size, onPage }) {
  if (!totalElements) return null
  const from = page * size + 1
  const to = Math.min(totalElements, (page + 1) * size)
  const pad = (n) => String(n).padStart(2, '0')
  const label = totalElements <= size
    ? `${pad(from)}/${pad(totalElements)}`
    : `${pad(from)}–${pad(to)} / ${pad(totalElements)}`

  return (
    <div className="pager">
      <span className="sm muted num">{label}</span>
      <div className="btns">
        <button type="button" className="btn sm" disabled={page <= 0} onClick={() => onPage(page - 1)}>
          Trước
        </button>
        <span className="sm muted" style={{ alignSelf: 'center' }}>
          Trang {page + 1}/{Math.max(1, totalPages)}
        </span>
        <button type="button" className="btn sm" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>
          Sau
        </button>
      </div>
    </div>
  )
}
