import { useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import { useAuth } from '@/context/AuthContext'
import { ROLE_HOME } from '@/config/constants'

export default function ForbiddenPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  return (
    <section className="card">
      <div className="empty" role="alert">
        <div className="ico">
          <Icon name="lock" />
        </div>
        <b>Bạn không có quyền truy cập trang này (403)</b>
        <span>Tài khoản của bạn không được phép xem nội dung này.</span>
        <button type="button" className="btn pri" onClick={() => navigate(ROLE_HOME[user.role])}>
          Về trang chủ
        </button>
      </div>
    </section>
  )
}
