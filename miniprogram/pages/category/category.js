const api = require('../../utils/api')
const imageCache = require('../../utils/image-cache')

const mapProductsForDisplay = imageCache.mapProductsForDisplay
const PAGE_SIZE = 20

Page({
  data: {
    settings: {},
    categories: [],
    sideItems: [],
    expandedParentId: null,
    activeCategoryId: null,
    products: [],
    skeletonItems: [1, 2, 3, 4, 5],
    keyword: '',
    page: 1,
    loading: false,
    finished: false
  },

  async onShow() {
    await this.loadCategory()
  },

  async onPullDownRefresh() {
    try {
      await this.loadCategory(true)
    } finally {
      wx.stopPullDownRefresh()
    }
  },

  async loadCategory(refresh = false) {
    const result = await Promise.all([api.getSettings(), api.getCategories()])
    const settings = result[0]
    const categories = result[1]
    const stored = refresh ? '' : wx.getStorageSync('selectedCategoryId')
    if (!refresh) wx.removeStorageSync('selectedCategoryId')
    const selectedId = stored ? Number(stored) : (refresh ? this.data.activeCategoryId : null)
    const expandedParentId = selectedId ? this.resolveParentId(categories, selectedId) : null
    this.setData({
      settings,
      categories,
      expandedParentId,
      activeCategoryId: selectedId,
      sideItems: this.buildSideItems(categories, expandedParentId)
    })
    await this.loadProducts(true)
  },

  resolveParentId(categories, selectedId) {
    for (const category of categories) {
      if (category.id === selectedId) return category.id
      if ((category.children || []).some((child) => child.id === selectedId)) return category.id
    }
    return selectedId
  },

  buildSideItems(categories, expandedParentId) {
    const items = [{ id: null, name: '全部', level: 0, type: 'all' }]
    categories.forEach((category) => {
      items.push({ id: category.id, name: category.name, level: 0, type: 'parent' })
      if (expandedParentId === category.id) {
        ;(category.children || []).forEach((child) => {
          items.push({ id: child.id, name: child.name, level: 1, type: 'child' })
        })
      }
    })
    return items
  },

  selectCategory(e) {
    const rawId = e.currentTarget.dataset.id
    const id = rawId === '' || rawId === undefined || rawId === null ? null : Number(rawId)
    const type = e.currentTarget.dataset.type
    let expandedParentId = this.data.expandedParentId

    if (type === 'all') {
      expandedParentId = null
    } else if (type === 'parent') {
      expandedParentId = expandedParentId === id ? null : id
    }

    this.setData({
      expandedParentId,
      activeCategoryId: id,
      sideItems: this.buildSideItems(this.data.categories, expandedParentId)
    })
    this.loadProducts(true)
  },

  input(e) {
    this.setData({ keyword: e.detail.value })
  },

  search() {
    this.loadProducts(true)
  },

  loadMoreProducts() {
    this.loadProducts()
  },

  async loadProducts(reset = false) {
    if (this.data.loading || (!reset && this.data.finished)) return
    const page = reset ? 1 : this.data.page
    const loadingData = { loading: true }
    if (reset) {
      loadingData.products = []
      loadingData.finished = false
    }
    this.setData(loadingData)
    const params = { page, size: PAGE_SIZE, keyword: this.data.keyword }
    if (this.data.activeCategoryId) params.categoryId = this.data.activeCategoryId
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

  goSearch() {
    wx.switchTab({ url: '/pages/search/search' })
  },

  goDetail(e) {
    wx.navigateTo({ url: `/pages/detail/detail?id=${e.currentTarget.dataset.id}` })
  },

  imageError(e) {
    console.warn('image load failed', e.currentTarget.dataset.src)
  }
})
