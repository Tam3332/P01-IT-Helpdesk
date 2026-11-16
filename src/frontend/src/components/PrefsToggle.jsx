import { useEffect, useState } from 'react'
import Icon from '@/components/Icon'
import { useLang } from '@/context/LangContext'
import { t } from '@/i18n'

const THEME_KEY = 'hd_theme'
const readTheme = () => {
  try {
    const v = localStorage.getItem(THEME_KEY)
    if (v === 'light' || v === 'dark') return v
  } catch {
    /* dùng theo hệ điều hành */
  }
  return null
}
// Áp chủ đề đã lưu ngay khi tải trang, tránh nháy sáng/tối
const saved = readTheme()
if (saved && typeof document !== 'undefined') document.documentElement.dataset.theme = saved

// Nút đổi ngôn ngữ (VI/EN) và chủ đề sáng/tối
export default function PrefsToggle() {
  const { lang, setLang } = useLang()
  const [theme, setTheme] = useState(() => saved || (window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'))
  useEffect(() => {
    document.documentElement.dataset.theme = theme
  }, [theme])
  const flip = () => {
    const next = theme === 'dark' ? 'light' : 'dark'
    setTheme(next)
    try {
      localStorage.setItem(THEME_KEY, next)
    } catch {
      /* không lưu được thì chỉ áp dụng cho lần xem này */
    }
  }
  return (
    <div className="prefs">
      <div className="seg langseg" role="group" aria-label={t('Ngôn ngữ')}>
        <button type="button" aria-pressed={lang === 'vi'} onClick={() => setLang('vi')} title="Tiếng Việt">
          VI
        </button>
        <button type="button" aria-pressed={lang === 'en'} onClick={() => setLang('en')} title="English">
          EN
        </button>
      </div>
      <button type="button" className="iconbtn" aria-label={theme === 'dark' ? t('Chế độ sáng') : t('Chế độ tối')} title={theme === 'dark' ? t('Chế độ sáng') : t('Chế độ tối')} onClick={flip}>
        <Icon name={theme === 'dark' ? 'sun' : 'moon'} />
      </button>
    </div>
  )
}
