import axios from 'axios'
import { API_BASE_URL, STORAGE_KEYS } from '@/config/constants'
import { LIMITS } from '@/config/domain'
import { emitForbidden, emitUnauthorized } from './authEvents'

const api = axios.create({ baseURL: API_BASE_URL, timeout: 15000 })

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(STORAGE_KEYS.token)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response?.status
    const url = err.config?.url || ''
    if (status === 401 && !url.includes('/auth/login') && !url.includes('/auth/change-password')) emitUnauthorized()
    if (status === 403) emitForbidden()
    return Promise.reject(err)
  },
)

export default api

// Bóc dữ liệu. Chấp nhận cả hai kiểu phản hồi đang có trong dự án:
//  - SDD/openapi: { success, message, data, timestamp }  (riêng LoginResponse có accessToken ở ngoài)
//  - Backend tri/be hiện tại: { code, message, data, timestamp }
export function unwrap(res) {
  const body = res?.data
  if (body && typeof body === 'object' && 'data' in body) return body.data
  return body
}

// Lấy thông điệp lỗi từ cả hai kiểu: { error: { code, message, details } } hoặc { code, message }
export function apiError(err, fallback = 'Đã xảy ra lỗi. Vui lòng thử lại.') {
  if (!err?.response) return 'Không kết nối được máy chủ. Vui lòng kiểm tra mạng và thử lại.'
  const body = err.response.data
  const e = body?.error
  const details = Array.isArray(e?.details) ? e.details : body?.data && typeof body.data === 'object' && !Array.isArray(body.data) ? Object.values(body.data) : []
  const msg = e?.message || body?.message
  if (details.length) return `${msg ? msg + ': ' : ''}${details.join('; ')}`
  if (msg) return msg
  // Nginx chặn trước khi tới backend và trả trang HTML (không có JSON), nên đọc theo mã HTTP
  const status = err.response.status
  if (status === 413) return `Tệp hoặc yêu cầu quá lớn. Mỗi tệp tối đa ${LIMITS.maxFileBytes / 1048576} MB.`
  if (status === 429) return 'Bạn thao tác quá nhanh hoặc quá nhiều lần. Vui lòng đợi một lúc rồi thử lại.'
  return fallback
}

export const httpStatus = (err) => err?.response?.status

// Trang kết quả -> dạng thống nhất { content, page, size, totalElements, totalPages }.
// openapi dùng page/size, backend tri/be dùng pageNo/pageSize.
export function normalizePage(p) {
  const content = Array.isArray(p) ? p : p?.content || []
  return {
    content,
    page: p?.page ?? p?.pageNo ?? 0,
    size: p?.size ?? p?.pageSize ?? content.length,
    totalElements: p?.totalElements ?? content.length,
    totalPages: p?.totalPages ?? 1,
  }
}

// Bỏ tham số rỗng trước khi gửi query
export const clean = (o) => Object.fromEntries(Object.entries(o).filter(([, v]) => v !== '' && v != null))
