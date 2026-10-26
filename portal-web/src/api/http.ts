import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 20000 })

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('portal_member_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && typeof res.code === 'number') {
      if (res.code === 0) return res
      if (res.code === 401) {
        localStorage.removeItem('portal_member_token')
        localStorage.removeItem('portal_member_profile')
      }
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  (error) => Promise.reject(new Error(error.response?.data?.msg || error.message || '网络异常'))
)

export default http
