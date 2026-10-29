import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { Card, Form, Input, Button, Alert, Typography, Space } from 'antd'
import { useAuth } from '@/context/AuthContext'
import { ROLE_HOME, USE_MOCK } from '@/config/constants'

export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [form] = Form.useForm()

  if (user) return <Navigate to={ROLE_HOME[user.role]} replace />

  const onFinish = async ({ username, password }) => {
    setLoading(true); setError('')
    try {
      const u = await login(username, password)
      navigate(ROLE_HOME[u.role], { replace: true })
    } catch (e) {
      setError(e.response?.data?.message || e.message)
    } finally { setLoading(false) }
  }

  return (
    <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: 16 }}>
      <Card style={{ width: 400, maxWidth: '100%' }}>
        <Typography.Title level={3} style={{ textAlign: 'center', color: '#3157D5' }}>IT Helpdesk</Typography.Title>
        {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 16 }} />}
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item name="username" label="Tên đăng nhập" rules={[{ required: true, message: 'Nhập tên đăng nhập' }]}><Input autoFocus /></Form.Item>
          <Form.Item name="password" label="Mật khẩu" rules={[{ required: true, message: 'Nhập mật khẩu' }]}><Input.Password /></Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>Đăng nhập</Button>
        </Form>
        {USE_MOCK && (
          <Space wrap style={{ marginTop: 16 }}>
            <Typography.Text type="secondary">Demo (mật khẩu 123456):</Typography.Text>
            {['employee', 'staff', 'manager', 'admin', 'locked'].map((u) => (
              <Button key={u} size="small" onClick={() => form.setFieldsValue({ username: u, password: '123456' })}>{u}</Button>
            ))}
          </Space>
        )}
      </Card>
    </div>
  )
}
