import api, { unwrap } from './apiClient'

/**
 * Lấy số liệu thống kê tổng hợp cho Dashboard (HD14).
 * Backend tự động phân quyền theo vai trò (Employee, Staff, Manager, Admin).
 */
export async function getDashboardStats() {
  const res = await api.get('/dashboard/stats')
  return unwrap(res)
}

/**
 * Lấy danh sách vé cần giám sát cảnh báo SLA (HD12).
 * @param {boolean} breachedOnly true = chỉ lấy vé đã vi phạm SLA, false = cả vé sắp quá hạn và bình thường
 */
export async function getSlaMonitor(breachedOnly = false) {
  const res = await api.get('/dashboard/sla-monitor', {
    params: { breachedOnly },
  })
  return unwrap(res) || []
}
