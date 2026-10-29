import PageHeader from '@/components/PageHeader'
import TicketTable from '@/components/TicketTable'
import useAsync from '@/hooks/useAsync'
import { useAuth } from '@/context/AuthContext'
import { listTickets } from '@/services/ticketService'

// SC-S02. TODO: lọc, modal Resolve (SC-S04)
export default function AssignedTicketsPage() {
  const { user } = useAuth()
  const { data, loading } = useAsync(() => listTickets({ assignee: user.username }), [user.username])
  return (<><PageHeader title="Ticket được giao" subtitle="SC-S02" /><TicketTable data={data} loading={loading} /></>)
}
