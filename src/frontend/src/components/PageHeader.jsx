import { Typography, Flex } from 'antd'

export default function PageHeader({ title, subtitle, extra }) {
  return (
    <Flex justify="space-between" align="center" wrap style={{ marginBottom: 16 }} gap={8}>
      <div>
        <Typography.Title level={3} style={{ margin: 0 }}>{title}</Typography.Title>
        {subtitle && <Typography.Text type="secondary">{subtitle}</Typography.Text>}
      </div>
      {extra}
    </Flex>
  )
}
