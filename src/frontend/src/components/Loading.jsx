import { t } from '@/i18n'

export default function Loading({ text = t('Đang tải...') }) {
  return (
    <div className="empty" role="status">
      <span className="muted">{text}</span>
    </div>
  )
}
