import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

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
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
    } else {
      ElMessage.error((error.response && error.response.data && error.response.data.msg) || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
