const api = require('../../utils/api')
const { mapProductsForDisplay } = require('../../utils/image-cache')
const subscription = require('../../utils/subscription')

const PAGE_SIZE = 20

Page({
  data: {
    settings: { siteName: '源创潮牌', homeSectionTitle: '精选面料' },
    announcements: [],
    noticeText: '',
    categories: [],
    sections: [],
    products: [],
    page: 1,
    loading: false,
    finished: false,
    showNotice: false,
    activeNotice: null,
    showSubscribePrompt: false
  },

  onLoad() {
    this.loadHome()
  },

  onReachBottom() {
    this.loadProducts()
  },

  async loadHome() {
    if (this._homeLoadingPromise) return this._homeLoadingPromise
    this._homeLoadingPromise = this.doLoadHome()
    try {
      return await this._homeLoadingPromise
    } finally {
      this._homeLoadingPromise = null
    }
  },

  async doLoadHome() {
    wx.showLoading({ title: '加载中', mask: true })
    try {
      const [settings, announcements, categories] = await Promise.all([
        api.getSettings(),
        api.getAnnouncements(),
        api.getCategories()
      ])
      const noticeText = announcements.map((item) => item.tickerText).filter(Boolean).join('   |   ')
      this.setData({ settings, announcements, categories, noticeText, showSubscribePrompt: subscription.shouldPrompt(settings) })
      wx.setNavigationBarTitle({ title: settings.siteName || '源创潮牌' })
      await this.loadProducts(true)
    } finally {
      wx.hideLoading()
    }
  },

  async loadProducts(reset = false) {
    if (this.data.loading || (!reset && this.data.finished)) return
    const page = reset ? 1 : this.data.page
    this.setData({ loading: true, ...(reset ? { products: [], sections: [], finished: false } : {}) })
    try {
      const result = await api.getProducts({ page, size: PAGE_SIZE })
      const records = await this.normalizeProducts(result.records || [])
      const products = reset ? records : this.data.products.concat(records)
      this.setData({
        products,
        sections: this.buildSections(products),
        page: page + 1,
        finished: records.length < PAGE_SIZE
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  buildSections(products) {
    const map = {}
    products.forEach((item) => {
      const key = item.categoryName || '精选面料'
      if (!map[key]) map[key] = []
      map[key].push(item)
    })
    return Object.keys(map).map((title) => ({ title, products: map[title].slice(0, 6) }))
  },

  async normalizeProducts(products) {
    return mapProductsForDisplay(products)
  },

  openNotice() {
    const notice = this.data.announcements[0]
    if (notice) this.setData({ activeNotice: notice, showNotice: true })
  },

  closeNotice() {
    this.setData({ showNotice: false })
  },

  async enableNewProductNotice() {
    try {
      const accepted = await subscription.askNewProductSubscribe(this.data.settings, true)
      this.setData({ showSubscribePrompt: false })
      if (!accepted) {
        wx.showModal({
          title: '未开启通知',
          content: '你可以稍后在“我的”页面重新开启新品上架通知。',
          showCancel: false
        })
      }
    } catch (error) {
      console.warn('subscribe failed', error)
      wx.showToast({ title: '通知开启失败', icon: 'none' })
    }
  },

  skipNewProductNotice() {
    subscription.markPromptSkipped()
    this.setData({ showSubscribePrompt: false })
  },

  noop() {},

  goSearch() {
    wx.switchTab({ url: '/pages/search/search' })
  },

  goCategory(e) {
    wx.setStorageSync('selectedCategoryId', e.currentTarget.dataset.id || '')
    wx.switchTab({ url: '/pages/category/category' })
  },

  goDetail(e) {
    wx.navigateTo({ url: `/pages/detail/detail?id=${e.currentTarget.dataset.id}` })
  },

  imageError(e) {
    console.warn('image load failed', e.currentTarget.dataset.src)
  }
})
