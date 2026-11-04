import { useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import { useAuth } from '@/context/AuthContext'
import { ROLE_HOME } from '@/config/constants'

export default function NotFoundPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  return (
    <section className="card">
      <div className="empty">
        <div className="ico">
          <Icon name="search" />
        </div>
        <b>Không tìm thấy trang (404)</b>
        <span>Đường dẫn không tồn tại hoặc đã bị di chuyển.</span>
        <button type="button" className="btn pri" onClick={() => navigate(user ? ROLE_HOME[user.role] : '/login')}>
          {user ? 'Về trang chủ' : 'Về đăng nhập'}
        </button>
      </div>
    </section>
  )
}
