export default function Loading({ text = 'Đang tải...' }) {
  return (
    <div className="empty" role="status">
      <span className="muted">{text}</span>
    </div>
  )
}
