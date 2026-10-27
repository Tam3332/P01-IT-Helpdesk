import apiClient from './apiClient'
import { USE_MOCK } from '@/config/constants'
import { accounts } from '@/mocks/data'

const delay = (ms = 300) => new Promise((r) => setTimeout(r, ms))

// Hợp đồng API giả định: POST /api/auth/login {username,password} -> {token,user:{username,fullName,role}}
export async function login(username, password) {
  if (USE_MOCK) {
    await delay()
    const acc = accounts.find((a) => a.username === username)
    if (acc?.locked) throw new Error('Tài khoản bị khóa 15 phút do đăng nhập sai quá 5 lần')
    if (!acc || acc.password !== password) throw new Error('Sai tên đăng nhập hoặc mật khẩu')
    return {
      token: `mock-jwt-${acc.username}`,
      user: { username: acc.username, fullName: acc.fullName, role: acc.role },
    }
  }
  const { data } = await apiClient.post('/auth/login', { username, password })
  return data
}
