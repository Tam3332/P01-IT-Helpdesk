import { useEffect, useState } from 'react'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import Pagination from '@/components/Pagination'
import Modal, { Field } from '@/components/Modal'
import { RoleBadge } from '@/components/Badges'
import { Async, EmptyState } from '@/components/StateViews'
import { useAuth } from '@/context/AuthContext'
import { useToast } from '@/context/ToastContext'
import useAsync from '@/hooks/useAsync'
import { LIMITS } from '@/config/domain'
import { NAV } from '@/config/nav'
import { ROLES, ROLE_LABEL } from '@/config/constants'
import { apiError } from '@/services/apiClient'
import { adminCreateUser, adminListUsers, adminResetPassword, adminSetRole, adminSetStatus } from '@/services/referenceService'
import { initials } from '@/utils/format'

const ROLE_KEYS = Object.values(ROLES)
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const NOT_SUPPORTED = 'Backend chưa hỗ trợ chức năng này (API quản lý người dùng chưa được triển khai).'
const wrap = async (fn, fallback) => { try { return await fn() } catch (e) { throw new Error([404, 405].includes(e?.response?.status) ? NOT_SUPPORTED : apiError(e, fallback)) } }

export default function UsersPage() {
  const item = NAV[ROLES.ADMIN].find((i) => i.path === '/admin/users')
  const { user: me } = useAuth()
  const toast = useToast()
  const [page, setPage] = useState(0)
  const [q, setQ] = useState('')
  const [role, setRole] = useState('')
  const [st, setSt] = useState('')
  const [dialog, setDialog] = useState(null) // { type, user? }
  const data = useAsync(() => adminListUsers({ page, size: LIMITS.pageSize }), [page])

  useEffect(() => setPage(0), [])
  const reload = () => data.reload()
  const done = (msg) => { toast(msg); reload() }

  const all = data.data?.content || []
  const needle = q.trim().toLowerCase()
  // Bộ lọc áp dụng trên trang đang xem (API /admin/users hiện chỉ có page, size)
  const list = all.filter((u) => (!needle || `${u.fullName} ${u.email} ${u.username}`.toLowerCase().includes(needle)) && (!role || u.role === role) && (!st || (st === 'on') === !!u.isActive))

  return (
    <>
      <PageHeader title={item.label} subtitle={item.subtitle} />
      <section className="card">
        <div className="ch">
          <h2>{data.data ? `${data.data.totalElements} tài khoản` : 'Tài khoản'}</h2>
          <button type="button" className="btn pri" onClick={() => setDialog({ type: 'add' })}><Icon name="plus" />Tạo tài khoản</button>
        </div>
        <div className="filters users-f">
          <div className="sw"><Icon name="search" /><input className="in" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Tìm theo họ tên, email hoặc tên đăng nhập" aria-label="Tìm tài khoản" /></div>
          <select className="sel" value={role} onChange={(e) => setRole(e.target.value)} aria-label="Lọc theo vai trò">
            <option value="">Mọi vai trò</option>
            {ROLE_KEYS.map((r) => <option key={r} value={r}>{ROLE_LABEL[r]}</option>)}
          </select>
          <select className="sel" value={st} onChange={(e) => setSt(e.target.value)} aria-label="Lọc theo trạng thái">
            <option value="">Mọi trạng thái</option>
            <option value="on">Đang hoạt động</option>
            <option value="off">Đã khóa</option>
          </select>
        </div>
        <Async state={data} notReadyText="Backend chưa có API quản lý người dùng (GET /api/v1/admin/users). Màn hình sẽ hiện dữ liệu khi API được triển khai.">
          {list.length ? (
            <div className="tw">
              <table>
                <thead><tr><th>Họ tên</th><th>Email</th><th>Tên đăng nhập</th><th>Vai trò</th><th>Phòng ban</th><th>Trạng thái</th><th /></tr></thead>
                <tbody>
                  {list.map((u) => {
                    const self = u.id === me.id
                    const dis = self ? { disabled: true, title: 'Không thể thay đổi tài khoản của chính bạn' } : {}
                    return (
                      <tr key={u.id}>
                        <td><div style={{ display: 'flex', gap: 10, alignItems: 'center' }}><div className="av">{initials(u.fullName)}</div><b>{u.fullName}</b>{self && <span className="sm muted">(bạn)</span>}</div></td>
                        <td>{u.email}</td>
                        <td><code>{u.username}</code></td>
                        <td><RoleBadge label={ROLE_LABEL[u.role] || u.role} /></td>
                        <td>{u.department || '—'}</td>
                        <td>{u.isActive ? <span className="badge s-done">Đang hoạt động</span> : <span className="badge s-breach">Đã khóa</span>}</td>
                        <td className="nw">
                          <button type="button" className="btn sm" onClick={() => setDialog({ type: 'role', user: u })} {...dis}>Gán role</button>{' '}
                          <button type="button" className="btn sm" onClick={() => setDialog({ type: 'resetPassword', user: u })} {...dis}>Đặt lại mật khẩu</button>{' '}
                          <button type="button" className="btn sm" onClick={() => setDialog({ type: 'lock', user: u })} {...dis}>{u.isActive ? 'Khóa' : 'Kích hoạt'}</button>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState title="Không có tài khoản phù hợp">Thử đổi từ khóa hoặc bỏ bớt bộ lọc.</EmptyState>
          )}
          {data.data && <Pagination page={data.data.page} totalPages={data.data.totalPages} totalElements={data.data.totalElements} size={data.data.size || LIMITS.pageSize} onPage={setPage} />}
        </Async>
      </section>

      {dialog?.type === 'add' && <CreateUserDialog onClose={() => setDialog(null)} onDone={done} />}
      {dialog?.type === 'role' && <RoleDialog user={dialog.user} onClose={() => setDialog(null)} onDone={done} />}
      {dialog?.type === 'resetPassword' && <ResetPasswordDialog user={dialog.user} onClose={() => setDialog(null)} onDone={done} />}
      {dialog?.type === 'lock' && (
        <Modal
          title={dialog.user.isActive ? 'Khóa tài khoản' : 'Kích hoạt tài khoản'} okText={dialog.user.isActive ? 'Khóa tài khoản' : 'Kích hoạt'} okClass={dialog.user.isActive ? 'danger' : 'pri'}
          onClose={() => setDialog(null)}
          onOk={async () => { await wrap(() => adminSetStatus(dialog.user.id, !dialog.user.isActive), 'Không đổi được trạng thái.'); done(dialog.user.isActive ? 'Đã khóa tài khoản.' : 'Đã kích hoạt tài khoản.') }}
        >
          <p>{dialog.user.isActive ? <>Khóa <b>{dialog.user.fullName}</b> ({dialog.user.username})? Người này sẽ không đăng nhập được cho đến khi được kích hoạt lại.</> : <>Kích hoạt lại tài khoản của <b>{dialog.user.fullName}</b> ({dialog.user.username})?</>}</p>
        </Modal>
      )}
    </>
  )
}

function CreateUserDialog({ onClose, onDone }) {
  const [f, setF] = useState({ username: '', email: '', fullName: '', password: '', department: '', role: '' })
  const set = (k) => (e) => setF((s) => ({ ...s, [k]: e.target.value }))
  return (
    <Modal title="Tạo tài khoản" okText="Tạo tài khoản" onClose={onClose} onOk={async () => {
      if (!f.username.trim()) throw new Error('Vui lòng nhập tên tài khoản.')
      if (!EMAIL_RE.test(f.email.trim())) throw new Error('Vui lòng nhập email hợp lệ.')
      if (!f.fullName.trim()) throw new Error('Vui lòng nhập họ tên.')
      if (!f.password) throw new Error('Vui lòng nhập mật khẩu.')
      if (!ROLE_KEYS.includes(f.role)) throw new Error('Vui lòng chọn vai trò.')
      await wrap(() => adminCreateUser({ username: f.username.trim(), email: f.email.trim(), fullName: f.fullName.trim(), password: f.password, department: f.department.trim() || undefined, role: f.role }), 'Không tạo được tài khoản.')
      onDone(`Đã tạo tài khoản ${f.username.trim()}.`)
    }}>
      <div className="row2">
        <Field label="Tên tài khoản" required htmlFor="uf-login" hint="Định danh nội bộ, không dùng để đăng nhập. Đăng nhập bằng email."><input className="in" id="uf-login" maxLength={50} autoComplete="off" value={f.username} onChange={set('username')} /></Field>
        <Field label="Email" required htmlFor="uf-email"><input className="in" id="uf-email" type="email" maxLength={120} autoComplete="off" value={f.email} onChange={set('email')} /></Field>
      </div>
      <Field label="Họ tên" required htmlFor="uf-name"><input className="in" id="uf-name" maxLength={100} value={f.fullName} onChange={set('fullName')} /></Field>
      <Field label="Mật khẩu" required htmlFor="uf-pass" hint="Backend băm mật khẩu bằng bcrypt."><input className="in" id="uf-pass" type="password" autoComplete="new-password" value={f.password} onChange={set('password')} /></Field>
      <div className="row2">
        <Field label="Phòng ban" htmlFor="uf-dept"><input className="in" id="uf-dept" maxLength={60} value={f.department} onChange={set('department')} placeholder="ví dụ: Kế toán" /></Field>
        <Field label="Vai trò" required htmlFor="uf-role">
          <select className="sel" id="uf-role" value={f.role} onChange={set('role')}>
            <option value="">Chọn vai trò</option>
            {ROLE_KEYS.map((r) => <option key={r} value={r}>{ROLE_LABEL[r]}</option>)}
          </select>
        </Field>
      </div>
    </Modal>
  )
}

function RoleDialog({ user, onClose, onDone }) {
  const [role, setRole] = useState(user.role)
  return (
    <Modal title="Gán vai trò" okText="Lưu vai trò" onClose={onClose} onOk={async () => {
      if (role === user.role) throw new Error('Vai trò mới trùng với vai trò hiện tại.')
      await wrap(() => adminSetRole(user.id, role), 'Không đổi được vai trò.')
      onDone(`Đã đổi vai trò của ${user.fullName}.`)
    }}>
      <p className="muted">Đổi vai trò của <b>{user.fullName}</b> ({user.username}). Hiện tại: <b>{ROLE_LABEL[user.role] || user.role}</b>. Thao tác được ghi vào nhật ký.</p>
      <Field label="Vai trò mới" htmlFor="ur-role">
        <select className="sel" id="ur-role" value={role} onChange={(e) => setRole(e.target.value)}>
          {ROLE_KEYS.map((r) => <option key={r} value={r}>{ROLE_LABEL[r]}</option>)}
        </select>
      </Field>
    </Modal>
  )
}

function ResetPasswordDialog({ user, onClose, onDone }) {
  const [pass, setPass] = useState('Demo@12345')
  const [useDefault, setUseDefault] = useState(true)

  return (
    <Modal
      title="Đặt lại mật khẩu"
      okText="Đặt lại mật khẩu"
      okClass="warn"
      onClose={onClose}
      onOk={async () => {
        const targetPass = useDefault ? 'Demo@12345' : pass.trim()
        if (!targetPass) throw new Error('Vui lòng nhập mật khẩu.')
        if (targetPass.length < 6) throw new Error('Mật khẩu phải có ít nhất 6 ký tự.')
        await wrap(() => adminResetPassword(user.id, targetPass), 'Không đặt lại được mật khẩu.')
        onDone(`Đã đặt lại mật khẩu cho ${user.fullName}. Người dùng phải đổi mật khẩu khi đăng nhập.`)
      }}
    >
      <p className="muted">
        Cấp lại mật khẩu cho tài khoản <b>{user.fullName}</b> (<code>{user.email}</code>).
      </p>
      <div style={{ marginBottom: 12 }}>
        <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
          <input
            type="checkbox"
            checked={useDefault}
            onChange={(e) => {
              setUseDefault(e.target.checked)
              if (e.target.checked) setPass('Demo@12345')
            }}
          />
          <span>Dùng mật khẩu mặc định (<code>Demo@12345</code>)</span>
        </label>
      </div>
      {!useDefault && (
        <Field label="Mật khẩu tạm thời mới" required htmlFor="rp-pass" hint="Tối thiểu 6 ký tự">
          <input
            className="in"
            id="rp-pass"
            type="text"
            autoComplete="off"
            value={pass}
            onChange={(e) => setPass(e.target.value)}
            placeholder="Nhập mật khẩu tạm thời"
          />
        </Field>
      )}
      <div className="banner info" style={{ marginTop: 12 }}>
        <Icon name="shield" />
        <div className="sm">
          Sau khi đặt lại, hệ thống sẽ bật cờ <b>bắt buộc đổi mật khẩu</b>. Người dùng sẽ phải tự tạo mật khẩu mới ngay trong lần đăng nhập tiếp theo.
        </div>
      </div>
    </Modal>
  )
}
