import axios from 'axios'
import { ElMessage } from 'element-plus'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('xy_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.success === false) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body?.data ?? body
  },
  (error) => {
    ElMessage.error(error.response?.data?.message || error.message || '网络异常')
    return Promise.reject(error)
  }
)

export function uploadImage(file) {
  const form = new FormData()
  form.append('file', file)
  return http.post('/api/admin/upload/image', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

