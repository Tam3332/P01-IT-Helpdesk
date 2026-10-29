import apiClient from './apiClient'
import { USE_MOCK } from '@/config/constants'
import { tickets, reopenRequests, auditLogs, users } from '@/mocks/data'

const delay = (ms = 250) => new Promise((r) => setTimeout(r, ms))

// Mỗi hàm: mock khi VITE_USE_MOCK=true, ngược lại gọi API Spring Boot.
export async function listTickets(params = {}) {
  if (USE_MOCK) {
    await delay()
    let rows = [...tickets]
    if (params.creator) rows = rows.filter((t) => t.creator === params.creator)
    if (params.assignee) rows = rows.filter((t) => t.assignee === params.assignee)
    if (params.status) rows = rows.filter((t) => t.status === params.status)
    return rows
  }
  const { data } = await apiClient.get('/tickets', { params })
  return data.content ?? data
}

export async function getTicket(id) {
  if (USE_MOCK) {
    await delay()
    return tickets.find((t) => t.id === id) ?? null
  }
  const { data } = await apiClient.get(`/tickets/${id}`)
  return data
}

export async function createTicket(payload) {
  if (USE_MOCK) {
    await delay()
    const t = {
      id: `TK-${String(tickets.length + 1).padStart(4, '0')}`,
      status: 'NEW', assignee: null, elapsed: 0, createdAt: new Date().toISOString(), ...payload,
    }
    tickets.unshift(t)
    return t
  }
  const { data } = await apiClient.post('/tickets', payload)
  return data
}

export async function listReopenRequests() {
  if (USE_MOCK) { await delay(); return reopenRequests }
  const { data } = await apiClient.get('/reopen-requests')
  return data
}

export async function listAuditLogs() {
  if (USE_MOCK) { await delay(); return auditLogs }
  const { data } = await apiClient.get('/audit-logs')
  return data
}

export async function listUsers() {
  if (USE_MOCK) { await delay(); return users }
  const { data } = await apiClient.get('/users')
  return data
}
