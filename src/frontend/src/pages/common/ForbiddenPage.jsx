import { useNavigate } from 'react-router-dom'
import ErrorArt from '@/components/ErrorArt'
import { useAuth } from '@/context/AuthContext'
import { ROLE_HOME } from '@/config/constants'
import { t } from '@/i18n'

export default function ForbiddenPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  return (
    <section className="card">
      <div className="empty" role="alert">
        <ErrorArt kind="403" />
        <b>{t('Bạn không có quyền truy cập trang này (403)')}</b>
        <span>{t('Tài khoản của bạn không được phép xem nội dung này.')}</span>
        <button type="button" className="btn pri" onClick={() => navigate(ROLE_HOME[user.role])}>
          {t('Về trang chính')}
        </button>
      </div>
    </section>
  )
}
