import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Icon from '@/components/Icon'
import Modal, { Field } from '@/components/Modal'
import { RoleBadge } from '@/components/Badges'
import { useAuth } from '@/context/AuthContext'
import { useToast } from '@/context/ToastContext'
import { ROLE_LABEL, STORAGE_KEYS } from '@/config/constants'
import { passwordPath } from '@/config/nav'
import { fmtDateTime, initials } from '@/utils/format'
import { getTokenExpiryMs } from '@/utils/jwt'
import { t } from '@/i18n'

const SESSION_MS = 15 * 60 * 1000 // access token sống 15 phút (ADR-02)
const MAX_FILE_SIZE = 5 * 1024 * 1024 // 5 MB
const RECOMMENDED_FILE_SIZE = 2 * 1024 * 1024 // 2 MB
const VALID_IMAGE_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp']

// Thời gian còn lại của phiên, cập nhật mỗi 15 giây
function useSessionLeft() {
  const exp = getTokenExpiryMs(sessionStorage.getItem(STORAGE_KEYS.token) || '')
  const [now, setNow] = useState(Date.now())
  useEffect(() => {
    const interval = setInterval(() => setNow(Date.now()), 15000)
    return () => clearInterval(interval)
  }, [])
  return exp ? { exp, left: Math.max(0, exp - now) } : null
}

const leftText = (ms) => {
  const m = Math.ceil(ms / 60000)
  return m <= 1 ? t('Còn dưới 1 phút') : t('Còn {m} phút', { m })
}

