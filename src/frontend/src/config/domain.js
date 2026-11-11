// Hằng số nghiệp vụ lấy từ SDD/SRS và openapi.yaml. Đây là quy tắc hệ thống, không phải dữ liệu mẫu.

// Trường dùng làm mã ticket trong URL và trong đường dẫn API (/tickets/{ref}).
// Backend hiện nhận mã dạng HD-2026-0001 (ticketNumber). Nếu đổi sang id số, chỉ cần đổi dòng này.
export const TICKET_REF_FIELD = 'ticketNumber'

export const STATUS = {
  NEW: { label: 'Mới', badge: 'b-New' },
  ASSIGNED: { label: 'Đã phân công', badge: 'b-Assigned' },
  IN_PROGRESS: { label: 'Đang xử lý', badge: 'b-InProgress' },
  RESOLVED: { label: 'Đã giải quyết', badge: 'b-Resolved' },
  CLOSED: { label: 'Đã đóng', badge: 'b-Closed' },
  REOPENED: { label: 'Mở lại', badge: 'b-Reopened' },
  REOPEN_PENDING: { label: 'Chờ duyệt mở lại', badge: 'b-ReopenPending' },
}
export const STATUS_KEYS = Object.keys(STATUS)
// Ticket còn đang xử lý (tính SLA, hiện ở "đang mở")
export const OPEN_STATUSES = ['NEW', 'ASSIGNED', 'IN_PROGRESS', 'REOPENED']
// Các mốc hiển thị ở thanh tiến trình trên trang chi tiết
export const LIFECYCLE = ['NEW', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED']

export const PRIORITY = {
  LOW: { label: 'Thấp', badge: 'p-low', level: 1 },
  MEDIUM: { label: 'Trung bình', badge: 'p-medium', level: 2 },
  HIGH: { label: 'Cao', badge: 'p-high', level: 3 },
  CRITICAL: { label: 'Nghiêm trọng', badge: 'p-critical', level: 4 },
}
export const PRIORITY_KEYS = Object.keys(PRIORITY)

export const SLA = {
  NORMAL: { label: 'Bình thường', badge: 's-normal', color: 'var(--ok)' },
  NEAR_BREACH: { label: 'Sắp quá hạn', badge: 's-near', color: 'var(--warn)' },
  BREACHED: { label: 'Quá hạn', badge: 's-breach', color: 'var(--bad)' },
  DONE: { label: 'Đúng hạn', badge: 's-done', color: 'var(--ok)' },
  LATE: { label: 'Trễ hạn', badge: 's-late', color: 'var(--bad)' },
}

export const LIMITS = {
  passwordMin: 8, // TẠM, chờ quy tắc mật khẩu của backend
  passwordMax: 128,
  title: 200,
  description: 2000,
  comment: 2000,
  feedbackComment: 500,
  resolutionNoteMin: 20, // BR-HD-01
  reopenReasonMin: 20, // BR-HD-04
  reopenWindowHours: 168, // 7 ngày kể từ lúc Resolved
  rejectNoteMin: 10,
  reviewNoteMax: 1000,
  maxFiles: 5,
  maxFileBytes: 10 * 1024 * 1024,
  pageSize: 20, // HD13
}
export const ALLOWED_EXT = ['jpg', 'jpeg', 'png', 'gif', 'pdf', 'docx', 'xlsx', 'txt', 'log']

export const statusLabel = (s) => STATUS[s]?.label || s || '—'
export const priorityLabel = (p) => PRIORITY[p]?.label || p || '—'
