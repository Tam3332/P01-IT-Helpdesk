import dayjs from 'dayjs'
import { FILE_RULES } from '@/config/constants'

export const fmtDate = (d) => (d ? dayjs(d).format('DD/MM/YYYY HH:mm') : '—')

// Trả về chuỗi lỗi hoặc null nếu file hợp lệ
export function validateFile(file) {
  const ext = file.name.split('.').pop().toLowerCase()
  if (!FILE_RULES.extensions.includes(ext)) return `Định dạng .${ext} không được hỗ trợ`
  if (file.size > FILE_RULES.maxSizeMB * 1024 * 1024) return `File vượt quá ${FILE_RULES.maxSizeMB} MB`
  return null
}
