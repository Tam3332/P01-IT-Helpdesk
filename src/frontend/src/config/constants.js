// Tên vai trò trùng với Backend (JWT payload, bảng roles)
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
  ADMIN: 'Quản trị viên',
}

// Trang chủ sau đăng nhập theo vai trò
export const ROLE_HOME = {
  EMPLOYEE: '/employee',
  IT_STAFF: '/staff',
  IT_MANAGER: '/manager',
  ADMIN: '/admin',
}

// Access token sống 15 phút (ADR-02), lưu trong sessionStorage: đóng tab là mất phiên
export const STORAGE_KEYS = { token: 'hd_token', expiresAt: 'hd_token_exp' }

// Mọi API nghiệp vụ nằm dưới /api/v1 (SDD §4.1). Dev: Vite proxy; Docker: Nginx gateway.
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
