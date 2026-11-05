import { useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import useAsync from '@/hooks/useAsync'
import { PRIORITY_KEYS, STATUS_KEYS } from '@/config/domain'
import { NAV } from '@/config/nav'
import { ROLES } from '@/config/constants'
import { adminListCategories, listSlaConfigs } from '@/services/referenceService'

export default function SettingsPage() {
  const item = NAV[ROLES.ADMIN].find((i) => i.path === '/admin/settings')
  const navigate = useNavigate()
  const cats = useAsync(() => adminListCategories().catch(() => null), [])
  const sla = useAsync(() => listSlaConfigs().catch(() => null), [])
  const count = (s, unit) => (s.loading ? 'Đang tải...' : s.data ? `${s.data.length} ${unit}` : 'Chưa tải được')
  const cards = [
    ['/admin/categories', 'Danh mục', 'tag', count(cats, 'danh mục'), 'Loại yêu cầu hiển thị khi nhân viên tạo ticket.'],
    ['/admin/priorities', 'Mức độ ưu tiên', 'flag', `${PRIORITY_KEYS.length} mức ưu tiên`, 'Các mức ưu tiên có thể chọn khi tạo ticket.'],
    ['/admin/statuses', 'Trạng thái', 'layers', `${STATUS_KEYS.length} trạng thái`, 'Các trạng thái trong vòng đời ticket.'],
    ['/admin/sla-config', 'Cấu hình SLA', 'clock', count(sla, 'quy tắc'), 'Giờ phản hồi và giờ xử lý theo mức ưu tiên.'],
  ]
  return (
    <>
      <PageHeader title={item.label} subtitle={item.subtitle} />
      <div className="kpis" style={{ gridTemplateColumns: 'repeat(auto-fit,minmax(260px,1fr))' }}>
        {cards.map(([path, name, icon, c, desc]) => (
          <section className="card" key={path}>
            <div className="cb" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div className="kpi" style={{ padding: 0 }}>
                <div className="ico t-primary"><Icon name={icon} /></div>
                <div><span>{c}</span><b style={{ fontSize: 18 }}>{name}</b></div>
              </div>
              <p className="muted">{desc}</p>
              <button type="button" className="btn" onClick={() => navigate(path)}>Quản lý {name === 'Cấu hình SLA' ? name : name.toLowerCase()}</button>
            </div>
          </section>
        ))}
      </div>
    </>
  )
}
