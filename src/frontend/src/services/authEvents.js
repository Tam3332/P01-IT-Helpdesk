// Cầu nối giữa apiClient (ngoài React) và React: khi API trả 401/403 thì báo cho App xử lý.
const listeners = { unauthorized: new Set(), forbidden: new Set() }

export const onUnauthorized = (fn) => (listeners.unauthorized.add(fn), () => listeners.unauthorized.delete(fn))
export const onForbidden = (fn) => (listeners.forbidden.add(fn), () => listeners.forbidden.delete(fn))
export const emitUnauthorized = () => listeners.unauthorized.forEach((fn) => fn())
export const emitForbidden = () => listeners.forbidden.forEach((fn) => fn())
