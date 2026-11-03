import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import Modal, { Field } from '@/components/Modal'
import { PriorityBadge } from '@/components/Badges'
import { Async, EmptyState } from '@/components/StateViews'
import { useToast } from '@/context/ToastContext'
import useAsync from '@/hooks/useAsync'
import { PRIORITY } from '@/config/domain'
import { NAV } from '@/config/nav'
import { ROLES } from '@/config/constants'
import { apiError } from '@/services/apiClient'
import { listSlaConfigs, updateSlaConfig } from '@/services/referenceService'

export default function SlaConfigPage() {
  const item = NAV[ROLES.ADMIN].find((i) => i.path === '/admin/sla-config')
  const navigate = useNavigate()
  const list = useAsync(listSlaConfigs, [])
  const [edit, setEdit] = useState(null)
  const items = (list.data || []).slice().sort((a, b) => (PRIORITY[b.priorityLevel]?.level || 0) - (PRIORITY[a.priorityLevel]?.level || 0))
  return (
    <>
      <PageHeader title={item.label} subtitle={item.subtitle} />
      <div><button type="button" className="btn ghost sm" onClick={() => navigate('/admin/settings')}><Icon name="back" />Cài đặt hệ thống</button></div>
      <section className="card">
        <div className="ch"><h2>{items.length} cấu hình SLA</h2></div>
        <Async state={list}>
          {items.length ? (
            <div className="tw" style={{ marginTop: 14 }}>
              <table>
                <thead><tr><th>Mức ưu tiên</th><th>Hạn phản hồi (giờ)</th><th>Hạn xử lý (giờ)</th><th /></tr></thead>
                <tbody>
                  {items.map((c) => (
                    <tr key={c.priorityLevel}>
                      <td><PriorityBadge priority={c.priorityLevel} /></td>
                      <td><span className="num">{c.responseTimeHours}</span></td>
                      <td><span className="num">{c.resolutionTimeHours}</span></td>
                      <td><button type="button" className="btn sm" onClick={() => setEdit(c)}><Icon name="edit" />Sửa</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState icon="clock" title="Chưa có cấu hình SLA" />
          )}
        </Async>
      </section>
      <p className="sm muted">Ticket mới dùng định mức tại thời điểm tạo.</p>
      {edit && <EditDialog cfg={edit} onClose={() => setEdit(null)} onSaved={() => list.reload()} />}
    </>
  )
}

function EditDialog({ cfg, onClose, onSaved }) {
  const toast = useToast()
  const [resp, setResp] = useState(String(cfg.responseTimeHours))
  const [res, setRes] = useState(String(cfg.resolutionTimeHours))
  return (
    <Modal title="Sửa cấu hình SLA" okText="Lưu thay đổi" onClose={onClose} onOk={async () => {
      const rp = Number(resp)
      const rs = Number(res)
      if (!(rp > 0)) throw new Error('Hạn phản hồi phải lớn hơn 0.')
      if (!(rs > 0)) throw new Error('Hạn xử lý phải lớn hơn 0.')
      if (rs < rp) throw new Error('Hạn xử lý không được nhỏ hơn hạn phản hồi.')
      try {
        await updateSlaConfig(cfg.priorityLevel, { responseTimeHours: rp, resolutionTimeHours: rs })
      } catch (e) {
        throw new Error(apiError(e, 'Không lưu được cấu hình.'))
      }
      toast('Đã lưu thay đổi.')
      onSaved()
    }}>
      <Field label="Mức ưu tiên"><input className="in" value={PRIORITY[cfg.priorityLevel]?.label || cfg.priorityLevel} disabled /></Field>
      <div className="row2">
        <Field label="Hạn phản hồi (giờ)" required htmlFor="cf-resp"><input className="in" id="cf-resp" type="number" min="0.1" step="0.05" value={resp} onChange={(e) => setResp(e.target.value)} /></Field>
        <Field label="Hạn xử lý (giờ)" required htmlFor="cf-res"><input className="in" id="cf-res" type="number" min="0.1" step="0.5" value={res} onChange={(e) => setRes(e.target.value)} /></Field>
      </div>
    </Modal>
  )
}
