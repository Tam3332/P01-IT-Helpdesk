// Màu avatar ổn định theo tên: cùng một người luôn ra cùng một màu (không lưu gì, chỉ băm tên).
export { initials } from './format'

export function avatarStyle(name = '') {
  let h = 0
  for (const ch of String(name)) h = (h * 31 + ch.charCodeAt(0)) % 360
  return { background: `hsl(${h} 60% 90%)`, color: `hsl(${h} 55% 28%)` }
}
