// Đọc thời điểm hết hạn (claim "exp", tính bằng giây) từ JWT mà không cần thư viện.
// Chỉ dùng để tự đăng xuất đúng lúc; việc xác thực chữ ký là của Backend.
export function getTokenExpiryMs(token) {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const json = JSON.parse(decodeURIComponent(escape(atob(payload))))
    return typeof json.exp === 'number' ? json.exp * 1000 : null
  } catch {
    return null
  }
}
