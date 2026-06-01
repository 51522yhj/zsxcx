const api = require('../../utils/api')
const imageCache = require('../../utils/image-cache')

const mapProductsForDisplay = imageCache.mapProductsForDisplay
const PAGE_SIZE = 20
const HISTORY_KEY = 'search_history'

Page({
  data: {
    settings: {},
    keyword: '',
    categories: [],
    tags: [],
    sortOptions: [
      { value: 'hot', label: '热度' },
      { value: 'latest', label: '最新' },
      { value: 'oldest', label: '较早' }
    ],
    activeSort: 'hot',
    searchHistory: [],
    activeCategoryId: null,
    activeTagId: null,
    products: [],
    skeletonItems: [1, 2, 3, 4],
    searched: false,
    page: 1,
    loading: false,
    finished: false
  },

  async onLoad() {
    const result = await Promise.all([
      api.getSettings(),
      api.getCategories(),
      api.getTags()
    ])
    const settings = result[0]
    const categories = result[1]
    const tags = result[2]
    this.setData({ settings, categories, tags, searchHistory: this.getSearchHistory() })
    this._initialized = true
    await this.loadProducts(true)
  },

  onShow() {
    if (this._initialized) this.loadProducts(true)
  },

  onReachBottom() {
    if (this.data.searched) this.loadProducts()
  },

  async onPullDownRefresh() {
    try {
      const result = await Promise.all([
        api.getSettings(),
        api.getCategories(),
        api.getTags()
      ])
      const settings = result[0]
      const categories = result[1]
      const tags = result[2]
      this.setData({ settings, categories, tags, searchHistory: this.getSearchHistory() })
      if (this.data.searched) await this.loadProducts(true)
    } finally {
      wx.stopPullDownRefresh()
    }
  },

  input(e) {
    this.setData({ keyword: e.detail.value })
  },

  selectCategory(e) {
    const rawId = e.currentTarget.dataset.id
    const id = rawId === '' || rawId === undefined || rawId === null ? null : Number(rawId)
    this.setData({ activeCategoryId: this.data.activeCategoryId === id ? null : id })
    this.search()
  },

  selectTag(e) {
    const id = Number(e.currentTarget.dataset.id)
    this.setData({ activeTagId: this.data.activeTagId === id ? null : id })
    this.search()
  },

  selectSort(e) {
    const activeSort = e.currentTarget.dataset.value || 'hot'
    this.setData({ activeSort })
    if (this.data.searched) this.loadProducts(true)
  },

  selectHistory(e) {
    const keyword = e.currentTarget.dataset.keyword || ''
    this.setData({ keyword })
    this.search()
  },

  clearHistory() {
    wx.removeStorageSync(HISTORY_KEY)
    this.setData({ searchHistory: [] })
  },

  search() {
    const keyword = (this.data.keyword || '').trim()
    this.setData({ keyword })
    this.saveSearchHistory(keyword)
    this.loadProducts(true)
  },

  async loadProducts(reset = false) {
    if (this.data.loading || (!reset && this.data.finished)) return
    const page = reset ? 1 : this.data.page
    const loadingData = { loading: true, searched: true }
    if (reset) {
      loadingData.products = []
      loadingData.finished = false
    }
    this.setData(loadingData)
    const params = { page, size: PAGE_SIZE, keyword: this.data.keyword, sort: this.data.activeSort }
    if (this.data.activeCategoryId) params.categoryId = this.data.activeCategoryId
    if (this.data.activeTagId) params.tagId = this.data.activeTagId

    try {
      const result = await api.getProducts(params)
      const records = await this.normalizeProducts(result.records || [])
      this.setData({
        products: reset ? records : this.data.products.concat(records),
        page: page + 1,
        finished: records.length < PAGE_SIZE
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  async normalizeProducts(products) {
    return mapProductsForDisplay(products)
  },

  getSearchHistory() {
    const history = wx.getStorageSync(HISTORY_KEY)
    return Array.isArray(history) ? history.slice(0, 8) : []
  },

  saveSearchHistory(keyword) {
    if (!keyword) return
    const history = this.getSearchHistory().filter((item) => item !== keyword)
    history.unshift(keyword)
    const nextHistory = history.slice(0, 8)
    wx.setStorageSync(HISTORY_KEY, nextHistory)
    this.setData({ searchHistory: nextHistory })
  },

  goDetail(e) {
    wx.navigateTo({ url: `/pages/detail/detail?id=${e.currentTarget.dataset.id}` })
  },

  imageError(e) {
    console.warn('image load failed', e.currentTarget.dataset.src)
  }
})
