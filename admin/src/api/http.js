import axios from 'axios'
import { ElMessage } from 'element-plus'

const MAX_UPLOAD_SIZE = 100 * 1024 * 1024
const VISITOR_KEY = 'xy_admin_visitor_id'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 60000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('xy_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers['X-Visitor-Id'] = adminVisitorId()
  config.headers['X-Client-Platform'] = 'admin'
  config.headers['X-Client-Source'] = adminSource()
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
    const isTimeout = error.code === 'ECONNABORTED' || String(error.message || '').includes('timeout')
    ElMessage.error(isTimeout ? '请求超时，请检查网络后重试' : (error.response?.data?.message || error.message || '网络异常'))
    return Promise.reject(error)
  }
)

export function uploadImage(file) {
  return directUpload(file, 'IMAGE')
}

export function uploadVideo(file) {
  return directUpload(file, 'VIDEO')
}

function adminVisitorId() {
  let visitorId = localStorage.getItem(VISITOR_KEY)
  if (!visitorId) {
    visitorId = `admin_${Date.now()}_${Math.random().toString(16).slice(2)}`
    localStorage.setItem(VISITOR_KEY, visitorId)
  }
  return visitorId
}

function adminSource() {
  return localStorage.getItem('xy_admin_source') || 'admin'
}

async function directUpload(file, mediaType) {
  if (file.size > MAX_UPLOAD_SIZE) {
    ElMessage.error('文件最大不得超过100MB')
    throw new Error('文件最大不得超过100MB')
  }
  const contentType = file.type || 'application/octet-stream'
  const signed = await http.post('/api/admin/upload/direct-url', {
    mediaType,
    filename: file.name,
    contentType,
    size: file.size
  })
  try {
    await axios.put(signed.uploadUrl, file, {
      headers: { 'Content-Type': contentType },
      timeout: mediaType === 'VIDEO' ? 120000 : 60000
    })
  } catch (error) {
    const isTimeout = error.code === 'ECONNABORTED' || String(error.message || '').includes('timeout')
    ElMessage.error(isTimeout ? '上传超时，请检查网络后重试' : '上传失败，请稍后重试')
    throw error
  }
  return {
    url: signed.url,
    objectKey: signed.objectKey
  }
}
