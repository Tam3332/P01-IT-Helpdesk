import { Tabs, Table } from 'antd'
import PageHeader from '@/components/PageHeader'
import PagePlaceholder from '@/components/PagePlaceholder'
import { SLA_DEFAULT, PRIORITY_META } from '@/config/constants'
import { categories } from '@/mocks/data'

// SC-A03 Danh mục, SC-A04 SLA. TODO: form chỉnh sửa
export default function SettingsPage() {
  const sla = Object.entries(SLA_DEFAULT).map(([k, v]) => ({ key: k, priority: PRIORITY_META[k].label, ...v }))
  return (
    <>
      <PageHeader title="Cấu hình hệ thống" subtitle="SC-A03 / SC-A04" />
      <Tabs items={[
        { key: 'sla', label: 'SLA', children: <Table pagination={false} dataSource={sla} columns={[
          { title: 'Ưu tiên', dataIndex: 'priority' }, { title: 'Phản hồi (giờ)', dataIndex: 'response' }, { title: 'Xử lý (giờ)', dataIndex: 'resolve' }]} /> },
        { key: 'cat', label: 'Danh mục', children: <Table pagination={false} dataSource={categories.map((c) => ({ key: c, name: c }))} columns={[{ title: 'Tên danh mục', dataIndex: 'name' }]} /> },
        { key: 'st', label: 'Trạng thái / Ưu tiên', children: <PagePlaceholder code="SC-A02" title="Trạng thái & ưu tiên" todo={['Xem/cấu hình nhãn']} /> },
      ]} />
    </>
  )
}
