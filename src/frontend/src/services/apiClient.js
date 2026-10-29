import axios from 'axios'
import { API_BASE_URL, STORAGE_KEYS } from '@/config/constants'

const apiClient = axios.create({ baseURL: API_BASE_URL, timeout: 15000 })

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(STORAGE_KEYS.token)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

apiClient.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response?.status
    if (status === 401) {
      localStorage.removeItem(STORAGE_KEYS.token)
      localStorage.removeItem(STORAGE_KEYS.user)
      if (location.pathname !== '/login') location.assign('/login')
    } else if (status === 403) {
      location.assign('/403')
    }
    return Promise.reject(err)
  },
)

export default apiClient
