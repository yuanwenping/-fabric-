import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 45000,
})

http.interceptors.request.use((config) => {
  const isConsumerRequest = String(config.url || '').startsWith('/consumer')
  const token = localStorage.getItem(isConsumerRequest ? 'consumer_token' : 'trace_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const isConsumerRequest = String(error.config?.url || '').startsWith('/consumer')
    if (error.response?.status === 401) {
      if (isConsumerRequest) {
        localStorage.removeItem('consumer_token')
        localStorage.removeItem('consumer_user')
        if (location.pathname.startsWith('/user')) location.assign('/user/login')
      } else if (!location.pathname.startsWith('/login')) {
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
