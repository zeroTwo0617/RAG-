import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || 'http://localhost:8080/api',
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (resp) => {
    const data = resp.data
    if (data && data.code !== 0) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    // 直接返回 Result 包络，调用方取 .data
    return data
  },
  (error) => {
    const msg = error.response?.data?.message || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
