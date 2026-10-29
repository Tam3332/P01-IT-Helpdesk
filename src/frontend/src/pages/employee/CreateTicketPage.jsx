import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Form, Input, Select, Upload, Button, message } from 'antd'
import { UploadOutlined } from '@ant-design/icons'
import PageHeader from '@/components/PageHeader'
import { useAuth } from '@/context/AuthContext'
import { createTicket } from '@/services/ticketService'
import { categories } from '@/mocks/data'
import { FILE_RULES, PRIORITY_META } from '@/config/constants'
import { validateFile } from '@/utils/format'

// SC-E02 — form tạo ticket + ràng buộc file (≤5 file, ≤10MB, đuôi cho phép)
export default function CreateTicketPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [saving, setSaving] = useState(false)
  const [msg, ctx] = message.useMessage()

  const onFinish = async (v) => {
    setSaving(true)
    try {
      const { files: _f, ...payload } = v
      const t = await createTicket({ ...payload, creator: user.username, limit: 24 })
      msg.success(`Đã tạo ${t.id}`)
      navigate(`/tickets/${t.id}`)
    } catch { msg.error('Tạo ticket thất bại') } finally { setSaving(false) }
  }

  return (
    <>
      {ctx}
      <PageHeader title="Tạo ticket" subtitle="SC-E02" />
      <Card style={{ maxWidth: 720 }}>
        <Form layout="vertical" onFinish={onFinish} initialValues={{ priority: 'MEDIUM' }}>
          <Form.Item name="title" label="Tiêu đề" rules={[{ required: true, message: 'Nhập tiêu đề' }, { max: 200 }]}><Input /></Form.Item>
          <Form.Item name="category" label="Danh mục" rules={[{ required: true, message: 'Chọn danh mục' }]}>
            <Select options={categories.map((c) => ({ value: c, label: c }))} />
          </Form.Item>
          <Form.Item name="priority" label="Mức ưu tiên">
            <Select options={Object.entries(PRIORITY_META).map(([value, m]) => ({ value, label: m.label }))} />
          </Form.Item>
          <Form.Item name="description" label="Mô tả" rules={[{ required: true, message: 'Nhập mô tả' }]}><Input.TextArea rows={5} /></Form.Item>
          <Form.Item name="files" label="Tệp đính kèm" valuePropName="fileList" getValueFromEvent={(e) => e?.fileList}
            extra={`Tối đa ${FILE_RULES.maxFiles} file, ≤ ${FILE_RULES.maxSizeMB} MB; ${FILE_RULES.extensions.join(', ')}`}>
            <Upload multiple maxCount={FILE_RULES.maxFiles}
              beforeUpload={(f) => { const err = validateFile(f); if (err) { msg.error(err); return Upload.LIST_IGNORE } return false }}>
              <Button icon={<UploadOutlined />}>Chọn file</Button>
            </Upload>
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={saving}>Gửi ticket</Button>
        </Form>
      </Card>
    </>
  )
}