export default function ProfilePage() {
  const { user, logout, updateUser } = useAuth()
  const navigate = useNavigate()
  const toast = useToast()
  const session = useSessionLeft()
  const fileInputRef = useRef(null)

  const [editOpen, setEditOpen] = useState(false)
  const [fullName, setFullName] = useState(user.fullName || '')
  const [phone, setPhone] = useState(user.phone || '')
  const [department, setDepartment] = useState(user.department || '')

  useEffect(() => {
    setFullName(user.fullName || '')
    setPhone(user.phone || '')
    setDepartment(user.department || '')
  }, [user])

  const role = t(ROLE_LABEL[user.role] || user.role)
  const locked = user.isActive === false

  const handleAvatarChange = (e) => {
    const file = e.target.files?.[0]
    if (!file) return

    // 1. Kiểm tra định dạng (JPG, JPEG, PNG, WebP)
    if (!VALID_IMAGE_TYPES.includes(file.type.toLowerCase())) {
      toast(t('Định dạng không hợp lệ. Vui lòng chọn ảnh JPG, JPEG, PNG hoặc WebP.'), 'err')
      e.target.value = ''
      return
    }

    // 2. Kiểm tra dung lượng tối đa 5 MB
    if (file.size > MAX_FILE_SIZE) {
      const sizeMb = (file.size / (1024 * 1024)).toFixed(1)
      toast(t('Dung lượng ảnh ({size} MB) vượt quá mức tối đa 5 MB. Vui lòng chọn ảnh nhỏ hơn.', { size: sizeMb }), 'err')
      e.target.value = ''
      return
    }

    // 3. Khuyến nghị dung lượng <= 2 MB
    if (file.size > RECOMMENDED_FILE_SIZE) {
      const sizeMb = (file.size / (1024 * 1024)).toFixed(1)
      toast(t('Ảnh có dung lượng {size} MB (khuyến nghị tối đa 2 MB để upload nhanh). Đang tải lên...', { size: sizeMb }), 'info')
    }

    // 4. Đọc file và lưu avatar
    const reader = new FileReader()
    reader.onload = (event) => {
      const dataUrl = event.target?.result
      if (typeof dataUrl === 'string') {
        updateUser({ avatarUrl: dataUrl })
        toast(t('Đã cập nhật ảnh đại diện thành công!'), 'ok')
      }
    }
    reader.onerror = () => {
      toast(t('Không thể đọc file ảnh. Vui lòng thử lại.'), 'err')
    }
    reader.readAsDataURL(file)
    e.target.value = ''
  }

  const handleRemoveAvatar = () => {
    updateUser({ avatarUrl: null })
    toast(t('Đã gỡ ảnh đại diện, chuyển về ảnh chữ cái mặc định.'), 'ok')
  }

  const handleSaveProfile = async () => {
    if (!fullName.trim()) {
      throw new Error(t('Họ và tên không được để trống.'))
    }
    updateUser({
      fullName: fullName.trim(),
      phone: phone.trim(),
    })
    toast(t('Đã cập nhật thông tin tài khoản thành công!'), 'ok')
    setEditOpen(false)
  }

  const rows = [
    ['user', t('Họ và tên'), user.fullName],
    ['user', t('Tên đăng nhập'), user.username || '—'],
    ['mail', 'Email', user.email],
    ['phone', t('Số điện thoại'), user.phone || '—'],
    ['building', t('Phòng ban'), user.department || '—'],
    ['shield', t('Vai trò'), role],
  ]

  return (
    <div className="pf">
      <section className="pf-hero card">
        <div className="pf-av-wrap">
          <div className="pf-av" aria-hidden="true">
            {user.avatarUrl ? (
              <img src={user.avatarUrl} alt={user.fullName} />
            ) : (
              initials(user.fullName)
            )}
          </div>
          <button
            type="button"
            className="pf-av-btn"
            title={t('Tải ảnh đại diện')}
            aria-label={t('Tải ảnh đại diện')}
            onClick={() => fileInputRef.current?.click()}
          >
            <Icon name="camera" />
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/jpg,image/png,image/webp"
            style={{ display: 'none' }}
            onChange={handleAvatarChange}
          />
        </div>

        <div className="pf-id">
          <h1>{user.fullName}</h1>
          <p>{user.email}</p>
          <div className="pf-tags">
            <RoleBadge label={role} />
            <span className={`badge ${locked ? 'b-x' : 'b-Resolved'}`}>
              {locked ? t('Đã bị khóa') : t('Đang hoạt động')}
            </span>
          </div>
        </div>

        <div className="pf-hero-acts">
          <button type="button" className="btn" onClick={() => setEditOpen(true)}>
            <Icon name="edit" />
            {t('Chỉnh sửa thông tin')}
          </button>
          <button type="button" className="btn pri" onClick={() => navigate(passwordPath(user.role))}>
            <Icon name="lock" />
            {t('Đổi mật khẩu')}
          </button>
        </div>
      </section>

      {user.mustChangePassword && (
        <div className="banner" role="alert">
          <Icon name="alert" />
          <div className="grow">
            {t('Quản trị viên vừa cấp lại mật khẩu cho bạn. Hãy đặt mật khẩu mới để tiếp tục sử dụng hệ thống.')}
          </div>
          <button type="button" className="btn sm" onClick={() => navigate(passwordPath(user.role))}>
            {t('Đặt mật khẩu mới')}
          </button>
        </div>
      )}

      <div className="pf-cols">
        <section className="card">
          <div className="ch">
            <h2>{t('Thông tin tài khoản')}</h2>
          </div>
          <ul className="pf-rows">
            {rows.map(([icon, label, value]) => (
              <li key={label}>
                <span className="pf-ico"><Icon name={icon} /></span>
                <span className="pf-k">{label}</span>
                <span className="pf-v">{value}</span>
              </li>
            ))}
          </ul>
          <p className="pf-note">
            {t('Thông tin này do quản trị viên quản lý. Cần chỉnh sửa, hãy liên hệ quản trị viên.')}
          </p>
        </section>

        <section className="card">
          <div className="ch">
            <h2>{t('Phiên đăng nhập')}</h2>
          </div>
          <div className="cb pf-session">
            {session ? (
              <>
                <div className="pf-left">
                  <b>{leftText(session.left)}</b>
                  <span className="muted sm">{t('Hết hạn lúc {d}', { d: fmtDateTime(session.exp) })}</span>
                </div>
                <div
                  className="pf-bar"
                  role="progressbar"
                  aria-label={t('Thời gian còn lại của phiên')}
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-valuenow={Math.round(Math.min(1, session.left / SESSION_MS) * 100)}
                >
                  <i style={{ width: `${Math.min(1, session.left / SESSION_MS) * 100}%` }} />
                </div>
              </>
            ) : (
              <span className="muted">{t('Không đọc được thời hạn phiên.')}</span>
            )}
            <p className="muted sm" style={{ margin: 0 }}>
              {t('Phiên tự kết thúc khi hết hạn. Đóng tab cũng sẽ đăng xuất bạn.')}
            </p>
            <button type="button" className="btn" onClick={logout}>
              <Icon name="logout" />
              {t('Đăng xuất')}
            </button>
          </div>
        </section>
      </div>

      {editOpen && (
        <Modal
          title={t('Chỉnh sửa thông tin tài khoản')}
          okText={t('Lưu thay đổi')}
          cancelText={t('Hủy')}
          onOk={handleSaveProfile}
          onClose={() => setEditOpen(false)}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div style={{ display: 'flex', gap: 16, alignItems: 'center', padding: '12px 14px', background: 'var(--surface2)', borderRadius: 10, border: '1px solid var(--border)' }}>
              <div className="pf-av-wrap" style={{ width: 64, height: 64 }}>
                <div className="pf-av" style={{ width: 64, height: 64, fontSize: 20 }}>
                  {user.avatarUrl ? <img src={user.avatarUrl} alt="" /> : initials(fullName || user.fullName)}
                </div>
                <button
                  type="button"
                  className="pf-av-btn"
                  style={{ width: 26, height: 26 }}
                  title={t('Tải ảnh')}
                  onClick={() => fileInputRef.current?.click()}
                >
                  <Icon name="camera" />
                </button>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <b style={{ fontSize: 14 }}>{t('Ảnh đại diện')}</b>
                <span className="muted sm">{t('JPG, PNG, WebP (Tối đa 5 MB, khuyến nghị ≤ 2 MB)')}</span>
                {user.avatarUrl && (
                  <button
                    type="button"
                    className="btn ghost sm"
                    style={{ alignSelf: 'flex-start', padding: '2px 8px', height: 26, marginTop: 2 }}
                    onClick={handleRemoveAvatar}
                  >
                    <Icon name="x" />
                    {t('Gỡ ảnh')}
                  </button>
                )}
              </div>
            </div>

            <Field label={t('Họ và tên')} required htmlFor="pf-name">
              <input
                id="pf-name"
                className="in"
                value={fullName}
                maxLength={100}
                onChange={(e) => setFullName(e.target.value)}
                placeholder={t('Nhập họ và tên')}
              />
            </Field>

            <Field label={t('Số điện thoại')} htmlFor="pf-phone" hint={t('Số điện thoại liên hệ nội bộ')}>
              <input
                id="pf-phone"
                className="in"
                value={phone}
                maxLength={20}
                onChange={(e) => setPhone(e.target.value)}
                placeholder={t('Ví dụ: 0912 345 678')}
              />
            </Field>

            <Field label={t('Phòng ban')} hint={t('Phòng ban do quản trị viên quản lý (chỉ xem)')}>
              <input
                className="in"
                value={user.department || '—'}
                disabled
              />
            </Field>

            <Field label={t('Tên đăng nhập')} hint={t('Định danh tài khoản (chỉ xem)')}>
              <input className="in" value={user.username || '—'} disabled />
            </Field>

            <Field label="Email" hint={t('Email đăng nhập và nhận thông báo (chỉ xem)')}>
              <input className="in" value={user.email} disabled />
            </Field>

            <Field label={t('Vai trò')}>
              <input className="in" value={role} disabled />
            </Field>
          </div>
        </Modal>
      )}
    </div>
  )
}
