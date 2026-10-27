import { createContext, useContext, useState, useCallback, useMemo } from 'react'
import { STORAGE_KEYS } from '@/config/constants'
import * as authService from '@/services/authService'

const AuthContext = createContext(null)

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.user))
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readUser)

  const login = useCallback(async (username, password) => {
    const { token, user: u } = await authService.login(username, password)
    localStorage.setItem(STORAGE_KEYS.token, token)
    localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(u))
    setUser(u)
    return u
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEYS.token)
    localStorage.removeItem(STORAGE_KEYS.user)
    setUser(null)
  }, [])

  const value = useMemo(() => ({ user, login, logout }), [user, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
