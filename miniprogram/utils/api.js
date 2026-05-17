const config = require('./config')

const VISITOR_KEY = 'xy_visitor_id'

function visitorId() {
  let id = wx.getStorageSync(VISITOR_KEY)
  if (!id) {
    id = `${Date.now()}${Math.random().toString(16).slice(2)}`
    wx.setStorageSync(VISITOR_KEY, id)
  }
  return id
}

function currentSource() {
  const pages = getCurrentPages()
  const page = pages && pages.length ? pages[pages.length - 1] : null
  return page?.route || 'app'
}

function trackingHeaders() {
  return {
    'X-Visitor-Id': visitorId(),
    'X-Client-Source': currentSource(),
    'X-Client-Platform': 'miniprogram'
  }
}

function unwrap(res) {
  const body = res.data || {}
  if (body.success === false) {
    const message = body.message || 'Request failed'
    wx.showToast({ title: message, icon: 'none' })
    throw new Error(message)
  }
  return body.data
}

function request(path, data = {}, method = 'GET') {
  if (config.mode === 'local') {
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${baseUrl()}${path}`,
        method,
        data,
        header: {
          'content-type': 'application/json',
          ...trackingHeaders()
        },
        success: (res) => {
          try {
            resolve(unwrap(res))
          } catch (error) {
            reject(error)
          }
        },
        fail: (error) => {
          wx.showToast({ title: 'Backend offline', icon: 'none' })
          reject(error)
        }
      })
    })
  }

  return wx.cloud.callContainer({
    config: {
      env: config.env
    },
    path,
    method,
    data,
    header: {
      'X-WX-SERVICE': config.service,
      'content-type': 'application/json',
      ...trackingHeaders()
    }
  }).then(unwrap)
}

function baseUrl() {
  const url = String(config.localBaseUrl || '').trim()
  if (!url) return ''
  return /^https?:\/\//i.test(url) ? url.replace(/\/$/, '') : `http://${url.replace(/\/$/, '')}`
}

module.exports = {
  getSettings: () => request('/api/public/settings'),
  getAnnouncements: () => request('/api/public/announcements/active'),
  getCategories: () => request('/api/public/categories/tree'),
  getTags: () => request('/api/public/tags'),
  getProducts: (params) => request('/api/public/products', params),
  getProduct: (id) => request(`/api/public/products/${id}`),
  bindOpenid: (code) => request('/api/public/subscription/openid', { code }, 'POST'),
  updateNewProductSubscription: (openid, enabled) => request('/api/public/subscription/new-product', { openid, enabled }, 'POST'),
  getSubscriptionStatus: (openid) => request('/api/public/subscription/status', { openid })
}
