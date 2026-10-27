import { Table } from 'antd'
import { Link } from 'react-router-dom'
import { PAGE_SIZE } from '@/config/constants'
import { fmtDate } from '@/utils/format'
import { getSlaState } from '@/utils/sla'
import { StatusTag, PriorityTag, SlaTag } from './Tags'

export default function TicketTable({ data, loading, extraColumns = [] }) {
  const columns = [
    { title: 'Mã', dataIndex: 'id', render: (id) => <Link to={`/tickets/${id}`}>{id}</Link> },
    { title: 'Tiêu đề', dataIndex: 'title' },
    { title: 'Ưu tiên', dataIndex: 'priority', render: (v) => <PriorityTag value={v} /> },
    { title: 'Trạng thái', dataIndex: 'status', render: (v) => <StatusTag value={v} /> },
    { title: 'SLA', render: (_, t) => <SlaTag value={getSlaState(t.elapsed, t.limit)} /> },
    { title: 'Tạo lúc', dataIndex: 'createdAt', render: fmtDate },
    ...extraColumns,
  ]
  return <Table rowKey="id" columns={columns} dataSource={data} loading={loading}
    pagination={{ pageSize: PAGE_SIZE, hideOnSinglePage: true }} scroll={{ x: 'max-content' }} />
}
