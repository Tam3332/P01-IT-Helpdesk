import { useEffect, useState } from 'react'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import Pagination from '@/components/Pagination'
import { Async, EmptyState } from '@/components/StateViews'
import useAsync from '@/hooks/useAsync'
import { LIMITS } from '@/config/domain'
import { NAV } from '@/config/nav'
import { ROLES } from '@/config/constants'
import { listAuditLogs } from '@/services/referenceService'
import { fmtDateTime } from '@/utils/format'
import { t } from '@/i18n'

const isOk = (r) => ['SUCCESS', 'Thành công', 'OK', true].includes(r)

export default function AuditLogsPage() {
  const item = NAV[ROLES.ADMIN].find((i) => i.path === '/admin/audit-logs')
  const [page, setPage] = useState(0)
  const [q, setQ] = useState('')
  const [res, setRes] = useState('')
  const data = useAsync(() => listAuditLogs({ page, size: LIMITS.pageSize }), [page])
  useEffect(() => setPage(0), [])

  const needle = q.trim().toLowerCase()
  const rows = (data.data?.content || []).filter((a) => (!needle || `${a.actorName ?? a.actor ?? ''} ${a.action ?? ''} ${a.target ?? a.object ?? ''}`.toLowerCase().includes(needle)) && (!res || (res === 'ok') === isOk(a.result)))

  return (
    <>
      <PageHeader title={t(item.label)} subtitle={t(item.subtitle)} />
      <div className="banner info"><Icon name="lock" /><div className="grow">{t('Bản ghi chỉ đọc, không có thao tác sửa hoặc xóa. Nhật ký bất biến do backend lưu.')}</div></div>
      <section className="card">
        <div className="filters audit-f">
          <div className="sw"><Icon name="search" /><input className="in" value={q} onChange={(e) => setQ(e.target.value)} placeholder={t('Tìm theo người thực hiện, hành động, đối tượng')} aria-label={t('Tìm trong nhật ký')} /></div>
          <select className="sel" value={res} onChange={(e) => setRes(e.target.value)} aria-label={t('Lọc theo kết quả')}>
            <option value="">{t('Mọi kết quả')}</option><option value="ok">{t('Thành công')}</option><option value="fail">{t('Thất bại')}</option>
          </select>
        </div>
        <Async state={data} notReadyText={t('Backend chưa có API nhật ký hệ thống (đề xuất GET /api/v1/admin/audit-logs). Màn hình sẽ hiện dữ liệu khi API được triển khai.')}>
          {rows.length ? (
            <div className="tw">
              <table>
                <thead><tr><th>{t('Thời gian')}</th><th>{t('Người thực hiện')}</th><th>{t('Hành động')}</th><th>{t('Đối tượng')}</th><th>{t('Kết quả')}</th></tr></thead>
                <tbody>
                  {rows.map((a, i) => (
                    <tr key={a.id ?? i}>
                      <td className="nw num">{fmtDateTime(a.createdAt ?? a.timestamp)}</td>
                      <td className="nw">{a.actorName ?? a.actor ?? '—'}</td>
                      <td>{a.action}</td>
                      <td className="nw">{a.target ?? a.object ?? '—'}</td>
                      <td><span className={`badge ${isOk(a.result) ? 's-done' : 's-breach'}`}>{isOk(a.result) ? t('Thành công') : t('Thất bại')}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState title={t('Không có bản ghi phù hợp')} />
          )}
          {data.data && <Pagination page={data.data.page} totalPages={data.data.totalPages} totalElements={data.data.totalElements} size={data.data.size || LIMITS.pageSize} onPage={setPage} />}
        </Async>
      </section>
    </>
  )
}
