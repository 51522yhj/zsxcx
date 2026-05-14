const api = require('../../utils/api')
const { hydrateProducts } = require('../../utils/image-cache')

Page({
  data: {
    settings: {},
    categories: [],
    sideItems: [],
    expandedParentId: null,
    activeCategoryId: null,
    products: [],
    keyword: ''
  },

  async onShow() {
    const [settings, categories] = await Promise.all([api.getSettings(), api.getCategories()])
    const stored = wx.getStorageSync('selectedCategoryId')
    wx.removeStorageSync('selectedCategoryId')
    const selectedId = stored ? Number(stored) : null
    const expandedParentId = selectedId ? this.resolveParentId(categories, selectedId) : null
    this.setData({
      settings,
      categories,
      expandedParentId,
      activeCategoryId: selectedId,
      sideItems: this.buildSideItems(categories, expandedParentId)
    })
    this.loadProducts()
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
    this.loadProducts()
  },

  input(e) {
    this.setData({ keyword: e.detail.value })
  },

  search() {
    this.loadProducts()
  },

  async loadProducts() {
    const params = { page: 1, size: 80, keyword: this.data.keyword }
    if (this.data.activeCategoryId) params.categoryId = this.data.activeCategoryId
    const result = await api.getProducts(params)
    this.setData({ products: await this.normalizeProducts(result.records || []) })
  },

  async normalizeProducts(products) {
    return hydrateProducts(products)
  },

  goDetail(e) {
    wx.navigateTo({ url: `/pages/detail/detail?id=${e.currentTarget.dataset.id}` })
  },

  imageError(e) {
    console.warn('image load failed', e.currentTarget.dataset.src)
  }
})
