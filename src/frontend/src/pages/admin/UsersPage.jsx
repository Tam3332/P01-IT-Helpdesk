import { Table, Tag } from 'antd'
import PageHeader from '@/components/PageHeader'
import useAsync from '@/hooks/useAsync'
import { listUsers } from '@/services/ticketService'
import { ROLE_LABEL } from '@/config/constants'

// SC-A01. TODO: thêm/sửa/khóa user (backend ghi audit log)
export default function UsersPage() {
  const { data, loading } = useAsync(listUsers, [])
  return (
    <>
      <PageHeader title="Quản lý người dùng" subtitle="SC-A01" />
      <Table rowKey="username" loading={loading} dataSource={data} scroll={{ x: 'max-content' }} columns={[
        { title: 'Tài khoản', dataIndex: 'username' }, { title: 'Họ tên', dataIndex: 'fullName' },
        { title: 'Role', dataIndex: 'role', render: (r) => ROLE_LABEL[r] }, { title: 'Phòng ban', dataIndex: 'dept' },
        { title: 'Trạng thái', dataIndex: 'locked', render: (l) => (l ? <Tag color="red">Khóa</Tag> : <Tag color="green">Hoạt động</Tag>) },
      ]} />
    </>
  )
}
