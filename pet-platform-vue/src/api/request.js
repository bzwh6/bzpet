import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 401 统一处理：区分"未登录"和"登录已过期"，并避免并发请求重复弹窗
let last401At = 0
function handleUnauthorized() {
  const now = Date.now()
  if (now - last401At < 1500) {
    return
  }
  last401At = now
  const hadToken = !!localStorage.getItem('token')
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  ElMessage.error(hadToken ? '登录已过期，请重新登录' : '请先登录')
  const current = router.currentRoute.value
  if (current.path !== '/login') {
    router.push({ path: '/login', query: { redirect: current.fullPath } })
  }
}

// 统一走 /api 前缀，由 vite proxy 转发到网关
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截：带上 token（网关 LoginFilter 读取 Authorization header）
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

// 响应拦截：解包后端统一响应 R { code, msg, data }，处理 401
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一响应体：{ code, msg, data }
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 200) {
        return res.data
      }
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    // void 接口（无包装）直接返回
    return res
  },
  (error) => {
    const status = error.response && error.response.status
    if (status === 401) {
      handleUnauthorized()
    } else {
      ElMessage.error((error.response && error.response.data && error.response.data.msg) || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
