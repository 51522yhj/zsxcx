const api = require('../../utils/api')

Page({
  data: {
    settings: {
      siteName: '小于印染',
      customerServiceEnabled: true,
      customerServiceText: '咨询客服'
    }
  },

  async onLoad() {
    this.setData({ settings: await api.getSettings() })
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
