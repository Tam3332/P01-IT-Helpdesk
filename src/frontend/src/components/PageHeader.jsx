// Tiêu đề trang theo prototype: h1 + mô tả ngắn, nút thao tác ở bên phải
export default function PageHeader({ title, subtitle, children }) {
  return (
    <div className="phead">
      <div>
        <h1>{title}</h1>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {children && <div className="acts">{children}</div>}
    </div>
  )
}
