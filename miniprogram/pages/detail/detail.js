const api = require('../../utils/api')
const imageCache = require('../../utils/image-cache')

const mapMediaForDisplay = imageCache.mapMediaForDisplay
const prepareShareImage = imageCache.prepareShareImage

Page({
  data: {
    id: null,
    product: null,
    settings: {},
    displayMedia: [],
    detailImages: [],
    shareImageUrl: '',
    messageImageUrl: '',
    carouselAutoplay: true,
    carouselInterval: 3000,
    current: 1,
    currentIndex: 0
  },

  async onLoad(options) {
    const id = options.id
    this.setData({ id })
    wx.showShareMenu({
      menus: ['shareAppMessage', 'shareTimeline']
    })
    wx.showLoading({ title: '加载中' })
    try {
      const result = await Promise.all([api.getSettings(), api.getProduct(id)])
      const settings = result[0]
      const product = result[1]
      const rawMedia = product.images && product.images.length ? product.images : [{ mediaType: 'IMAGE', imageUrl: product.coverUrl }]
      const displayMedia = mapMediaForDisplay(rawMedia)
      const detailImages = displayMedia
        .filter((item) => item.mediaType !== 'VIDEO' && item.showInDetail !== false)
        .sort((a, b) => (a.detailSortOrder || 0) - (b.detailSortOrder || 0))
      const firstImage = displayMedia.find((item) => item.mediaType !== 'VIDEO')
      const firstVideo = displayMedia.find((item) => item.mediaType === 'VIDEO')
      const messageImageUrl = (firstImage && firstImage.rawImageUrl) || (firstVideo && firstVideo.rawPosterUrl) || product.coverUrl || '/assets/ui/image-placeholder.png'
      const shareImageUrl = await prepareShareImage(messageImageUrl)
      const intervalSeconds = Number(product.carouselIntervalSeconds || settings.carouselIntervalSeconds || 3)
      this.setData({
        settings,
        product,
        displayMedia,
        detailImages,
        shareImageUrl,
        messageImageUrl,
        carouselAutoplay: product.carouselAutoplayEnabled !== false,
        carouselInterval: Math.min(Math.max(intervalSeconds, 1), 20) * 1000
      })
      wx.setNavigationBarTitle({ title: product.name })
      this.playCurrentVideo()
    } finally {
      wx.hideLoading()
    }
  },

  swiperChange(e) {
    const oldIndex = this.data.currentIndex
    const currentIndex = e.detail.current
    this.pauseVideo(oldIndex)
    this.setData({ current: currentIndex + 1, currentIndex }, () => this.playCurrentVideo())
  },

  playCurrentVideo() {
    const media = this.data.displayMedia[this.data.currentIndex]
    if (media && media.mediaType === 'VIDEO') {
      wx.createVideoContext(media.videoId, this).play()
    }
  },

  pauseVideo(index) {
    const media = this.data.displayMedia[index]
    if (media && media.mediaType === 'VIDEO') {
      wx.createVideoContext(media.videoId, this).pause()
    }
  },

  pauseAllVideos() {
    this.data.displayMedia.forEach((media) => {
      if (media.mediaType === 'VIDEO') {
        wx.createVideoContext(media.videoId, this).pause()
      }
    })
  },

  onHide() {
    this.pauseAllVideos()
  },

  onUnload() {
    this.pauseAllVideos()
  },

  onShareAppMessage() {
    const product = this.data.product || {}
    return {
      title: product.name ? `${product.name} - ${this.data.settings.siteName || '源创潮牌'}` : '商品详情',
      path: `/pages/detail/detail?id=${product.id || this.data.id}`,
      imageUrl: this.data.shareImageUrl
    }
  },

  onShareTimeline() {
    const product = this.data.product || {}
    return {
      title: product.name ? `${product.name} - ${this.data.settings.siteName || '源创潮牌'}` : '商品详情',
      query: `id=${product.id || this.data.id}`,
      imageUrl: this.data.shareImageUrl
    }
  },

  previewGalleryImage(e) {
    const current = e.currentTarget.dataset.url
    this.previewImages(current, this.data.displayMedia
      .filter((item) => item.mediaType !== 'VIDEO')
      .map((item) => item.rawImageUrl || item.imageUrl)
      .filter(Boolean))
  },

  previewDetailImage(e) {
    const current = e.currentTarget.dataset.url
    this.previewImages(current, this.data.detailImages
      .map((item) => item.rawImageUrl || item.imageUrl)
      .filter(Boolean))
  },

  previewImages(current, urls) {
    if (!current || !urls.length) return
    this.pauseAllVideos()
    wx.previewImage({ current, urls })
  },

  goHome() {
    wx.switchTab({ url: '/pages/index/index' })
  }
})
