import api, { unwrap } from './apiClient'

// LoginResponse: openapi để accessToken ở cấp ngoài cùng, backend có thể bọc trong data.
export async function login(email, password) {
  const res = await api.post('/auth/login', { email, password })
  const body = res.data
  return body?.accessToken ? body : unwrap(res)
}

export const fetchMe = async () => unwrap(await api.get('/auth/me'))
export const logout = async () => api.post('/auth/logout')

// TẠM: backend chưa có API đổi mật khẩu (tri/be chỉ có login, me, logout). Khi Trí chốt endpoint thật,
// chỉ cần sửa đường dẫn và tên trường ở đây.
export const changePassword = async (currentPassword, newPassword) =>
  unwrap(await api.post('/auth/change-password', { currentPassword, newPassword }))
