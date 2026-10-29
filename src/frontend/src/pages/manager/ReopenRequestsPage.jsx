import { Table } from 'antd'
import PageHeader from '@/components/PageHeader'
import useAsync from '@/hooks/useAsync'
import { listReopenRequests } from '@/services/ticketService'
import { fmtDate } from '@/utils/format'

// SC-M06/M07. TODO: duyệt / từ chối kèm lý do
export default function ReopenRequestsPage() {
  const { data, loading } = useAsync(listReopenRequests, [])
  return (
    <>
      <PageHeader title="Yêu cầu mở lại" subtitle="SC-M06" />
      <Table rowKey="id" loading={loading} dataSource={data} scroll={{ x: 'max-content' }} columns={[
        { title: 'Ticket', dataIndex: 'ticketId' }, { title: 'Người yêu cầu', dataIndex: 'requester' },
        { title: 'Lý do', dataIndex: 'reason' }, { title: 'Gửi lúc', dataIndex: 'createdAt', render: fmtDate },
        { title: 'Trạng thái', dataIndex: 'status' },
      ]} />
    </>
  )
}
