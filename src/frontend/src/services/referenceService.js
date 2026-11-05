import api, { normalizePage, unwrap } from './apiClient'

export const listCategories = async () => unwrap(await api.get('/categories')) || []
export const listItStaff = async () => unwrap(await api.get('/users/it-staff')) || []

export const listReopenRequests = async (status = 'PENDING') => {
  const d = unwrap(await api.get('/reopen-requests', { params: { status } }))
  return Array.isArray(d) ? d : d?.content || []
}
export const approveReopen = async (id) => unwrap(await api.post(`/reopen-requests/${id}/approve`))
export const rejectReopen = async (id, reviewNote) => unwrap(await api.post(`/reopen-requests/${id}/reject`, { reviewNote }))

// ---- Admin ----
export const adminListUsers = async (params = {}) => normalizePage(unwrap(await api.get('/admin/users', { params })))
export const adminCreateUser = async (body) => unwrap(await api.post('/admin/users', body))
export const adminSetRole = async (id, role) => unwrap(await api.patch(`/admin/users/${id}/role`, { role }))
export const adminSetStatus = async (id, isActive) => unwrap(await api.patch(`/admin/users/${id}/status`, { isActive }))

export const adminListCategories = async () => unwrap(await api.get('/admin/categories')) || []
export const adminCreateCategory = async (body) => unwrap(await api.post('/admin/categories', body))
export const adminUpdateCategory = async (id, body) => unwrap(await api.put(`/admin/categories/${id}`, body))
export const adminDeleteCategory = async (id) => unwrap(await api.delete(`/admin/categories/${id}`))
export const adminSetCategoryActive = async (id, active) => unwrap(await api.patch(`/admin/categories/${id}/status`, { active }))

export const listSlaConfigs = async () => unwrap(await api.get('/admin/sla-configs')) || []
export const updateSlaConfig = async (priority, body) => unwrap(await api.put(`/admin/sla-configs/${priority}`, body))

// Chưa có trong openapi: đề xuất GET /admin/audit-logs (xem README, mục "Điểm cần thống nhất")
export const listAuditLogs = async (params = {}) => normalizePage(unwrap(await api.get('/admin/audit-logs', { params })))
