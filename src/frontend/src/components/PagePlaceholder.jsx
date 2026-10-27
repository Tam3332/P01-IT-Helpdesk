import { Card, Alert } from 'antd'
import PageHeader from './PageHeader'

// Trang chờ code: ghi rõ mã màn hình & task để dễ lần theo SRS/SDD
export default function PagePlaceholder({ code, title, todo = [] }) {
  return (
    <>
      <PageHeader title={title} subtitle={`Màn hình ${code}`} />
      <Card>
        <Alert type="info" showIcon title="Skeleton — chưa cài đặt chức năng"
          description={<ul style={{ margin: 0, paddingLeft: 18 }}>{todo.map((t) => <li key={t}>{t}</li>)}</ul>} />
      </Card>
    </>
  )
}
