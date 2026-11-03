import Icon from './Icon'

export const Spinner = ({ text = 'Đang tải...' }) => (
  <div className="empty" role="status">
    <span className="spin" aria-hidden="true" />
    <span>{text}</span>
  </div>
)

export const EmptyState = ({ icon = 'search', title, children, action }) => (
  <div className="empty">
    <div className="ico">
      <Icon name={icon} />
    </div>
    <b>{title}</b>
    {children && <span>{children}</span>}
    {action}
  </div>
)

// status = mã HTTP (0 = mất kết nối). 404 từ API chưa có -> thông báo riêng.
export function ErrorState({ message, status, onRetry, notReadyText }) {
  const notReady = status === 404 && notReadyText
  return (
    <div className="empty" role="alert">
      <div className="ico">
        <Icon name={notReady ? 'info' : 'alert'} />
      </div>
      <b>{notReady ? 'API chưa sẵn sàng' : 'Không tải được dữ liệu'}</b>
      <span>{notReady ? notReadyText : message}</span>
      {onRetry && (
        <button type="button" className="btn sm" onClick={onRetry}>
          <Icon name="refresh" />
          Thử lại
        </button>
      )}
    </div>
  )
}

// Gom 3 trạng thái tải/lỗi/rỗng cho các thẻ dữ liệu
export function Async({ state, children, notReadyText }) {
  if (state.loading && !state.data) return <Spinner />
  if (state.error && !state.data) return <ErrorState message={state.error} status={state.status} onRetry={state.reload} notReadyText={notReadyText} />
  return children
}
