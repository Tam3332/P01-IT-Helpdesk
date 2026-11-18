import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import FilePicker from '@/components/FilePicker'
import CustomSelect from '@/components/CustomSelect'
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
import useUnsavedGuard from '@/hooks/useUnsavedGuard'
import { t } from '@/i18n'

export default function CreateTicketPage() {
  const item = NAV[ROLES.EMPLOYEE][2]
  const navigate = useNavigate()
  const toast = useToast()
  const cats = useAsync(listCategories, [])
  const [f, setF] = useState({ title: '', categoryId: '', impactLevel: 'MEDIUM', description: '' })
  const [files, setFiles] = useState([])
  const [errors, setErrors] = useState({})
  const [busy, setBusy] = useState(false)
  useUnsavedGuard(!busy && !!(f.title.trim() || f.description.trim()))
  // Sửa trường nào thì xóa lỗi của trường đó, tránh báo lỗi cũ sau khi đã nhập đúng
  const set = (k) => (e) => {
    setF((s) => ({ ...s, [k]: e.target.value }))
    setErrors((s) => (s[k] ? { ...s, [k]: undefined } : s))
  }

  const submit = async (e) => {
    e.preventDefault()
    if (busy) return
    const er = {}
    if (!f.title.trim()) er.title = t('Vui lòng nhập tiêu đề.')
    else if (f.title.length > LIMITS.title) er.title = t('Tiêu đề tối đa {n} ký tự.', { n: LIMITS.title })
    if (!f.categoryId) er.categoryId = t('Vui lòng chọn danh mục.')
    if (!f.description.trim()) er.description = t('Vui lòng nhập mô tả chi tiết.')
    else if (f.description.length > LIMITS.description) er.description = t('Mô tả tối đa {n} ký tự.', { n: LIMITS.description })
    setErrors((s) => ({ files: s.files, ...er }))
    if (Object.keys(er).length) {
      toast(t('Chưa gửi được. Hãy điền các trường bắt buộc.'), 'err')
      return
    }
    setBusy(true)
    let ticket
    try {
      ticket = await createTicket({ title: f.title.trim(), description: f.description.trim(), categoryId: Number(f.categoryId), impactLevel: f.impactLevel })
    } catch (err) {
      toast(apiError(err, t('Không tạo được ticket.')), 'err')
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
        failed.push(`"${file.name}": ${apiError(err, t('tải lên thất bại'))}`)
      }
    }
    toast(t('Đã tạo ticket {n}.', { n: ticket.ticketNumber ?? '' }).replace(' .', '.'))
    if (failed.length) toast(`${t('Có tệp chưa tải lên được.')} ${failed.join(' ')}`, 'err')
    try {
      window.dispatchEvent(new Event('ticket_created'))
      window.dispatchEvent(new Event('ticket_updated'))
    } catch { /* không ảnh hưởng luồng chính */ }
    navigate(`/employee/tickets/${encodeURIComponent(ref)}`, { replace: true })
  }

  return (
    <div className="narrow">
      <PageHeader title={t(item.label)} subtitle={t(item.subtitle)} />
      <Async state={cats}>
        <form className="card" onSubmit={submit} noValidate>
          <div className="cb" style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
            <Field label={t('Tiêu đề')} required htmlFor="f-title" error={errors.title} hint={t('{n}/{max} ký tự', { n: f.title.length, max: LIMITS.title })}>
              <input className="in" id="f-title" maxLength={LIMITS.title} value={f.title} onChange={set('title')} placeholder={t('Ví dụ: Không thể kết nối Wi-Fi văn phòng')} />
            </Field>
            <div className="row2">
              <Field label={t('Danh mục')} required htmlFor="f-cat" error={errors.categoryId}>
                <CustomSelect
                  value={f.categoryId}
                  onChange={(val) => setF((prev) => ({ ...prev, categoryId: val }))}
                  placeholder={t('Chọn danh mục')}
                  options={[
                    { value: '', label: t('Chọn danh mục') },
                    ...(cats.data || []).map((c) => ({ value: c.id, label: c.categoryName })),
                  ]}
                  ariaLabel={t('Chọn danh mục')}
                />
              </Field>
              <Field label={t('Mức độ ảnh hưởng')} htmlFor="f-imp" hint={t('IT Manager sẽ chốt mức ưu tiên chính thức khi phân công.')}>
                <CustomSelect
                  value={f.impactLevel}
                  onChange={(val) => setF((prev) => ({ ...prev, impactLevel: val }))}
                  options={PRIORITY_KEYS.map((p) => ({ value: p, label: t(PRIORITY[p].label) }))}
                  ariaLabel={t('Mức độ ảnh hưởng')}
                />
              </Field>
            </div>
            <Field label={t('Mô tả chi tiết')} required htmlFor="f-desc" error={errors.description} hint={t('{n}/{max} ký tự', { n: f.description.length, max: LIMITS.description })}>
              <textarea className="ta" id="f-desc" rows={6} maxLength={LIMITS.description} value={f.description} onChange={set('description')} placeholder={t('Mô tả sự cố, thời điểm xảy ra và các bước bạn đã thử')} />
            </Field>
            <FilePicker files={files} error={errors.files} onChange={(fl, msg) => { setFiles(fl); setErrors((s) => ({ ...s, files: msg })); if (msg) toast(t('Có tệp không được đính kèm.'), 'err') }} />
            <div className="acts" style={{ justifyContent: 'flex-end' }}>
              <button type="button" className="btn" onClick={() => navigate('/employee')} disabled={busy}>{t('Hủy')}</button>
              <button type="submit" className="btn pri" disabled={busy} aria-busy={busy}>
                <Icon name="send" />{busy ? t('Đang gửi...') : t('Gửi yêu cầu')}
              </button>
            </div>
          </div>
        </form>
      </Async>
    </div>
  )
}
