const api = require('../../utils/api')
const subscription = require('../../utils/subscription')

Page({
  data: {
    settings: {
      siteName: '源创潮牌',
      customerServiceEnabled: true,
      customerServiceText: '咨询客服',
      newProductNoticeEnabled: false,
      newProductTemplateId: ''
    },
    newProductEnabled: false,
    noticeOperating: false
  },

  async onLoad() {
    await this.loadSettings()
  },

  async onShow() {
    this.setData({ newProductEnabled: subscription.getLocalEnabled() })
  },

  async loadSettings() {
    const settings = await api.getSettings()
    this.setData({
      settings,
      newProductEnabled: subscription.getLocalEnabled()
    })
  },

  async enableNewProduct() {
    if (this.data.noticeOperating) return
    if (!this.data.settings.newProductNoticeEnabled || !this.data.settings.newProductTemplateId) {
      wx.showModal({
        title: '暂未配置',
        content: '后台还没有开启新品通知或填写订阅消息模板 ID。',
        showCancel: false
      })
      return
    }
    this.setData({ noticeOperating: true })
    try {
      const result = await subscription.setNewProductEnabled(this.data.settings, true)
      this.setData({ newProductEnabled: result })
    } catch (error) {
      console.warn('enable notice failed', error)
      this.setData({ newProductEnabled: false })
      wx.showToast({ title: '通知设置失败', icon: 'none' })
    } finally {
      setTimeout(() => this.setData({ noticeOperating: false }), 900)
    }
  },

  async renewNewProduct() {
    if (this.data.noticeOperating) return
    this.setData({ noticeOperating: true })
    try {
      const result = await subscription.setNewProductEnabled(this.data.settings, true)
      this.setData({ newProductEnabled: result })
    } catch (error) {
      console.warn('renew notice failed', error)
      wx.showToast({ title: '重新授权失败', icon: 'none' })
    } finally {
      setTimeout(() => this.setData({ noticeOperating: false }), 900)
    }
  },

  async disableNewProduct() {
    if (this.data.noticeOperating) return
    this.setData({ noticeOperating: true })
    try {
      wx.showLoading({ title: '设置中' })
      const result = await subscription.setNewProductEnabled(this.data.settings, false)
      this.setData({ newProductEnabled: result })
      wx.showToast({ title: '已关闭新品通知', icon: 'none' })
    } catch (error) {
      console.warn('disable notice failed', error)
      wx.showToast({ title: '通知设置失败', icon: 'none' })
    } finally {
      wx.hideLoading()
      setTimeout(() => this.setData({ noticeOperating: false }), 900)
    }
  },

  copyPhone() {
    if (!this.data.settings.contactPhone) return wx.showToast({ title: '暂未配置电话', icon: 'none' })
    wx.setClipboardData({ data: this.data.settings.contactPhone })
  },

  copyWechat() {
    if (!this.data.settings.contactWechat) return wx.showToast({ title: '暂未配置微信号', icon: 'none' })
    wx.setClipboardData({ data: this.data.settings.contactWechat })
  }
})
