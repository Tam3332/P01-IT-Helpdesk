import { Button } from 'antd'
import { Link } from 'react-router-dom'
import PageHeader from '@/components/PageHeader'
import TicketTable from '@/components/TicketTable'
import useAsync from '@/hooks/useAsync'
import { useAuth } from '@/context/AuthContext'
import { listTickets } from '@/services/ticketService'

// SC-E01 — mẫu hoàn chỉnh: gọi service -> bảng. TODO: lọc theo trạng thái, tìm kiếm
export default function MyTicketsPage() {
  const { user } = useAuth()
  const { data, loading } = useAsync(() => listTickets({ creator: user.username }), [user.username])
  return (
    <>
      <PageHeader title="Ticket của tôi" subtitle="SC-E01"
        extra={<Link to="/employee/tickets/new"><Button type="primary">Tạo ticket</Button></Link>} />
      <TicketTable data={data} loading={loading} />
    </>
  )
}
