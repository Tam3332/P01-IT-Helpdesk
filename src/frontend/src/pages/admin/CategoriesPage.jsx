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
import { adminCreateCategory, adminListCategories, adminSetCategoryActive, adminUpdateCategory } from '@/services/referenceService'

export default function CategoriesPage() {
  const item = NAV[ROLES.ADMIN].find((i) => i.path === '/admin/categories')
  const navigate = useNavigate()
  const toast = useToast()
  const list = useAsync(adminListCategories, [])
  const [open, setOpen] = useState(false)
  const [edit, setEdit] = useState(null)
  const [f, setF] = useState({ categoryCode: '', categoryName: '', defaultPriority: 'MEDIUM' })
  const set = (k) => (e) => setF((s) => ({ ...s, [k]: e.target.value }))
  const items = list.data || []
  const toggle = async (c) => {
    try {
      await adminSetCategoryActive(c.id, c.isActive === false)
      toast(c.isActive === false ? 'Đã bật danh mục.' : 'Đã tắt danh mục.')
      list.reload()
    } catch (e) {
      toast(apiError(e, 'Không đổi được trạng thái.'), 'err')
    }
  }

  return (
    <>
      <PageHeader title={item.label} subtitle={item.subtitle} />
      <div><button type="button" className="btn ghost sm" onClick={() => navigate('/admin/settings')}><Icon name="back" />Cài đặt hệ thống</button></div>
      <section className="card">
        <div className="ch"><h2>{items.length} danh mục</h2><button type="button" className="btn pri" onClick={() => setOpen(true)}><Icon name="plus" />Thêm danh mục</button></div>
        <Async state={list}>
          {items.length ? (
            <div className="tw" style={{ marginTop: 14 }}>
              <table>
                <thead><tr><th>Mã</th><th>Tên danh mục</th><th>Ưu tiên mặc định</th><th>Trạng thái</th><th /></tr></thead>
                <tbody>
                  {items.map((c) => (
                    <tr key={c.id}>
                      <td><code>{c.categoryCode}</code></td>
                      <td><b>{c.categoryName}</b></td>
                      <td><PriorityBadge priority={c.defaultPriority} /></td>
                      <td><span className={`badge ${c.isActive === false ? 'b-Closed' : 's-done'}`}>{c.isActive === false ? 'Tạm ẩn' : 'Đang dùng'}</span></td>
                      <td>
                        <button type="button" className="btn sm" onClick={() => setEdit(c)}><Icon name="edit" />Sửa</button>{' '}
                        <button type="button" className="btn sm" onClick={() => toggle(c)}>{c.isActive === false ? 'Bật' : 'Tắt'}</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState icon="tag" title="Chưa có danh mục nào" />
          )}
        </Async>
      </section>
      <p className="sm muted">Danh mục không xóa được; tắt danh mục để ẩn khỏi form tạo ticket.</p>
      {edit && <EditCategory cat={edit} onClose={() => setEdit(null)} onSaved={() => list.reload()} />}
      {open && (
        <Modal title="Thêm danh mục" okText="Thêm" onClose={() => setOpen(false)} onOk={async () => {
          const code = f.categoryCode.trim().toUpperCase()
          if (!/^[A-Z0-9_]+$/.test(code)) throw new Error('Mã chỉ gồm chữ in hoa, số hoặc dấu gạch dưới.')
          if (!f.categoryName.trim()) throw new Error('Vui lòng nhập tên danh mục.')
          try {
            await adminCreateCategory({ categoryCode: code, categoryName: f.categoryName.trim(), defaultPriority: f.defaultPriority })
          } catch (e) {
            throw new Error(e?.response?.status === 409 ? 'Mã hoặc tên danh mục đã tồn tại.' : apiError(e, 'Không thêm được danh mục.'))
          }
          toast('Đã thêm danh mục.')
          setF({ categoryCode: '', categoryName: '', defaultPriority: 'MEDIUM' })
          list.reload()
        }}>
          <Field label="Mã danh mục" required htmlFor="cf-code" hint="Chữ in hoa, số hoặc dấu gạch dưới. Không đổi sau khi tạo."><input className="in" id="cf-code" maxLength={30} value={f.categoryCode} onChange={set('categoryCode')} placeholder="ví dụ: NETWORK" /></Field>
          <Field label="Tên danh mục" required htmlFor="cf-name"><input className="in" id="cf-name" maxLength={100} value={f.categoryName} onChange={set('categoryName')} /></Field>
          <Field label="Mức ưu tiên mặc định" htmlFor="cf-pri">
            <select className="sel" id="cf-pri" value={f.defaultPriority} onChange={set('defaultPriority')}>{PRIORITY_KEYS.map((p) => <option key={p} value={p}>{PRIORITY[p].label}</option>)}</select>
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
    <Modal title="Sửa danh mục" okText="Lưu thay đổi" onClose={onClose} onOk={async () => {
      if (!name.trim()) throw new Error('Vui lòng nhập tên danh mục.')
      try {
        await adminUpdateCategory(cat.id, { categoryName: name.trim(), defaultPriority: pri })
      } catch (e) {
        throw new Error(apiError(e, 'Không lưu được danh mục.'))
      }
      toast('Đã lưu thay đổi.')
      onSaved()
    }}>
      <Field label="Mã danh mục"><input className="in" value={cat.categoryCode} disabled /></Field>
      <Field label="Tên danh mục" required htmlFor="ce-name"><input className="in" id="ce-name" maxLength={100} value={name} onChange={(e) => setName(e.target.value)} /></Field>
      <Field label="Mức ưu tiên mặc định" htmlFor="ce-pri">
        <select className="sel" id="ce-pri" value={pri} onChange={(e) => setPri(e.target.value)}>{PRIORITY_KEYS.map((p) => <option key={p} value={p}>{PRIORITY[p].label}</option>)}</select>
      </Field>
    </Modal>
  )
}
