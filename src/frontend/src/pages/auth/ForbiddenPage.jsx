import { Result, Button } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { ROLE_HOME } from '@/config/constants'

export default function ForbiddenPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  return <Result status="403" title="403" subTitle="Bạn không có quyền truy cập trang này."
    extra={<Button type="primary" onClick={() => navigate(user ? ROLE_HOME[user.role] : '/login')}>Về trang chủ</Button>} />
}
