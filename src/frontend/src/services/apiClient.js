import axios from 'axios'
import { API_BASE_URL, STORAGE_KEYS } from '@/config/constants'
import { emitForbidden, emitUnauthorized } from './authEvents'

const apiClient = axios.create({ baseURL: API_BASE_URL, timeout: 15000 })

apiClient.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(STORAGE_KEYS.token)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

apiClient.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response?.status
    // Đăng nhập sai cũng trả 401 nhưng đó là lỗi của form, không phải hết phiên
    const isLogin = err.config?.url?.includes('/auth/login')
    if (status === 401 && !isLogin) emitUnauthorized()
    if (status === 403) emitForbidden()
    return Promise.reject(err)
  },
)

// Bóc dữ liệu theo ApiResponse<T> { success, message, data, timestamp } (SDD §4.2)
export const unwrap = (res) => res.data?.data ?? res.data

// Lấy thông báo lỗi cho người dùng từ ErrorResponse { success:false, error:{ code, message, details } }
export function apiError(err, fallback = 'Có lỗi xảy ra, vui lòng thử lại.') {
  if (!err.response) return 'Không kết nối được máy chủ. Vui lòng thử lại sau.'
  return err.response.data?.error?.message || fallback
}

export default apiClient
