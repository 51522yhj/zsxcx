const api = require('../../utils/api')

Page({
  data: {
    settings: { siteName: '小于印染', homeSectionTitle: '精选面料' },
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
    imageErrors: {}
  },

  onLoad() {
    this.loadHome()
  },

  onReachBottom() {
    this.loadProducts()
  },

  async loadHome() {
    wx.showLoading({ title: '加载中' })
    try {
      const [settings, announcements, categories] = await Promise.all([
        api.getSettings(),
        api.getAnnouncements(),
        api.getCategories()
      ])
      const noticeText = announcements.map((item) => item.tickerText).filter(Boolean).join('   |   ')
      this.setData({ settings, announcements, categories, noticeText })
      wx.setNavigationBarTitle({ title: settings.siteName || '小于印染' })
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
      const result = await api.getProducts({ page, size: 30 })
      const records = result.records || []
      const products = reset ? records : this.data.products.concat(records)
      this.setData({
        products,
        sections: this.buildSections(products),
        page: page + 1,
        finished: records.length < 30
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

  openNotice() {
    const notice = this.data.announcements[0]
    if (notice) this.setData({ activeNotice: notice, showNotice: true })
  },

  closeNotice() {
    this.setData({ showNotice: false })
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
    const id = e.currentTarget.dataset.id
    if (id) this.setData({ [`imageErrors.${id}`]: true })
  }
})
