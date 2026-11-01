import { useLocation } from 'react-router-dom'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import { useAuth } from '@/context/AuthContext'
import { findNavItem } from '@/config/nav'

// Chỗ giữ trang cho các màn hình sẽ nối API ở các đợt sau. Không có dữ liệu giả.
export default function ComingSoonPage({ wave }) {
  const { user } = useAuth()
  const { pathname } = useLocation()
  const item = findNavItem(user.role, pathname)
  return (
    <>
      {item && <PageHeader title={item.label} subtitle={item.subtitle} />}
      <section className="card">
        <div className="empty">
          <div className="ico">
            <Icon name={item?.icon || 'layers'} />
          </div>
          <b>Màn hình này sẽ được nối API ở đợt {wave}</b>
          <span>Khung giao diện và phân quyền đã sẵn sàng.</span>
        </div>
      </section>
    </>
  )
}
