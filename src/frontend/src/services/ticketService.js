import api, { clean, normalizePage, unwrap } from './apiClient'
import { OPEN_STATUSES, STATUS_KEYS, TICKET_REF_FIELD } from '@/config/domain'

export const ticketRef = (t) => t?.[TICKET_REF_FIELD] ?? t?.id

export async function listTickets(params = {}) {
  const res = await api.get('/tickets', { params: clean(params) })
  return normalizePage(unwrap(res))
}

// Lấy toàn bộ ticket khớp bộ lọc (dùng cho tổng quan/giám sát SLA khi chưa có API dashboard HD14)
export async function listAllTickets(params = {}, { pageSize = 100, maxPages = 10 } = {}) {
  const out = []
  let page = 0
  let totalPages = 1
  do {
    const r = await listTickets({ ...params, page, size: pageSize })
    out.push(...r.content)
    totalPages = r.totalPages
    page += 1
  } while (page < totalPages && page < maxPages)
  return out
}

export async function countTickets(params = {}) {
  const r = await listTickets({ ...params, page: 0, size: 1 })
  return r.totalElements
}

export const getTicket = async (ref) => unwrap(await api.get(`/tickets/${encodeURIComponent(ref)}`))
export const createTicket = async (body) => unwrap(await api.post('/tickets', body))
export const assignTicket = async (ref, body) => unwrap(await api.put(`/tickets/${encodeURIComponent(ref)}/assign`, body))
export const changeStatus = async (ref, body) => unwrap(await api.patch(`/tickets/${encodeURIComponent(ref)}/status`, body))
export const closeTicket = async (ref) => unwrap(await api.post(`/tickets/${encodeURIComponent(ref)}/close`))
export const reopenTicket = async (ref, reopenReason) => unwrap(await api.post(`/tickets/${encodeURIComponent(ref)}/reopen`, { reopenReason }))

export const listComments = async (ref) => unwrap(await api.get(`/tickets/${encodeURIComponent(ref)}/comments`)) || []
export const addComment = async (ref, body) => unwrap(await api.post(`/tickets/${encodeURIComponent(ref)}/comments`, body))

export const listAttachments = async (ref) => unwrap(await api.get(`/tickets/${encodeURIComponent(ref)}/attachments`)) || []
export async function uploadAttachment(ref, file) {
  const form = new FormData()
  form.append('file', file)
  return unwrap(await api.post(`/tickets/${encodeURIComponent(ref)}/attachments`, form, { timeout: 60000 }))
}
// Tải tệp bằng blob (cần gửi kèm Bearer token nên không dùng thẻ <a href> trực tiếp)
export async function downloadAttachment(ref, attachment) {
  const res = await api.get(`/tickets/${encodeURIComponent(ref)}/attachments/${attachment.id}`, { responseType: 'blob', timeout: 60000 })
  const url = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = url
  a.download = attachment.fileName || 'attachment'
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export const listHistory = async (ref) => unwrap(await api.get(`/tickets/${encodeURIComponent(ref)}/history`)) || []
export const getFeedback = async (ref) => unwrap(await api.get(`/tickets/${encodeURIComponent(ref)}/feedback`))
export const sendFeedback = async (ref, body) => unwrap(await api.post(`/tickets/${encodeURIComponent(ref)}/feedback`, body))


// Số ticket theo từng trạng thái (đã được backend giới hạn theo quyền của người gọi)
export async function getStatusCounts() {
  const entries = await Promise.all(STATUS_KEYS.map(async (s) => [s, await countTickets({ status: s })]))
  return Object.fromEntries(entries)
}

// Toàn bộ ticket đang mở. Dùng cho tổng quan/giám sát SLA cho tới khi có API dashboard (HD14).
export async function fetchOpenTickets() {
  const lists = await Promise.all(OPEN_STATUSES.map((status) => listAllTickets({ status })))
  return lists.flat()
}
