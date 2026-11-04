import { ROLES } from './constants'

// Menu theo vai trò, giữ nguyên thứ tự và tên như prototype.
// path: đường dẫn route; sub: mục con của "Cài đặt hệ thống"; title/subtitle: tiêu đề trang.
const E = ROLES.EMPLOYEE
const S = ROLES.STAFF
const M = ROLES.MANAGER
const A = ROLES.ADMIN

export const NAV = {
  [E]: [
    { path: '/employee', label: 'Tổng quan', icon: 'home', subtitle: 'Theo dõi nhanh các yêu cầu hỗ trợ bạn đã gửi.' },
    { path: '/employee/tickets', label: 'Ticket của tôi', icon: 'ticket', subtitle: 'Tất cả yêu cầu hỗ trợ bạn đã gửi.' },
    { path: '/employee/tickets/new', label: 'Tạo ticket', icon: 'plus', subtitle: 'Mô tả sự cố để đội IT tiếp nhận và xử lý.' },
  ],
  [S]: [
    { path: '/staff', label: 'Tổng quan', icon: 'home', subtitle: 'Ticket được giao cho bạn và các cảnh báo SLA.' },
    { path: '/staff/tickets', label: 'Ticket được giao', icon: 'inbox', subtitle: 'Các ticket đang thuộc trách nhiệm của bạn.' },
    { path: '/staff/sla', label: 'Giám sát SLA', icon: 'clock', subtitle: 'Thời hạn xử lý của các ticket đang giao cho bạn.' },
  ],
  [M]: [
    { path: '/manager', label: 'Tổng quan', icon: 'home', subtitle: 'Hàng đợi ticket, tình trạng SLA và yêu cầu mở lại.' },
    { path: '/manager/tickets', label: 'Quản lý ticket', icon: 'list', subtitle: 'Toàn bộ ticket, phân công và phân công lại người xử lý.' },
    { path: '/manager/sla', label: 'Giám sát SLA', icon: 'clock', subtitle: 'Thời hạn xử lý của mọi ticket đang mở.' },
    { path: '/manager/reopen-requests', label: 'Yêu cầu mở lại', icon: 'refresh', subtitle: 'Xem xét lý do và quyết định mở lại ticket đã đóng.' },
  ],
  [A]: [
    { path: '/admin', label: 'Tổng quan', icon: 'home', subtitle: 'Người dùng, cấu hình hệ thống và nhật ký chỉ đọc.' },
    { path: '/admin/users', label: 'Người dùng', icon: 'users', subtitle: 'Tạo tài khoản, gán vai trò, khóa hoặc kích hoạt tài khoản.' },
    { path: '/admin/settings', label: 'Cài đặt hệ thống', icon: 'sliders', subtitle: 'Chọn nhóm cấu hình cần quản lý.' },
    { path: '/admin/categories', label: 'Danh mục', icon: 'tag', sub: true, subtitle: 'Phân loại yêu cầu hiển thị khi nhân viên tạo ticket.' },
    { path: '/admin/priorities', label: 'Mức độ ưu tiên', icon: 'flag', sub: true, subtitle: 'Các mức ưu tiên có thể chọn khi tạo ticket.' },
    { path: '/admin/statuses', label: 'Trạng thái', icon: 'layers', sub: true, subtitle: 'Tên hiển thị của các trạng thái trong vòng đời ticket.' },
    { path: '/admin/sla-config', label: 'Cấu hình SLA', icon: 'clock', sub: true, subtitle: 'Định mức giờ phản hồi và giờ xử lý theo mức ưu tiên.' },
    { path: '/admin/audit-logs', label: 'Nhật ký hệ thống', icon: 'file', subtitle: 'Bản ghi chỉ đọc về các thao tác trên hệ thống.' },
  ],
}

// Mục menu được xem là "đang chọn" khi đường dẫn hiện tại trùng hoặc nằm dưới nó
// (chọn mục dài nhất khớp để /employee/tickets/new không làm sáng /employee/tickets).
export function findNavItem(role, pathname) {
  const items = NAV[role] || []
  const hit = items
    .filter((i) => pathname === i.path || pathname.startsWith(i.path + '/'))
    .sort((a, b) => b.path.length - a.path.length)
  return hit[0] || null
}

// Mục "Ticket" của mỗi vai trò, dùng làm đường dẫn cha trong breadcrumb chi tiết ticket
export function ticketListItem(role) {
  return (NAV[role] || []).find((i) => /\/tickets$/.test(i.path)) || null
}
