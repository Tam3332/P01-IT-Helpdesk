import { Result, Button } from 'antd'
import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return <Result status="404" title="404" subTitle="Không tìm thấy trang." extra={<Link to="/"><Button type="primary">Về trang chủ</Button></Link>} />
}
