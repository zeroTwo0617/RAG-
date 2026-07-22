import axios from 'axios'
import { ElMessage } from 'element-plus'

// 全局 Axios 实例：所有 api/* 模块都复用它
const request = axios.create({
  // 后端地址；可用 .env 的 VITE_API_BASE 覆盖（默认指向本地 :8080/api）
  baseURL: import.meta.env.VITE_API_BASE || 'http://localhost:8080/api',
  timeout: 30000
})

// 请求拦截器：自动把登录拿到的 JWT 塞进 Authorization 头（Bearer <token>）
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一拆包 + 错误提示
request.interceptors.response.use(
  (resp) => {
    const data = resp.data
    // 后端约定 code !== 0 即业务错误，直接弹消息并 reject
    if (data && data.code !== 0) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    // 成功时直接返回 Result 包络，调用方取 .data 即可
    return data
  },
  (error) => {
    const msg = error.response?.data?.message || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
