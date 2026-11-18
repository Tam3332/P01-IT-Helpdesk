import { useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import useAsync from '@/hooks/useAsync'
import { PRIORITY_KEYS, STATUS_KEYS } from '@/config/domain'
import { NAV } from '@/config/nav'
import { ROLES } from '@/config/constants'
import { adminListCategories, listSlaConfigs } from '@/services/referenceService'
import { t } from '@/i18n'

export default function SettingsPage() {
  const item = NAV[ROLES.ADMIN].find((i) => i.path === '/admin/settings')
  const navigate = useNavigate()
  const cats = useAsync(() => adminListCategories().catch(() => null), [])
  const sla = useAsync(() => listSlaConfigs().catch(() => null), [])
  const count = (s, key) => (s.loading ? t('Đang tải...') : s.data ? t(key, { n: s.data.length }) : t('Chưa tải được'))
  const cards = [
    ['/admin/categories', 'Danh mục', 'tag', count(cats, '{n} danh mục'), 'Loại yêu cầu hiển thị khi nhân viên tạo ticket.'],
    ['/admin/priorities', 'Mức độ ưu tiên', 'flag', t('{n} mức ưu tiên', { n: PRIORITY_KEYS.length }), 'Các mức ưu tiên có thể chọn khi tạo ticket.'],
    ['/admin/statuses', 'Trạng thái', 'layers', t('{n} trạng thái', { n: STATUS_KEYS.length }), 'Các trạng thái trong vòng đời ticket.'],
    ['/admin/sla-config', 'Cấu hình SLA', 'clock', count(sla, '{n} quy tắc'), 'Giờ phản hồi và giờ xử lý theo mức ưu tiên.'],
  ]
  return (
    <>
      <PageHeader title={t(item.label)} subtitle={t(item.subtitle)} />
      <div className="kpis" style={{ gridTemplateColumns: 'repeat(auto-fit,minmax(260px,1fr))' }}>
        {cards.map(([path, name, icon, c, desc]) => (
          <section className="card" key={path} style={{ display: 'flex', flexDirection: 'column' }}>
            <div className="cb" style={{ display: 'flex', flexDirection: 'column', gap: 14, flex: 1 }}>
              <div className="kpi" style={{ padding: 0 }}>
                <div className="ico t-primary"><Icon name={icon} /></div>
                <div><span>{c}</span><b style={{ fontSize: 18 }}>{t(name)}</b></div>
              </div>
              <p className="muted" style={{ margin: 0, flex: 1, minHeight: 38, lineHeight: 1.4 }}>{t(desc)}</p>
              <button type="button" className="btn" style={{ marginTop: 'auto' }} onClick={() => navigate(path)}>{t('Quản lý {name}', { name: name === 'Cấu hình SLA' ? t(name) : t(name).toLowerCase() })}</button>
            </div>
          </section>
        ))}
      </div>
    </>
  )
}
