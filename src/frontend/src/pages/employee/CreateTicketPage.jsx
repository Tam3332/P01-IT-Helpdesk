import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import FilePicker from '@/components/FilePicker'
import { Async } from '@/components/StateViews'
import { Field } from '@/components/Modal'
import { useToast } from '@/context/ToastContext'
import useAsync from '@/hooks/useAsync'
import { LIMITS, PRIORITY, PRIORITY_KEYS } from '@/config/domain'
import { NAV } from '@/config/nav'
import { ROLES } from '@/config/constants'
import { apiError } from '@/services/apiClient'
import { createTicket, ticketRef, uploadAttachment } from '@/services/ticketService'
import { listCategories } from '@/services/referenceService'

export default function CreateTicketPage() {
  const item = NAV[ROLES.EMPLOYEE][2]
  const navigate = useNavigate()
  const toast = useToast()
  const cats = useAsync(listCategories, [])
  const [f, setF] = useState({ title: '', categoryId: '', impactLevel: 'MEDIUM', description: '' })
  const [files, setFiles] = useState([])
  const [errors, setErrors] = useState({})
  const [busy, setBusy] = useState(false)
  // Sửa trường nào thì xóa lỗi của trường đó, tránh báo lỗi cũ sau khi đã nhập đúng
  const set = (k) => (e) => {
    setF((s) => ({ ...s, [k]: e.target.value }))
    setErrors((s) => (s[k] ? { ...s, [k]: undefined } : s))
  }

  const submit = async (e) => {
    e.preventDefault()
    if (busy) return
    const er = {}
    if (!f.title.trim()) er.title = 'Vui lòng nhập tiêu đề.'
    else if (f.title.length > LIMITS.title) er.title = `Tiêu đề tối đa ${LIMITS.title} ký tự.`
    if (!f.categoryId) er.categoryId = 'Vui lòng chọn danh mục.'
    if (!f.description.trim()) er.description = 'Vui lòng nhập mô tả chi tiết.'
    else if (f.description.length > LIMITS.description) er.description = `Mô tả tối đa ${LIMITS.description} ký tự.`
    setErrors((s) => ({ files: s.files, ...er }))
    if (Object.keys(er).length) {
      toast('Chưa gửi được. Hãy điền các trường bắt buộc.', 'err')
      return
    }
    setBusy(true)
    let ticket
    try {
      ticket = await createTicket({ title: f.title.trim(), description: f.description.trim(), categoryId: Number(f.categoryId), impactLevel: f.impactLevel })
    } catch (err) {
      toast(apiError(err, 'Không tạo được ticket.'), 'err')
      setBusy(false)
      return
    }
    // Ticket đã tạo; tải tệp từng cái một, lỗi tệp nào báo tệp đó nhưng vẫn mở ticket.
    const ref = ticketRef(ticket)
    const failed = []
    for (const file of files) {
      try {
        await uploadAttachment(ref, file)
      } catch (err) {
        failed.push(`"${file.name}": ${apiError(err, 'tải lên thất bại')}`)
      }
    }
    toast(`Đã tạo ticket ${ticket.ticketNumber ?? ''}.`.trim())
    if (failed.length) toast(`Có tệp chưa tải lên được. ${failed.join(' ')}`, 'err')
    navigate(`/employee/tickets/${encodeURIComponent(ref)}`, { replace: true })
  }

  return (
    <div className="narrow">
      <PageHeader title={item.label} subtitle={item.subtitle} />
      <Async state={cats}>
        <form className="card" onSubmit={submit} noValidate>
          <div className="cb" style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
            <Field label="Tiêu đề" required htmlFor="f-title" error={errors.title} hint={`${f.title.length}/${LIMITS.title} ký tự`}>
              <input className="in" id="f-title" maxLength={LIMITS.title} value={f.title} onChange={set('title')} placeholder="Ví dụ: Không thể kết nối Wi-Fi văn phòng" />
            </Field>
            <div className="row2">
              <Field label="Danh mục" required htmlFor="f-cat" error={errors.categoryId}>
                <select className="sel" id="f-cat" value={f.categoryId} onChange={set('categoryId')}>
                  <option value="">Chọn danh mục</option>
                  {(cats.data || []).map((c) => <option key={c.id} value={c.id}>{c.categoryName}</option>)}
                </select>
              </Field>
              <Field label="Mức độ ảnh hưởng" htmlFor="f-imp" hint="IT Manager sẽ chốt mức ưu tiên chính thức khi phân công.">
                <select className="sel" id="f-imp" value={f.impactLevel} onChange={set('impactLevel')}>
                  {PRIORITY_KEYS.map((p) => <option key={p} value={p}>{PRIORITY[p].label}</option>)}
                </select>
              </Field>
            </div>
            <Field label="Mô tả chi tiết" required htmlFor="f-desc" error={errors.description} hint={`${f.description.length}/${LIMITS.description} ký tự`}>
              <textarea className="ta" id="f-desc" rows={6} maxLength={LIMITS.description} value={f.description} onChange={set('description')} placeholder="Mô tả sự cố, thời điểm xảy ra và các bước bạn đã thử" />
            </Field>
            <FilePicker files={files} error={errors.files} onChange={(fl, msg) => { setFiles(fl); setErrors((s) => ({ ...s, files: msg })); if (msg) toast('Có tệp không được đính kèm.', 'err') }} />
            <div className="acts" style={{ justifyContent: 'flex-end' }}>
              <button type="button" className="btn" onClick={() => navigate('/employee')} disabled={busy}>Hủy</button>
              <button type="submit" className="btn pri" disabled={busy}>
                <Icon name="send" />{busy ? 'Đang gửi...' : 'Gửi yêu cầu'}
              </button>
            </div>
          </div>
        </form>
      </Async>
    </div>
  )
}
