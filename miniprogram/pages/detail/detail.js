const api = require('../../utils/api')

Page({
  data: {
    id: null,
    product: null,
    settings: {},
    displayImages: [],
    current: 1
  },

  async onLoad(options) {
    const id = options.id
    this.setData({ id })
    wx.showLoading({ title: '加载中' })
    try {
      const [settings, product] = await Promise.all([api.getSettings(), api.getProduct(id)])
      const displayImages = product.images && product.images.length ? product.images : [{ imageUrl: product.coverUrl }]
      this.setData({ settings, product, displayImages })
      wx.setNavigationBarTitle({ title: product.name })
    } finally {
      wx.hideLoading()
    }
  },

  swiperChange(e) {
    this.setData({ current: e.detail.current + 1 })
  },

  goHome() {
    wx.switchTab({ url: '/pages/index/index' })
  }
})
