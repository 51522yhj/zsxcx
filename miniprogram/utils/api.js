const config = require('./config')

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
        url: `${config.localBaseUrl}${path}`,
        method,
        data,
        header: {
          'content-type': 'application/json'
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
      'content-type': 'application/json'
    }
  }).then(unwrap)
}

module.exports = {
  getSettings: () => request('/api/public/settings'),
  getAnnouncements: () => request('/api/public/announcements/active'),
  getCategories: () => request('/api/public/categories/tree'),
  getTags: () => request('/api/public/tags'),
  getProducts: (params) => request('/api/public/products', params),
  getProduct: (id) => request(`/api/public/products/${id}`)
}

