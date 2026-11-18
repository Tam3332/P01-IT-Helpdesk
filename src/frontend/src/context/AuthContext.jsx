import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { ROLES, STORAGE_KEYS } from '@/config/constants'
import * as authService from '@/services/authService'
import { onUnauthorized } from '@/services/authEvents'
import { getTokenExpiryMs } from '@/utils/jwt'
import { t } from '@/i18n'

const AuthContext = createContext(null)
const VALID_ROLES = Object.values(ROLES)

// UserResponse -> { id, email, fullName, role }. Chấp nhận role là chuỗi hoặc đối tượng { roleName }.
function normalizeUser(u) {
  if (!u) return null
  const role = typeof u.role === 'string' ? u.role : u.role?.roleName || u.roleName
  const savedAvatar = (() => {
    try {
      return localStorage.getItem(`hd_avatar_${u.id}`) || null
    } catch {
      return null
    }
  })()
  const savedProfile = (() => {
    try {
      return JSON.parse(localStorage.getItem(`hd_profile_${u.id}`) || '{}')
    } catch {
      return {}
    }
  })()
  return {
    id: u.id,
    username: u.username,
    email: u.email,
    fullName: savedProfile.fullName || u.fullName || u.username || u.email || '',
    department: savedProfile.department !== undefined ? savedProfile.department : (u.department || ''),
    phone: savedProfile.phone !== undefined ? savedProfile.phone : (u.phone || ''),
    avatarUrl: savedAvatar || u.avatarUrl || u.avatar || null,
    isActive: u.isActive,
    mustChangePassword: u.mustChangePassword === true, // backend chưa gửi; true khi Admin vừa cấp lại mật khẩu
    role,
  }
}

const clearSession = () => {
  sessionStorage.removeItem(STORAGE_KEYS.token)
  sessionStorage.removeItem(STORAGE_KEYS.expiresAt)
}

const sessionExpiredMsg = () => t('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [ready, setReady] = useState(false) // false trong lúc kiểm tra token còn hiệu lực khi mở trang
  const [notice, setNotice] = useState('') // thông báo hiển thị ở trang đăng nhập (ví dụ hết phiên)
  const timer = useRef(null)

  const endSession = useCallback((message = '') => {
    clearTimeout(timer.current)
    clearSession()
    setUser(null)
    setNotice(message)
  }, [])

  // Tự đăng xuất đúng lúc token hết hạn
  const armExpiry = useCallback(
    (token) => {
      clearTimeout(timer.current)
      const exp = getTokenExpiryMs(token)
      if (!exp) return
      const wait = exp - Date.now()
      if (wait <= 0) return endSession(sessionExpiredMsg())
      timer.current = setTimeout(() => endSession(sessionExpiredMsg()), Math.min(wait, 2 ** 31 - 1))
    },
    [endSession],
  )

  // Mở trang: nếu còn token thì hỏi Backend (GET /auth/me) để chắc chắn token còn hiệu lực
  useEffect(() => {
    let alive = true
    const token = sessionStorage.getItem(STORAGE_KEYS.token)
    const exp = token && getTokenExpiryMs(token)
    if (!token || (exp && exp <= Date.now())) {
      clearSession()
      setReady(true)
      return
    }
    authService
      .fetchMe()
      .then((u) => {
        if (!alive) return
        const nu = normalizeUser(u)
        if (!VALID_ROLES.includes(nu?.role)) throw new Error('role')
        setUser(nu)
        armExpiry(token)
      })
      .catch(() => alive && clearSession())
      .finally(() => alive && setReady(true))
    return () => {
      alive = false
    }
  }, [armExpiry])

  // API trả 401 ở bất kỳ đâu -> kết thúc phiên
  useEffect(() => onUnauthorized(() => endSession(sessionExpiredMsg())), [endSession])

  const login = useCallback(
    async (email, password) => {
      const res = await authService.login(email, password)
      sessionStorage.setItem(STORAGE_KEYS.token, res.accessToken)
      try {
        // Ưu tiên user trong LoginResponse, thiếu thì hỏi /auth/me
        const nu = normalizeUser(res.user) ?? normalizeUser(await authService.fetchMe())
        if (!VALID_ROLES.includes(nu?.role)) throw Object.assign(new Error(t('Tài khoản chưa được gán vai trò hợp lệ.')), { local: true })
        setUser(nu)
        setNotice('')
        armExpiry(res.accessToken)
        return nu
      } catch (e) {
        clearSession()
        throw e
      }
    },
    [armExpiry],
  )

  const logout = useCallback(async () => {
    try {
      await authService.logout() // đưa token vào blacklist; lỗi mạng vẫn cho đăng xuất phía giao diện
    } catch {
      /* bỏ qua */
    }
    endSession('')
  }, [endSession])

  // Cập nhật thông tin người dùng cục bộ và đồng bộ localStorage
  const updateUser = useCallback((updates) => {
    setUser((prev) => {
      if (!prev) return prev
      const next = { ...prev, ...updates }
      if (updates.avatarUrl !== undefined) {
        try {
          if (updates.avatarUrl) localStorage.setItem(`hd_avatar_${prev.id}`, updates.avatarUrl)
          else localStorage.removeItem(`hd_avatar_${prev.id}`)
        } catch { /* ignore */ }
      }
      if (updates.fullName !== undefined || updates.department !== undefined || updates.phone !== undefined) {
        try {
          const prevSaved = JSON.parse(localStorage.getItem(`hd_profile_${prev.id}`) || '{}')
          const nextSaved = {
            ...prevSaved,
            ...(updates.fullName !== undefined ? { fullName: updates.fullName } : {}),
            ...(updates.department !== undefined ? { department: updates.department } : {}),
            ...(updates.phone !== undefined ? { phone: updates.phone } : {}),
          }
          localStorage.setItem(`hd_profile_${prev.id}`, JSON.stringify(nextSaved))
        } catch { /* ignore */ }
      }
      return next
    })
  }, [])

  // Tải lại thông tin tài khoản từ /auth/me (ví dụ sau khi đổi mật khẩu)
  const reloadUser = useCallback(async () => {
    const nu = normalizeUser(await authService.fetchMe())
    if (VALID_ROLES.includes(nu?.role)) setUser(nu)
  }, [])

  const value = useMemo(() => ({ user, ready, notice, login, logout, reloadUser, updateUser }), [user, ready, notice, login, logout, reloadUser, updateUser])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
