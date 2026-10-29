import dayjs from 'dayjs'

export const accounts = [
  { username: 'employee', password: '123456', fullName: 'Nguyễn Văn An', role: 'EMPLOYEE', dept: 'Kế toán' },
  { username: 'staff', password: '123456', fullName: 'Trần Thị Bình', role: 'IT_STAFF', dept: 'IT' },
  { username: 'manager', password: '123456', fullName: 'Lê Minh Cường', role: 'IT_MANAGER', dept: 'IT' },
  { username: 'admin', password: '123456', fullName: 'Phạm Quốc Dũng', role: 'ADMIN', dept: 'IT' },
  { username: 'locked', password: '123456', fullName: 'Tài khoản bị khóa', role: 'EMPLOYEE', dept: 'Kinh doanh', locked: true },
]

const h = (n) => dayjs().subtract(n, 'hour').toISOString()

export const categories = ['Phần cứng', 'Phần mềm', 'Mạng', 'Tài khoản & quyền', 'Khác']

export const tickets = [
  { id: 'TK-0001', title: 'Máy in tầng 3 không in được', category: 'Phần cứng', priority: 'MEDIUM', status: 'NEW', creator: 'employee', assignee: null, createdAt: h(2), elapsed: 2, limit: 24 },
  { id: 'TK-0002', title: 'Không truy cập được VPN từ nhà', category: 'Mạng', priority: 'HIGH', status: 'ASSIGNED', creator: 'employee', assignee: 'staff', createdAt: h(7), elapsed: 7, limit: 8 },
  { id: 'TK-0003', title: 'Cài đặt Office cho máy mới', category: 'Phần mềm', priority: 'LOW', status: 'IN_PROGRESS', creator: 'employee', assignee: 'staff', createdAt: h(10), elapsed: 10, limit: 48 },
  { id: 'TK-0004', title: 'Quên mật khẩu hệ thống ERP', category: 'Tài khoản & quyền', priority: 'CRITICAL', status: 'IN_PROGRESS', creator: 'employee', assignee: 'staff', createdAt: h(6), elapsed: 6, limit: 4 },
  { id: 'TK-0005', title: 'Laptop bị chậm, treo thường xuyên', category: 'Phần cứng', priority: 'MEDIUM', status: 'RESOLVED', creator: 'employee', assignee: 'staff', createdAt: h(30), elapsed: 20, limit: 24 },
  { id: 'TK-0006', title: 'Xin cấp quyền thư mục dùng chung', category: 'Tài khoản & quyền', priority: 'LOW', status: 'CLOSED', creator: 'employee', assignee: 'staff', createdAt: h(90), elapsed: 12, limit: 48 },
]

export const reopenRequests = [
  { id: 'RO-001', ticketId: 'TK-0006', requester: 'employee', reason: 'Vẫn chưa truy cập được thư mục sau khi được cấp quyền, cần kiểm tra lại.', createdAt: h(5), status: 'PENDING' },
]

export const auditLogs = [
  { id: 1, time: h(1), actor: 'admin', action: 'UPDATE_SLA', target: 'HIGH', detail: 'resolve 8h' },
  { id: 2, time: h(3), actor: 'manager', action: 'ASSIGN_TICKET', target: 'TK-0002', detail: '-> staff' },
  { id: 3, time: h(8), actor: 'locked', action: 'LOGIN_LOCKED', target: 'locked', detail: '>5 lần sai' },
]

export const users = accounts.map(({ password: _p, ...u }) => u)
