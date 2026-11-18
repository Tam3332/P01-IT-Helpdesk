import { useNavigate } from 'react-router-dom'
import ErrorArt from '@/components/ErrorArt'
import { useAuth } from '@/context/AuthContext'
import { ROLE_HOME } from '@/config/constants'
import { t } from '@/i18n'

export default function NotFoundPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  return (
    <section className="card">
      <div className="empty">
        <ErrorArt kind="404" />
        <b>{t('Không tìm thấy trang (404)')}</b>
        <span>{t('Đường dẫn không tồn tại hoặc đã bị di chuyển.')}</span>
        <button type="button" className="btn pri" onClick={() => navigate(user ? ROLE_HOME[user.role] : '/login')}>
          {user ? t('Về trang chính') : t('Về đăng nhập')}
        </button>
      </div>
    </section>
  )
}
