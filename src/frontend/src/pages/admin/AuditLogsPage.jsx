import { Table, Alert } from 'antd'
import PageHeader from '@/components/PageHeader'
import useAsync from '@/hooks/useAsync'
import { listAuditLogs } from '@/services/ticketService'
import { fmtDate } from '@/utils/format'

// SC-A07 — chỉ đọc (audit log bất biến: KHÔNG có nút sửa/xóa)
export default function AuditLogsPage() {
  const { data, loading } = useAsync(listAuditLogs, [])
  return (
    <>
      <PageHeader title="Audit log" subtitle="SC-A07" />
      <Alert type="info" showIcon title="Chỉ đọc — không thể sửa hoặc xóa" style={{ marginBottom: 12 }} />
      <Table rowKey="id" loading={loading} dataSource={data} scroll={{ x: 'max-content' }} columns={[
        { title: 'Thời gian', dataIndex: 'time', render: fmtDate }, { title: 'Người thực hiện', dataIndex: 'actor' },
        { title: 'Hành động', dataIndex: 'action' }, { title: 'Đối tượng', dataIndex: 'target' }, { title: 'Chi tiết', dataIndex: 'detail' },
      ]} />
    </>
  )
}
