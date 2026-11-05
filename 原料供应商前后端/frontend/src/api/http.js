import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 45000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('trace_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      if (!location.pathname.startsWith('/login')) {
        localStorage.removeItem('trace_token')
        localStorage.removeItem('trace_user')
        location.assign('/login')
      }
    }
    const message = error.response?.data?.message || error.message || '请求失败'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default http
