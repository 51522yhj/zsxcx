const api = require('./api')

const OPENID_KEY = 'xy_openid'
const ENABLED_KEY = 'xy_new_product_enabled'
const PROMPTED_KEY = 'xy_new_product_prompted_v3'

let subscribing = false

function wxLogin() {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (res) => resolve(res.code || ''),
      fail: reject
    })
  })
}

async function ensureOpenid() {
  const cached = wx.getStorageSync(OPENID_KEY)
  if (cached) return cached
  const code = await wxLogin()
  const result = await api.bindOpenid(code)
  if (result && result.openid) {
    wx.setStorageSync(OPENID_KEY, result.openid)
    wx.setStorageSync(ENABLED_KEY, Boolean(result.newProductEnabled))
    return result.openid
  }
  return ''
}

function getSubscriptionSetting() {
  return new Promise((resolve) => {
    if (!wx.getSetting) {
      resolve({})
      return
    }
    wx.getSetting({
      withSubscriptions: true,
      success: (res) => resolve(res.subscriptionsSetting || {}),
      fail: () => resolve({})
    })
  })
}

function openMiniProgramSetting() {
  return new Promise((resolve) => {
    if (!wx.openSetting) {
      resolve()
      return
    }
    wx.openSetting({
      success: resolve,
      fail: resolve
    })
  })
}

async function ensureCanRequestSubscribe(templateId) {
  const setting = await getSubscriptionSetting()
  const itemSetting = setting.itemSettings || {}
  const templateStatus = itemSetting[templateId]

  if (setting.mainSwitch === false) {
    wx.showModal({
      title: '订阅消息已关闭',
      content: '请在小程序设置里打开“订阅消息”，然后再点击授权。',
      confirmText: '去设置',
      success: (res) => {
        if (res.confirm) openMiniProgramSetting()
      }
    })
    return false
  }

  if (templateStatus === 'reject') {
    wx.showModal({
      title: '已拒绝该通知',
      content: '你之前拒绝过新品通知。请在小程序设置里重新允许订阅消息，再点击授权。',
      confirmText: '去设置',
      success: (res) => {
        if (res.confirm) openMiniProgramSetting()
      }
    })
    return false
  }

  return true
}

function requestSubscribe(templateId) {
  return new Promise((resolve) => {
    if (subscribing) {
      resolve({ accepted: false, message: '订阅授权正在处理中，请稍后再试' })
      return
    }
    if (!templateId || !wx.requestSubscribeMessage) {
      resolve({ accepted: false, message: '当前微信版本不支持订阅消息' })
      return
    }

    subscribing = true
    wx.requestSubscribeMessage({
      tmplIds: [templateId],
      success: (res) => {
        const value = res[templateId]
        resolve({
          accepted: value === 'accept',
          message: value === 'accept' ? '' : `订阅结果：${value || '未知'}`
        })
      },
      fail: (error) => {
        resolve({
          accepted: false,
          message: (error && error.errMsg) || '订阅授权未弹出'
        })
      },
      complete: () => {
        setTimeout(() => {
          subscribing = false
        }, 800)
      }
    })
  })
}

function isNoticeConfigured(settings) {
  return Boolean(settings && settings.newProductNoticeEnabled && settings.newProductTemplateId)
}

function shouldPrompt(settings) {
  return isNoticeConfigured(settings)
    && !wx.getStorageSync(PROMPTED_KEY)
    && !wx.getStorageSync(ENABLED_KEY)
}

function markPromptSkipped() {
  wx.setStorageSync(PROMPTED_KEY, true)
}

async function saveSubscriptionEnabled(enabled) {
  const openid = await ensureOpenid()
  if (!openid) return false
  await api.updateNewProductSubscription(openid, enabled)
  wx.setStorageSync(ENABLED_KEY, enabled)
  wx.setStorageSync(PROMPTED_KEY, true)
  return enabled
}

async function requestAndSave(settings) {
  const templateId = settings.newProductTemplateId
  const canRequest = await ensureCanRequestSubscribe(templateId)
  if (!canRequest) {
    wx.setStorageSync(ENABLED_KEY, false)
    return false
  }

  const result = await requestSubscribe(templateId)
  if (!result.accepted) {
    wx.setStorageSync(PROMPTED_KEY, true)
    wx.setStorageSync(ENABLED_KEY, false)
    wx.showModal({
      title: '未开启通知',
      content: result.message || '微信没有返回允许订阅，请检查真机小程序设置。',
      showCancel: false
    })
    return false
  }

  wx.showToast({ title: '已开启新品上架提醒', icon: 'none' })
  return saveSubscriptionEnabled(true)
}

async function askNewProductSubscribe(settings, force = false) {
  if (!isNoticeConfigured(settings)) {
    wx.showToast({ title: '新品通知暂未配置', icon: 'none' })
    return false
  }
  if (!force && wx.getStorageSync(PROMPTED_KEY)) {
    return Boolean(wx.getStorageSync(ENABLED_KEY))
  }
  wx.setStorageSync(PROMPTED_KEY, true)
  return requestAndSave(settings)
}

async function setNewProductEnabled(settings, enabled) {
  if (!enabled) {
    return saveSubscriptionEnabled(false)
  }
  if (!isNoticeConfigured(settings)) {
    wx.showToast({ title: '新品通知暂未配置', icon: 'none' })
    return false
  }
  return requestAndSave(settings)
}

function getLocalEnabled() {
  return Boolean(wx.getStorageSync(ENABLED_KEY))
}

module.exports = {
  ensureOpenid,
  askNewProductSubscribe,
  setNewProductEnabled,
  getLocalEnabled,
  shouldPrompt,
  markPromptSkipped
}
