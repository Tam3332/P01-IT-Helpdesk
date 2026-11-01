import apiClient, { unwrap } from './apiClient'

// POST /auth/login  { email, password } -> LoginResponse { accessToken, tokenType, expiresIn, user }
export async function login(email, password) {
  return unwrap(await apiClient.post('/auth/login', { email, password }))
}

// GET /auth/me -> UserResponse (xác nhận token còn hiệu lực, lấy thông tin tài khoản hiện tại)
export async function fetchMe() {
  return unwrap(await apiClient.get('/auth/me'))
}

// POST /auth/logout: đưa token vào blacklist phía Backend (ADR-02)
export async function logout() {
  await apiClient.post('/auth/logout')
}
