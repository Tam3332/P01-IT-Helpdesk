import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import Modal, { Field } from '@/components/Modal'
import { PriorityBadge } from '@/components/Badges'
import { Async, EmptyState } from '@/components/StateViews'
import { useToast } from '@/context/ToastContext'
import useAsync from '@/hooks/useAsync'
import { PRIORITY, PRIORITY_KEYS } from '@/config/domain'
import { NAV } from '@/config/nav'
import { ROLES } from '@/config/constants'
import { apiError } from '@/services/apiClient'
import { adminCreateCategory, adminDeleteCategory, adminListCategories, adminSetCategoryActive, adminUpdateCategory } from '@/services/referenceService'
import { t } from '@/i18n'

// Backend không dùng 409: trùng mã/tên trả 400 kèm error.code (SRS UC-10 3a)
const DUPLICATE = { CATEGORY_CODE_DUPLICATE: 'Mã danh mục đã tồn tại.', CATEGORY_NAME_DUPLICATE: 'Tên danh mục đã tồn tại.' }
const duplicateMessage = (e) => (e?.response?.status === 400 && DUPLICATE[e.response.data?.error?.code] ? t(DUPLICATE[e.response.data.error.code]) : undefined)

export default function CategoriesPage() {
  const item = NAV[ROLES.ADMIN].find((i) => i.path === '/admin/categories')
  const navigate = useNavigate()
  const toast = useToast()
  const list = useAsync(adminListCategories, [])
  const [open, setOpen] = useState(false)
  const [edit, setEdit] = useState(null)
  const [del, setDel] = useState(null)
  const [f, setF] = useState({ categoryCode: '', categoryName: '', defaultPriority: 'MEDIUM' })
  const set = (k) => (e) => setF((s) => ({ ...s, [k]: e.target.value }))
  const items = list.data || []
  const toggle = async (c) => {
    try {
      await adminSetCategoryActive(c.id, c.isActive === false)
      toast(c.isActive === false ? t('Đã bật danh mục.') : t('Đã tắt danh mục.'))
      list.reload()
    } catch (e) {
      toast(apiError(e, t('Không đổi được trạng thái.')), 'err')
    }
  }

  return (
    <>
      <PageHeader title={t(item.label)} subtitle={t(item.subtitle)} />
      <div><button type="button" className="btn ghost sm" onClick={() => navigate('/admin/settings')}><Icon name="back" />{t('Cài đặt hệ thống')}</button></div>
      <section className="card">
        <div className="ch"><h2>{t('{n} danh mục', { n: items.length })}</h2><button type="button" className="btn pri" onClick={() => setOpen(true)}><Icon name="plus" />{t('Thêm danh mục')}</button></div>
        <Async state={list}>
          {items.length ? (
            <div className="tw" style={{ marginTop: 14 }}>
              <table>
                <thead><tr><th>{t('Mã')}</th><th>{t('Tên danh mục')}</th><th>{t('Ưu tiên mặc định')}</th><th>{t('Trạng thái')}</th><th /></tr></thead>
                <tbody>
                  {items.map((c) => (
                    <tr key={c.id}>
                      <td><code>{c.categoryCode}</code></td>
                      <td><b>{c.categoryName}</b></td>
                      <td><PriorityBadge priority={c.defaultPriority} /></td>
                      <td><span className={`badge ${c.isActive === false ? 'b-Closed' : 's-done'}`}>{c.isActive === false ? t('Tạm ẩn') : t('Đang dùng')}</span></td>
                      <td>
                        <button type="button" className="btn sm" onClick={() => setEdit(c)}><Icon name="edit" />{t('Sửa')}</button>{' '}
                        <button type="button" className="btn sm" onClick={() => toggle(c)}>{c.isActive === false ? t('Bật') : t('Tắt')}</button>{' '}
                        <button type="button" className="btn sm" onClick={() => setDel(c)}>{t('Xóa')}</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState icon="tag" title={t('Chưa có danh mục nào')} />
          )}
        </Async>
      </section>
      <p className="sm muted">{t('Chỉ xóa được danh mục chưa có ticket nào tham chiếu. Danh mục đã có ticket thì tắt để ẩn khỏi form tạo ticket.')}</p>
      {del && <DeleteCategory cat={del} onClose={() => setDel(null)} onDone={() => list.reload()} />}
      {edit && <EditCategory cat={edit} onClose={() => setEdit(null)} onSaved={() => list.reload()} />}
      {open && (
        <Modal title={t('Thêm danh mục')} okText={t('Thêm')} onClose={() => setOpen(false)} onOk={async () => {
          const code = f.categoryCode.trim().toUpperCase()
          if (!/^[A-Z0-9_]+$/.test(code)) throw new Error(t('Mã chỉ gồm chữ in hoa, số hoặc dấu gạch dưới.'))
          if (!f.categoryName.trim()) throw new Error(t('Vui lòng nhập tên danh mục.'))
          try {
            await adminCreateCategory({ categoryCode: code, categoryName: f.categoryName.trim(), defaultPriority: f.defaultPriority })
          } catch (e) {
            throw new Error(duplicateMessage(e) || apiError(e, t('Không thêm được danh mục.')))
          }
          toast(t('Đã thêm danh mục.'))
          setF({ categoryCode: '', categoryName: '', defaultPriority: 'MEDIUM' })
          list.reload()
        }}>
          <Field label={t('Mã danh mục')} required htmlFor="cf-code" hint={t('Chữ in hoa, số hoặc dấu gạch dưới. Không đổi sau khi tạo.')}><input className="in" id="cf-code" maxLength={30} value={f.categoryCode} onChange={set('categoryCode')} placeholder={t('ví dụ: NETWORK')} /></Field>
          <Field label={t('Tên danh mục')} required htmlFor="cf-name"><input className="in" id="cf-name" maxLength={100} value={f.categoryName} onChange={set('categoryName')} /></Field>
          <Field label={t('Mức ưu tiên mặc định')} htmlFor="cf-pri">
            <select className="sel" id="cf-pri" value={f.defaultPriority} onChange={set('defaultPriority')}>{PRIORITY_KEYS.map((p) => <option key={p} value={p}>{t(PRIORITY[p].label)}</option>)}</select>
          </Field>
        </Modal>
      )}
    </>
  )
}

function EditCategory({ cat, onClose, onSaved }) {
  const toast = useToast()
  const [name, setName] = useState(cat.categoryName)
  const [pri, setPri] = useState(cat.defaultPriority)
  return (
    <Modal title={t('Sửa danh mục')} okText={t('Lưu thay đổi')} onClose={onClose} onOk={async () => {
      if (!name.trim()) throw new Error(t('Vui lòng nhập tên danh mục.'))
      try {
        await adminUpdateCategory(cat.id, { categoryName: name.trim(), defaultPriority: pri })
      } catch (e) {
        throw new Error(duplicateMessage(e) || apiError(e, t('Không lưu được danh mục.')))
      }
      toast(t('Đã lưu thay đổi.'))
      onSaved()
    }}>
      <Field label={t('Mã danh mục')}><input className="in" value={cat.categoryCode} disabled /></Field>
      <Field label={t('Tên danh mục')} required htmlFor="ce-name"><input className="in" id="ce-name" maxLength={100} value={name} onChange={(e) => setName(e.target.value)} /></Field>
      <Field label={t('Mức ưu tiên mặc định')} htmlFor="ce-pri">
        <select className="sel" id="ce-pri" value={pri} onChange={(e) => setPri(e.target.value)}>{PRIORITY_KEYS.map((p) => <option key={p} value={p}>{t(PRIORITY[p].label)}</option>)}</select>
      </Field>
    </Modal>
  )
}

function DeleteCategory({ cat, onClose, onDone }) {
  const toast = useToast()
  return (
    <Modal title={t('Xóa danh mục')} okText={t('Xóa vĩnh viễn')} okClass="danger" onClose={onClose} onOk={async () => {
      try {
        await adminDeleteCategory(cat.id)
      } catch (e) {
        if (e?.response?.status === 404) onDone() // danh mục đã bị xóa ở nơi khác: làm mới danh sách
        throw new Error(apiError(e, t('Không xóa được danh mục.')))
      }
      toast(t('Đã xóa danh mục.'))
      onDone()
    }}>
      <p>{t('Xóa vĩnh viễn danh mục')} <b>{cat.categoryName}</b> (<code>{cat.categoryCode}</code>)? {t('Thao tác này không hoàn tác được.')}</p>
      <p className="sm muted">{t('Chỉ xóa được khi chưa có ticket nào dùng danh mục này. Nếu chỉ muốn ẩn khỏi form tạo ticket, hãy dùng nút Tắt.')}</p>
    </Modal>
  )
}
