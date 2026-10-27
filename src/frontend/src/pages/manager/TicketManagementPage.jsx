import { Button } from 'antd'
import PageHeader from '@/components/PageHeader'
import TicketTable from '@/components/TicketTable'
import useAsync from '@/hooks/useAsync'
import { listTickets } from '@/services/ticketService'

// SC-M02. TODO: modal phân công (SC-M04) gọi PUT /tickets/:id/assign
export default function TicketManagementPage() {
  const { data, loading } = useAsync(() => listTickets(), [])
  const assignCol = { title: 'Xử lý bởi', render: (_, t) => t.assignee ?? <Button size="small" disabled>Phân công</Button> }
  return (<><PageHeader title="Quản lý ticket" subtitle="SC-M02" /><TicketTable data={data} loading={loading} extraColumns={[assignCol]} /></>)
}
