import { useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import { Kpi } from '@/components/Kpi'
import useAsync from '@/hooks/useAsync'
import { PRIORITY_KEYS, STATUS_KEYS } from '@/config/domain'
import { NAV } from '@/config/nav'
import { ROLES } from '@/config/constants'
import { adminListCategories, adminListUsers, listAuditLogs } from '@/services/referenceService'

// Mỗi số liệu tải riêng: API nào lỗi chỉ hiện "—" ở ô đó.
const settle = (p) => p.then((v) => v).catch(() => null)
const load = async () => {
  const [users, cats, logs] = await Promise.all([
    settle(adminListUsers({ page: 0, size: 1 }).then((r) => r.totalElements)),
    settle(adminListCategories().then((r) => r.length)),
    settle(listAuditLogs({ page: 0, size: 1 }).then((r) => r.totalElements)),
  ])
  return { users, cats, logs }
}

export default function AdminOverview() {
  const item = NAV[ROLES.ADMIN][0]
  const navigate = useNavigate()
  const d = useAsync(load, [])
  const v = d.data || {}
  return (
    <>
      <PageHeader title={item.label} subtitle={item.subtitle} />
      <div className="kpis" style={{ gridTemplateColumns: 'repeat(auto-fit,minmax(200px,1fr))' }}>
        <Kpi label="Người dùng" value={d.loading ? '…' : v.users} tone="primary" icon="users" />
        <Kpi label="Danh mục" value={d.loading ? '…' : v.cats} tone="info" icon="tag" />
        <Kpi label="Mức ưu tiên" value={PRIORITY_KEYS.length} tone="info" icon="flag" />
        <Kpi label="Trạng thái" value={STATUS_KEYS.length} tone="vio" icon="layers" />
        <Kpi label="Bản ghi nhật ký" value={d.loading ? '…' : v.logs} tone="neu" icon="file" />
      </div>
      <div className="banner info"><Icon name="shield" /><div className="grow">Quản trị viên không xử lý ticket (phân tách nhiệm vụ): chỉ xem toàn bộ ticket ở chế độ chỉ đọc, quản lý người dùng, cấu hình hệ thống và xem nhật ký.</div></div>
      <section className="card">
        <div className="ch"><h2>Truy cập nhanh</h2></div>
        <div className="cb">
          <div className="acts">
            <button type="button" className="btn pri" onClick={() => navigate('/admin/users')}><Icon name="users" />Người dùng</button>
            <button type="button" className="btn" onClick={() => navigate('/admin/settings')}><Icon name="sliders" />Cài đặt hệ thống</button>
            <button type="button" className="btn" onClick={() => navigate('/admin/audit-logs')}><Icon name="file" />Nhật ký hệ thống</button>
          </div>
        </div>
      </section>
    </>
  )
}
