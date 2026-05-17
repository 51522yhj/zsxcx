const api = require('../../utils/api')
const { hydrateMedia } = require('../../utils/image-cache')

Page({
  data: {
    id: null,
    product: null,
    settings: {},
    displayMedia: [],
    detailImages: [],
    shareImageUrl: '',
    carouselAutoplay: true,
    carouselInterval: 3000,
    current: 1,
    currentIndex: 0
  },

  async onLoad(options) {
    const id = options.id
    this.setData({ id })
    wx.showLoading({ title: '加载中' })
    try {
      const [settings, product] = await Promise.all([api.getSettings(), api.getProduct(id)])
      const rawMedia = product.images && product.images.length ? product.images : [{ mediaType: 'IMAGE', imageUrl: product.coverUrl }]
      const displayMedia = await hydrateMedia(rawMedia)
      const detailImages = displayMedia
        .filter((item) => item.mediaType !== 'VIDEO' && item.showInDetail !== false)
        .sort((a, b) => (a.detailSortOrder || 0) - (b.detailSortOrder || 0))
      const firstImage = displayMedia.find((item) => item.mediaType !== 'VIDEO')
      const firstVideo = displayMedia.find((item) => item.mediaType === 'VIDEO')
      const shareImageUrl = (firstImage && firstImage.rawImageUrl) || (firstVideo && firstVideo.rawPosterUrl) || product.coverUrl || '/assets/ui/image-placeholder.png'
      const intervalSeconds = Number(product.carouselIntervalSeconds || settings.carouselIntervalSeconds || 3)
      this.setData({
        settings,
        product,
        displayMedia,
        detailImages,
        shareImageUrl,
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

  goHome() {
    wx.switchTab({ url: '/pages/index/index' })
  }
})
