import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 20000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('hotel_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  const profile = JSON.parse(localStorage.getItem('hotel_profile') || 'null')
  if (profile?.platform) {
    const drill = Number(localStorage.getItem('hotel_drill_tenant') || 0)
    if (drill) config.headers['X-Tenant-Id'] = String(drill)
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && typeof res.code === 'number') {
      if (res.code === 0) return res
      if (res.code === 401) {
        localStorage.removeItem('hotel_token')
        window.location.href = '/admin/login'
      }
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('hotel_token')
      window.location.href = '/admin/login'
      ElMessage.error('登录已失效，请重新登录')
    } else {
      ElMessage.error(error.response?.data?.msg || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default http
