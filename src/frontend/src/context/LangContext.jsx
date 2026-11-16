import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import { getLang, setLang as storeLang } from '@/i18n'

const LangContext = createContext(null)

// Đổi ngôn ngữ: AppRoutes đọc useLang() nên render lại toàn bộ cây (state của form giữ nguyên, không remount).
export function LangProvider({ children }) {
  const [lang, setLangState] = useState(getLang)
  const setLang = useCallback((next) => {
    storeLang(next)
    setLangState(getLang())
  }, [])
  const value = useMemo(() => ({ lang, setLang }), [lang, setLang])
  return (
    <LangContext.Provider value={value}>{children}</LangContext.Provider>
  )
}

export const useLang = () => useContext(LangContext)
