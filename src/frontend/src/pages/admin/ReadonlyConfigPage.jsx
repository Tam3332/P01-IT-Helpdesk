import { useLocation, useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import { PriorityBadge, StatusBadge } from '@/components/Badges'
import { PRIORITY, PRIORITY_KEYS, STATUS_KEYS } from '@/config/domain'
import { NAV } from '@/config/nav'
import { ROLES } from '@/config/constants'

const STATUS_DESC = {
  NEW: 'Ticket vừa được tạo, chờ phân công',
  ASSIGNED: 'Đã có người phụ trách chính',
  IN_PROGRESS: 'IT Staff đang xử lý sự cố',
  RESOLVED: 'Có giải pháp, chờ nhân viên xác nhận',
  CLOSED: 'Hoàn tất, không chỉnh sửa trực tiếp',
  REOPENED: 'Yêu cầu mở lại đã được IT Manager duyệt',
  REOPEN_PENDING: 'Nhân viên đã gửi yêu cầu mở lại, chờ IT Manager duyệt',
}

// Mức ưu tiên và trạng thái là tập giá trị cố định do cơ sở dữ liệu ràng buộc (CHECK constraint),
// backend dùng chúng để tính SLA và kiểm tra luồng, nên màn hình chỉ xem, không sửa.
export default function ReadonlyConfigPage({ kind }) {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const item = NAV[ROLES.ADMIN].find((i) => pathname.startsWith(i.path))
  const isPri = kind === 'priorities'
  return (
    <>
      <PageHeader title={item.label} subtitle={item.subtitle} />
      <div><button type="button" className="btn ghost sm" onClick={() => navigate('/admin/settings')}><Icon name="back" />Cài đặt hệ thống</button></div>
      <div className="banner info"><Icon name="lock" /><div className="grow">{isPri ? 'Các mức ưu tiên' : 'Các trạng thái'} là tập giá trị cố định của hệ thống (ràng buộc trong cơ sở dữ liệu) nên không thể thêm, sửa hoặc ẩn từ giao diện.</div></div>
      <section className="card">
        <div className="ch"><h2>{isPri ? PRIORITY_KEYS.length : STATUS_KEYS.length} {isPri ? 'mức ưu tiên' : 'trạng thái'}</h2></div>
        <div className="tw" style={{ marginTop: 14 }}>
          {isPri ? (
            <table>
              <thead><tr><th>Tên hiển thị</th><th>Mã</th><th>Cấp độ</th></tr></thead>
              <tbody>{PRIORITY_KEYS.map((p) => <tr key={p}><td><PriorityBadge priority={p} /></td><td><code>{p}</code></td><td className="num">{PRIORITY[p].level}</td></tr>)}</tbody>
            </table>
          ) : (
            <table>
              <thead><tr><th>Tên hiển thị</th><th>Mã</th><th>Mô tả</th></tr></thead>
              <tbody>{STATUS_KEYS.map((s) => <tr key={s}><td><StatusBadge status={s} /></td><td><code>{s}</code></td><td>{STATUS_DESC[s]}</td></tr>)}</tbody>
            </table>
          )}
        </div>
      </section>
    </>
  )
}
