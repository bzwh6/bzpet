// 从 JWT 中解析 userId（后端把 userId 放在 claims 里）
export function parseUserId(token) {
  if (!token) return null
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const payload = JSON.parse(decodeURIComponent(escape(atob(base64))))
    return (payload.claims && payload.claims.userId) || null
  } catch (e) {
    return null
  }
}
