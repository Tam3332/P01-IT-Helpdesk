import api, { unwrap } from './apiClient'

/**
 * Lấy danh sách thông báo biến động vé gần đây của người dùng hiện tại (Employee, Staff, Manager).
 */
export async function getNotifications(limit = 15) {
  try {
    return unwrap(await api.get(`/notifications?limit=${limit}`))
  } catch (e) {
    console.error('Không thể tải thông báo:', e)
    return []
  }
}
