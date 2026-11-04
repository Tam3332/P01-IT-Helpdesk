export default function Pagination({ page, totalPages, totalElements, size, onPage }) {
  if (!totalElements) return null
  const from = page * size + 1
  const to = Math.min(totalElements, (page + 1) * size)
  return (
    <div className="pager">
      <span className="sm muted num">
        {from}–{to} / {totalElements}
      </span>
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
