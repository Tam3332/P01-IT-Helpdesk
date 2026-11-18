import { useEffect, useState } from 'react'
import Icon from '@/components/Icon'
import PageHeader from '@/components/PageHeader'
import Pagination from '@/components/Pagination'
import CustomSelect from '@/components/CustomSelect'
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
import { adminCreateUser, adminListUsers, adminResetPassword, adminSetRole, adminSetStatus, adminUpdateUser } from '@/services/referenceService'
import { initials } from '@/utils/format'
import { t } from '@/i18n'

const ROLE_KEYS = Object.values(ROLES)
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const NOT_SUPPORTED = 'Backend chưa hỗ trợ chức năng này (API quản lý người dùng chưa được triển khai).'
const wrap = async (fn, fallback) => { try { return await fn() } catch (e) { throw new Error([404, 405].includes(e?.response?.status) ? t(NOT_SUPPORTED) : apiError(e, t(fallback))) } }

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

  const all = (data.data?.content || []).map((u) => {
    const savedAvatar = localStorage.getItem(`hd_avatar_${u.id}`)
    const savedProfile = (() => {
      try {
        return JSON.parse(localStorage.getItem(`hd_profile_${u.id}`) || '{}')
      } catch {
        return {}
      }
    })()
    return {
      ...u,
      fullName: savedProfile.fullName || u.fullName,
      phone: savedProfile.phone || u.phone,
      avatarUrl: savedAvatar || u.avatarUrl,
    }
  })
  const needle = q.trim().toLowerCase()
  // Bộ lọc áp dụng trên trang đang xem (API /admin/users hiện chỉ có page, size)
  const list = all.filter((u) => (!needle || `${u.fullName} ${u.email} ${u.username}`.toLowerCase().includes(needle)) && (!role || u.role === role) && (!st || (st === 'on') === !!u.isActive))

  return (
    <>
      <PageHeader title={t(item.label)} subtitle={t(item.subtitle)} />
      <section className="card">
        <div className="ch">
          <h2>{data.data ? t('{n} tài khoản', { n: data.data.totalElements }) : t('Tài khoản')}</h2>
          <button type="button" className="btn pri" onClick={() => setDialog({ type: 'add' })}><Icon name="plus" />{t('Tạo tài khoản')}</button>
        </div>
        <div className="filters users-f">
          <div className={`sw${q ? ' has' : ''}`}>
            <Icon name="search" />
            <input className="in" value={q} onChange={(e) => setQ(e.target.value)} placeholder={t('Tìm theo họ tên, email hoặc tên đăng nhập')} aria-label={t('Tìm tài khoản')} />
            <button type="button" className="clr" aria-label={t('Xóa từ khóa')} onClick={() => setQ('')}><Icon name="x" /></button>
          </div>
          <CustomSelect
            value={role}
            onChange={setRole}
            options={[
              { value: '', label: t('Mọi vai trò') },
              ...ROLE_KEYS.map((r) => ({ value: r, label: ROLE_LABEL[r] })),
            ]}
            ariaLabel={t('Lọc theo vai trò')}
          />
          <CustomSelect
            value={st}
            onChange={setSt}
            options={[
              { value: '', label: t('Mọi trạng thái') },
              { value: 'on', label: t('Đang hoạt động') },
              { value: 'off', label: t('Đã khóa') },
            ]}
            ariaLabel={t('Lọc theo trạng thái')}
          />
        </div>
        <Async state={data} notReadyText={t('Backend chưa có API quản lý người dùng (GET /api/v1/admin/users). Màn hình sẽ hiện dữ liệu khi API được triển khai.')}>
          {list.length ? (
            <div className="tw">
              <table>
                <thead><tr><th>{t('Họ tên')}</th><th>Email</th><th>{t('Tên đăng nhập')}</th><th>{t('Vai trò')}</th><th>{t('Phòng ban')}</th><th>{t('Trạng thái')}</th><th /></tr></thead>
                <tbody>
                  {list.map((u) => {
                    const self = u.id === me.id
                    const dis = self ? { disabled: true, title: t('Không thể thay đổi tài khoản của chính bạn') } : {}
                    return (
                      <tr
                        key={u.id}
                        onClick={() => setDialog({ type: 'edit', user: u })}
                        style={{ cursor: 'pointer' }}
                        title={t('Bấm để xem và chỉnh sửa thông tin')}
                      >
                        <td className="nw">
                          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                            <div className="av" style={u.avatarUrl ? { padding: 0, overflow: 'hidden', flexShrink: 0 } : { flexShrink: 0 }}>
                              {u.avatarUrl ? (
                                <img src={u.avatarUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%' }} />
                              ) : (
                                initials(u.fullName)
                              )}
                            </div>
                            <div style={{ whiteSpace: 'nowrap' }}>
                              <b>{u.fullName}</b>
                              {u.phone && <span className="sm muted" style={{ display: 'block' }}>{u.phone}</span>}
                            </div>
                            {self && <span className="sm muted" style={{ whiteSpace: 'nowrap' }}>{t('(bạn)')}</span>}
                          </div>
                        </td>
                        <td>{u.email}</td>
                        <td><code>{u.username}</code></td>
                        <td><RoleBadge label={t(ROLE_LABEL[u.role] || u.role)} /></td>
                        <td>{u.department || '—'}</td>
                        <td>{u.isActive ? <span className="badge s-done">{t('Đang hoạt động')}</span> : <span className="badge s-breach">{t('Đã khóa')}</span>}</td>
                        <td className="nw" onClick={(e) => e.stopPropagation()}>
                          <button type="button" className="btn sm" onClick={() => setDialog({ type: 'role', user: u })} {...dis}>{t('Gán vai trò')}</button>{' '}
                          <button type="button" className="btn sm" onClick={() => setDialog({ type: 'resetPassword', user: u })} {...dis}>{t('Đặt lại mật khẩu')}</button>{' '}
                          <button type="button" className="btn sm" onClick={() => setDialog({ type: 'lock', user: u })} {...dis}>{u.isActive ? t('Khóa') : t('Kích hoạt')}</button>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState title={t('Không có tài khoản phù hợp')}>{t('Thử đổi từ khóa hoặc bỏ bớt bộ lọc.')}</EmptyState>
          )}
          {data.data && <Pagination page={data.data.page} totalPages={data.data.totalPages} totalElements={data.data.totalElements} size={data.data.size || LIMITS.pageSize} onPage={setPage} />}
        </Async>
      </section>

      {dialog?.type === 'add' && <CreateUserDialog onClose={() => setDialog(null)} onDone={done} />}
      {dialog?.type === 'edit' && (
        <EditUserDialog
          user={dialog.user}
          currentUser={me}
          onClose={() => setDialog(null)}
          onDone={done}
          onOpenResetPassword={(u) => setDialog({ type: 'resetPassword', user: u })}
        />
      )}
      {dialog?.type === 'role' && <RoleDialog user={dialog.user} onClose={() => setDialog(null)} onDone={done} />}
      {dialog?.type === 'resetPassword' && <ResetPasswordDialog user={dialog.user} onClose={() => setDialog(null)} onDone={done} />}
      {dialog?.type === 'lock' && (
        <Modal
          title={dialog.user.isActive ? t('Khóa tài khoản') : t('Kích hoạt tài khoản')} okText={dialog.user.isActive ? t('Khóa tài khoản') : t('Kích hoạt')} okClass={dialog.user.isActive ? 'danger' : 'pri'}
          onClose={() => setDialog(null)}
          onOk={async () => { await wrap(() => adminSetStatus(dialog.user.id, !dialog.user.isActive), 'Không đổi được trạng thái.'); done(dialog.user.isActive ? t('Đã khóa tài khoản.') : t('Đã kích hoạt tài khoản.')) }}
        >
          <p>{dialog.user.isActive ? <>{t('Khóa')} <b>{dialog.user.fullName}</b> ({dialog.user.username})? {t('Người này sẽ không đăng nhập được cho đến khi được kích hoạt lại.')}</> : <>{t('Kích hoạt lại tài khoản của')} <b>{dialog.user.fullName}</b> ({dialog.user.username})?</>}</p>
        </Modal>
      )}
    </>
  )
}

function CreateUserDialog({ onClose, onDone }) {
  const [f, setF] = useState({ username: '', email: '', fullName: '', password: '', department: '', role: '' })
  const set = (k) => (e) => setF((s) => ({ ...s, [k]: e.target.value }))
  return (
    <Modal title={t('Tạo tài khoản')} okText={t('Tạo tài khoản')} onClose={onClose} onOk={async () => {
      if (!f.username.trim()) throw new Error(t('Vui lòng nhập tên tài khoản.'))
      if (!EMAIL_RE.test(f.email.trim())) throw new Error(t('Vui lòng nhập email hợp lệ.'))
      if (!f.fullName.trim()) throw new Error(t('Vui lòng nhập họ tên.'))
      if (!f.password) throw new Error(t('Vui lòng nhập mật khẩu.'))
      if (!ROLE_KEYS.includes(f.role)) throw new Error(t('Vui lòng chọn vai trò.'))
      await wrap(() => adminCreateUser({ username: f.username.trim(), email: f.email.trim(), fullName: f.fullName.trim(), password: f.password, department: f.department.trim() || undefined, role: f.role }), 'Không tạo được tài khoản.')
      onDone(t('Đã tạo tài khoản {name}.', { name: f.username.trim() }))
    }}>
      <div className="row2">
        <Field label={t('Tên tài khoản')} required htmlFor="uf-login" hint={t('Định danh nội bộ, không dùng để đăng nhập. Đăng nhập bằng email.')}><input className="in" id="uf-login" maxLength={50} autoComplete="off" value={f.username} onChange={set('username')} /></Field>
        <Field label="Email" required htmlFor="uf-email"><input className="in" id="uf-email" type="email" maxLength={120} autoComplete="off" value={f.email} onChange={set('email')} /></Field>
      </div>
      <Field label={t('Họ tên')} required htmlFor="uf-name"><input className="in" id="uf-name" maxLength={100} value={f.fullName} onChange={set('fullName')} /></Field>
      <Field label={t('Mật khẩu')} required htmlFor="uf-pass" hint={t('Backend băm mật khẩu bằng bcrypt.')}><input className="in" id="uf-pass" type="password" autoComplete="new-password" value={f.password} onChange={set('password')} /></Field>
      <div className="row2">
        <Field label={t('Phòng ban')} htmlFor="uf-dept"><input className="in" id="uf-dept" maxLength={60} value={f.department} onChange={set('department')} placeholder={t('ví dụ: Kế toán')} /></Field>
        <Field label={t('Vai trò')} required htmlFor="uf-role">
          <CustomSelect
            value={f.role}
            onChange={(val) => setF((prev) => ({ ...prev, role: val }))}
            placeholder={t('Chọn vai trò')}
            options={[
              { value: '', label: t('Chọn vai trò') },
              ...ROLE_KEYS.map((r) => ({ value: r, label: ROLE_LABEL[r] })),
            ]}
            ariaLabel={t('Chọn vai trò')}
          />
        </Field>
      </div>
    </Modal>
  )
}

function RoleDialog({ user, onClose, onDone }) {
  const [role, setRole] = useState(user.role)
  return (
    <Modal title={t('Gán vai trò')} okText={t('Lưu vai trò')} onClose={onClose} onOk={async () => {
      if (role === user.role) throw new Error(t('Vai trò mới trùng với vai trò hiện tại.'))
      await wrap(() => adminSetRole(user.id, role), 'Không đổi được vai trò.')
      onDone(t('Đã đổi vai trò của {name}.', { name: user.fullName }))
    }}>
      <p className="muted">{t('Đổi vai trò của')} <b>{user.fullName}</b> ({user.username}). {t('Hiện tại:')} <b>{t(ROLE_LABEL[user.role] || user.role)}</b>. {t('Thao tác được ghi vào nhật ký.')}</p>
      <Field label={t('Vai trò mới')} htmlFor="ur-role">
        <CustomSelect
          value={role}
          onChange={setRole}
          options={ROLE_KEYS.map((r) => ({ value: r, label: ROLE_LABEL[r] }))}
          ariaLabel={t('Chọn vai trò mới')}
        />
      </Field>
    </Modal>
  )
}

function ResetPasswordDialog({ user, onClose, onDone }) {
  const [pass, setPass] = useState('Demo@12345')
  const [useDefault, setUseDefault] = useState(true)

  return (
    <Modal
      title={t('Đặt lại mật khẩu')}
      okText={t('Đặt lại mật khẩu')}
      okClass="warn"
      onClose={onClose}
      onOk={async () => {
        const targetPass = useDefault ? 'Demo@12345' : pass.trim()
        if (!targetPass) throw new Error(t('Vui lòng nhập mật khẩu.'))
        if (targetPass.length < 6) throw new Error(t('Mật khẩu phải có ít nhất 6 ký tự.'))
        await wrap(() => adminResetPassword(user.id, targetPass), 'Không đặt lại được mật khẩu.')
        onDone(t('Đã đặt lại mật khẩu cho {name}. Người dùng phải đổi mật khẩu khi đăng nhập.', { name: user.fullName }))
      }}
    >
      <p className="muted">
        {t('Cấp lại mật khẩu cho tài khoản')} <b>{user.fullName}</b> (<code>{user.email}</code>).
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
          <span>{t('Dùng mật khẩu mặc định')} (<code>Demo@12345</code>)</span>
        </label>
      </div>
      {!useDefault && (
        <Field label={t('Mật khẩu tạm thời mới')} required htmlFor="rp-pass" hint={t('Tối thiểu 6 ký tự')}>
          <input
            className="in"
            id="rp-pass"
            type="text"
            autoComplete="off"
            value={pass}
            onChange={(e) => setPass(e.target.value)}
            placeholder={t('Nhập mật khẩu tạm thời')}
          />
        </Field>
      )}
      <div className="banner info" style={{ marginTop: 12 }}>
        <Icon name="shield" />
        <div className="sm">
          {t('Sau khi đặt lại, hệ thống sẽ bật cờ')} <b>{t('bắt buộc đổi mật khẩu')}</b>. {t('Người dùng sẽ phải tự tạo mật khẩu mới ngay trong lần đăng nhập tiếp theo.')}
        </div>
      </div>
    </Modal>
  )
}

function EditUserDialog({ user, currentUser, onClose, onDone, onOpenResetPassword }) {
  const isSelf = user.id === currentUser?.id
  const [f, setF] = useState({
    fullName: user.fullName || '',
    phone: user.phone || '',
    department: user.department || '',
    role: user.role,
    isActive: !!user.isActive,
  })

  const set = (k) => (e) => setF((s) => ({ ...s, [k]: e.target.value }))

  return (
    <Modal
      title={t('Thông tin người dùng')}
      okText={t('Lưu thay đổi')}
      onClose={onClose}
      onOk={async () => {
        if (!f.fullName.trim()) throw new Error(t('Vui lòng nhập họ tên.'))
        if (!ROLE_KEYS.includes(f.role)) throw new Error(t('Vui lòng chọn vai trò.'))
        await wrap(
          () =>
            adminUpdateUser(user.id, {
              fullName: f.fullName.trim(),
              phone: f.phone.trim() || undefined,
              department: f.department.trim() || undefined,
              role: f.role,
              isActive: f.isActive,
            }),
          'Không cập nhật được thông tin người dùng.'
        )
        try {
          localStorage.setItem(
            `hd_profile_${user.id}`,
            JSON.stringify({
              fullName: f.fullName.trim(),
              phone: f.phone.trim(),
              department: f.department.trim(),
            })
          )
        } catch { /* không ảnh hưởng luồng chính */ }
        onDone(t('Đã cập nhật thông tin cho {name}.', { name: f.fullName.trim() }))
      }}
    >
      <div style={{ display: 'flex', gap: 16, alignItems: 'center', marginBottom: 20, paddingBottom: 16, borderBottom: '1px solid var(--border-c, rgba(255,255,255,0.08))' }}>
        <div className="av" style={{ width: 56, height: 56, fontSize: 20, flexShrink: 0, padding: 0, overflow: 'hidden' }}>
          {user.avatarUrl ? (
            <img src={user.avatarUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%' }} />
          ) : (
            initials(f.fullName || user.fullName)
          )}
        </div>
        <div style={{ minWidth: 0, flex: 1 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
            <h3 style={{ margin: 0, fontSize: 17 }}>{f.fullName || user.fullName}</h3>
            {isSelf && <span className="badge muted sm">{t('(Tài khoản của bạn)')}</span>}
          </div>
          <div className="sm muted" style={{ marginTop: 2, display: 'flex', gap: 12, flexWrap: 'wrap' }}>
            <span><code>{user.username}</code></span>
            <span>•</span>
            <span>{user.email}</span>
          </div>
        </div>
      </div>

      <div className="row2">
        <Field label={t('Họ và tên')} required htmlFor="eu-fullname">
          <input className="in" id="eu-fullname" maxLength={100} value={f.fullName} onChange={set('fullName')} placeholder={t('Nhập họ và tên')} />
        </Field>
        <Field label={t('Số điện thoại')} htmlFor="eu-phone">
          <input className="in" id="eu-phone" maxLength={20} value={f.phone} onChange={set('phone')} placeholder={t('ví dụ: 0912345678')} />
        </Field>
      </div>

      <div className="row2">
        <Field label={t('Phòng ban')} htmlFor="eu-dept">
          <input className="in" id="eu-dept" maxLength={60} value={f.department} onChange={set('department')} placeholder={t('ví dụ: Kế toán, Nhân sự, IT...')} />
        </Field>
        <Field label={t('Vai trò')} required htmlFor="eu-role" hint={isSelf ? t('Không thể tự đổi vai trò của chính mình') : undefined}>
          <CustomSelect
            value={f.role}
            disabled={isSelf}
            onChange={(val) => setF((prev) => ({ ...prev, role: val }))}
            options={ROLE_KEYS.map((r) => ({ value: r, label: ROLE_LABEL[r] }))}
            ariaLabel={t('Chọn vai trò')}
          />
        </Field>
      </div>

      <div className="row2">
        <Field label={t('Trạng thái hoạt động')} htmlFor="eu-status" hint={isSelf ? t('Không thể tự khóa tài khoản của chính mình') : undefined}>
          <CustomSelect
            value={f.isActive ? 'active' : 'locked'}
            disabled={isSelf}
            onChange={(val) => setF((prev) => ({ ...prev, isActive: val === 'active' }))}
            options={[
              { value: 'active', label: t('Đang hoạt động') },
              { value: 'locked', label: t('Đã khóa') },
            ]}
            ariaLabel={t('Trạng thái hoạt động')}
          />
        </Field>
        <Field label={t('Bảo mật & Mật khẩu')}>
          <button
            type="button"
            className="btn"
            style={{ width: '100%', justifyContent: 'center' }}
            disabled={isSelf}
            onClick={() => {
              onClose()
              onOpenResetPassword(user)
            }}
          >
            <Icon name="lock" /> {t('Đặt lại mật khẩu')}
          </button>
        </Field>
      </div>

      <div className="row2" style={{ marginTop: 8 }}>
        <Field label="Email" hint={t('Định danh đăng nhập (chỉ đọc)')}>
          <input className="in" value={user.email} disabled readOnly style={{ opacity: 0.7 }} />
        </Field>
        <Field label={t('Tên đăng nhập')} hint={t('Tên tài khoản (chỉ đọc)')}>
          <input className="in" value={user.username} disabled readOnly style={{ opacity: 0.7 }} />
        </Field>
      </div>
    </Modal>
  )
}
