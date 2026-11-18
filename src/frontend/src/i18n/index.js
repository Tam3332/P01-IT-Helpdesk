// Chuyển đổi Tiếng Việt / English. Khoá là chính câu tiếng Việt: t('Tổng quan').
// Tiếng Việt trả nguyên văn; English tra trong i18n/en/*.js, thiếu thì giữ tiếng Việt (và báo trong console khi chạy dev).
// Tham số: t('Còn {n} ngày', { n: 3 }).
const modules = import.meta.glob('./en/*.js', { eager: true })
const EN = Object.assign({}, ...Object.values(modules).map((m) => m.default))

const KEY = 'hd_lang'
const read = () => {
  try {
    const v = localStorage.getItem(KEY)
    if (v === 'vi' || v === 'en') return v
  } catch {
    /* không đọc được thì dùng mặc định */
  }
  return 'vi'
}

let lang = read()
if (typeof document !== 'undefined') document.documentElement.lang = lang
const warned = new Set()

export const getLang = () => lang
export function setLang(next) {
  lang = next === 'en' ? 'en' : 'vi'
  try {
    localStorage.setItem(KEY, lang)
  } catch {
    /* chỉ áp dụng cho lần xem này */
  }
  document.documentElement.lang = lang
}

// Khoá có thể kèm ngữ cảnh sau dấu '|' ('Mở lại|nút'): tiếng Việt cắt phần ngữ cảnh, tiếng Anh tra theo khoá đầy đủ.
const bare = (k) => {
  const i = k.indexOf('|')
  return i < 0 ? k : k.slice(0, i)
}

export function t(vi, params) {
  let s = bare(vi)
  if (lang === 'en') {
    const en = EN[vi]
    if (en === undefined) {
      if (import.meta.env.DEV && !warned.has(vi)) {
        warned.add(vi)
        console.debug('[i18n] thiếu bản English:', vi)
      }
    } else s = en
  }
  if (params) s = s.replace(/\{(\w+)\}/g, (m, k) => (params[k] ?? m))
  return s
}
