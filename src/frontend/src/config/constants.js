export const ROLES = {
  EMPLOYEE: 'EMPLOYEE',
  STAFF: 'IT_STAFF',
  MANAGER: 'IT_MANAGER',
  ADMIN: 'ADMIN',
}

export const ROLE_LABEL = {
  EMPLOYEE: 'Nhân viên',
  IT_STAFF: 'IT Staff',
  IT_MANAGER: 'IT Manager',
  ADMIN: 'Admin',
}

// Trang chủ sau đăng nhập theo role
export const ROLE_HOME = {
  EMPLOYEE: '/employee',
  IT_STAFF: '/staff',
  IT_MANAGER: '/manager',
  ADMIN: '/admin',
}

export const STATUS_META = {
  NEW: { label: 'Mới', color: 'blue' },
  ASSIGNED: { label: 'Đã phân công', color: 'purple' },
  IN_PROGRESS: { label: 'Đang xử lý', color: 'orange' },
  RESOLVED: { label: 'Đã giải quyết', color: 'green' },
  CLOSED: { label: 'Đã đóng', color: 'default' },
}

export const PRIORITY_META = {
  CRITICAL: { label: 'Critical', color: 'red' },
  HIGH: { label: 'High', color: 'orange' },
  MEDIUM: { label: 'Medium', color: 'gold' },
  LOW: { label: 'Low', color: 'default' },
}

// SLA: Normal < 80%, Near Breach 80-100%, Breached > 100%
export const SLA_META = {
  NORMAL: { label: 'Bình thường', color: 'green' },
  NEAR_BREACH: { label: 'Sắp vi phạm', color: 'orange' },
  BREACHED: { label: 'Vi phạm', color: 'red' },
}
export const SLA_THRESHOLDS = { NEAR: 0.8, BREACH: 1 }

// Cấu hình SLA mặc định (giờ): [phản hồi, xử lý]
export const SLA_DEFAULT = {
  CRITICAL: { response: 0.25, resolve: 4 },
  HIGH: { response: 0.5, resolve: 8 },
  MEDIUM: { response: 1, resolve: 24 },
  LOW: { response: 2, resolve: 48 },
}

export const FILE_RULES = {
  maxFiles: 5,
  maxSizeMB: 10,
  extensions: ['jpg', 'jpeg', 'png', 'gif', 'pdf', 'docx', 'xlsx', 'txt', 'log'],
}

export const REOPEN_RULES = { windowDays: 7, minReasonLength: 20 }
export const PAGE_SIZE = 20
export const STORAGE_KEYS = { token: 'hd_token', user: 'hd_user' }
export const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
