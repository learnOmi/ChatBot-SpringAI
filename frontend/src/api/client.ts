/**
 * API 客户端基础封装
 *
 * 提供 JSON 和 text/plain 两种响应类型支持。
 * 开发时通过 Vite proxy 代理到后端，生产环境需要绝对 URL。
 */

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

/**
 * 构建查询字符串
 * 接受任意键值对对象，自动过滤空值并编码。
 */
function buildQuery(base: string, params: object): string {
  const entries = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== '' && v !== null)
    .map(([k, v]) => [k, String(v)] as const)
  if (entries.length === 0) return base
  return `${base}?${entries.map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`).join('&')}`
}

/**
 * JSON 响应请求
 */
export async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  })
  if (!response.ok) {
    const text = await response.text()
    throw new Error(`API error ${response.status}: ${text}`)
  }
  return response.json() as Promise<T>
}

/**
 * text/plain 响应请求
 */
export async function requestText(url: string, options: RequestInit = {}): Promise<string> {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'text/plain', ...options.headers },
    ...options,
  })
  if (!response.ok) {
    const text = await response.text()
    throw new Error(`API error ${response.status}: ${text}`)
  }
  return response.text()
}

export { API_BASE, buildQuery }
