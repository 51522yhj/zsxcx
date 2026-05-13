const api = require('../../utils/api')

Page({
  data: {
    settings: {},
    keyword: '',
    categories: [],
    tags: [],
    activeCategoryId: null,
    activeTagId: null,
    products: [],
    searched: false,
    imageErrors: {}
  },

  async onLoad() {
    const [settings, categories, tags] = await Promise.all([
      api.getSettings(),
      api.getCategories(),
      api.getTags()
    ])
    this.setData({ settings, categories, tags })
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

  async search() {
    const params = {
      page: 1,
      size: 60,
      keyword: this.data.keyword
    }
    if (this.data.activeCategoryId) params.categoryId = this.data.activeCategoryId
    if (this.data.activeTagId) params.tagId = this.data.activeTagId

    const result = await api.getProducts(params)
    this.setData({ products: result.records || [], searched: true })
  },

  goDetail(e) {
    wx.navigateTo({ url: `/pages/detail/detail?id=${e.currentTarget.dataset.id}` })
  },

  imageError(e) {
    const id = e.currentTarget.dataset.id
    if (id) this.setData({ [`imageErrors.${id}`]: true })
  }
})
