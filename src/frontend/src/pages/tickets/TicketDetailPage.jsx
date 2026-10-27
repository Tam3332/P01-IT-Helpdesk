import { useParams } from 'react-router-dom'
import { Card, Descriptions, Result, Spin } from 'antd'
import PageHeader from '@/components/PageHeader'
import { StatusTag, PriorityTag, SlaTag } from '@/components/Tags'
import useAsync from '@/hooks/useAsync'
import { getTicket } from '@/services/ticketService'
import { getSlaState } from '@/utils/sla'
import { fmtDate } from '@/utils/format'

// SC-E03/S03/M03 dùng chung. TODO (tuần 4): nút hành động theo role+trạng thái
// (Đóng/Mở lại cho Employee; Nhận/Resolve cho Staff; Assign/Duyệt mở lại cho Manager), timeline, comment.
export default function TicketDetailPage() {
  const { id } = useParams()
  const { data: t, loading } = useAsync(() => getTicket(id), [id])
  if (loading) return <Spin />
  if (!t) return <Result status="404" title="Không tìm thấy ticket" />
  return (
    <>
      <PageHeader title={`${t.id} — ${t.title}`} subtitle="Chi tiết ticket" />
      <Card>
        <Descriptions column={{ xs: 1, md: 2 }} bordered>
          <Descriptions.Item label="Trạng thái"><StatusTag value={t.status} /></Descriptions.Item>
          <Descriptions.Item label="Ưu tiên"><PriorityTag value={t.priority} /></Descriptions.Item>
          <Descriptions.Item label="SLA"><SlaTag value={getSlaState(t.elapsed, t.limit)} /></Descriptions.Item>
          <Descriptions.Item label="Danh mục">{t.category}</Descriptions.Item>
          <Descriptions.Item label="Người tạo">{t.creator}</Descriptions.Item>
          <Descriptions.Item label="Người xử lý">{t.assignee ?? '—'}</Descriptions.Item>
          <Descriptions.Item label="Tạo lúc">{fmtDate(t.createdAt)}</Descriptions.Item>
        </Descriptions>
      </Card>
    </>
  )
}
