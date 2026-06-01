const memory = {}
const VIDEO_PLACEHOLDER = '/assets/ui/image-placeholder.png'

function hash(input) {
  let value = 5381
  for (let i = 0; i < input.length; i += 1) {
    value = ((value << 5) + value + input.charCodeAt(i)) >>> 0
  }
  return value.toString(16)
}

function extFromUrl(url) {
  const clean = (url || '').split('?')[0].split('#')[0]
  const match = clean.match(/\.([a-zA-Z0-9]+)$/)
  const ext = match ? match[1].toLowerCase() : 'jpg'
  return ['jpg', 'jpeg', 'png', 'webp', 'gif'].includes(ext) ? ext : 'jpg'
}

function cacheImage(url) {
  if (!url) return Promise.resolve('')
  if (url.startsWith('wxfile://') || url.startsWith('data:') || url.startsWith('/')) return Promise.resolve(url)
  if (memory[url]) return Promise.resolve(memory[url])

  const fs = wx.getFileSystemManager()
  const filePath = `${wx.env.USER_DATA_PATH}/xy_img_${hash(url)}.${extFromUrl(url)}`

  return new Promise((resolve) => {
    fs.access({
      path: filePath,
      success: () => {
        memory[url] = filePath
        resolve(filePath)
      },
      fail: () => {
        wx.request({
          url,
          method: 'GET',
          responseType: 'arraybuffer',
          success: (res) => {
            if (res.statusCode < 200 || res.statusCode >= 300 || !res.data) {
              console.warn('image request failed', url, res.statusCode)
              resolve(url)
              return
            }
            fs.writeFile({
              filePath,
              data: res.data,
              success: () => {
                memory[url] = filePath
                resolve(filePath)
              },
              fail: (error) => {
                console.warn('image cache write failed', url, error)
                resolve(url)
              }
            })
          },
          fail: (error) => {
            console.warn('image request failed', url, error)
            resolve(url)
          }
        })
      }
    })
  })
}

function prepareShareImage(url) {
  const fallback = VIDEO_PLACEHOLDER
  if (!url || url.startsWith('/')) return Promise.resolve(url || fallback)

  return new Promise((resolve) => {
    wx.downloadFile({
      url,
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300 && res.tempFilePath) {
          resolve(res.tempFilePath)
          return
        }
        console.warn('share image download failed', url, res.statusCode)
        resolve(fallback)
      },
      fail: (error) => {
        console.warn('share image download failed', url, error)
        resolve(fallback)
      }
    })
  })
}

function getProductImageUrl(product) {
  if (product.coverUrl) return product.coverUrl
  const firstImage = (product.images || []).find((item) => item.mediaType !== 'VIDEO')
  const firstMedia = product.images && product.images[0]
  if (firstImage) return firstImage.imageUrl
  if (firstMedia && firstMedia.mediaType === 'VIDEO') return firstMedia.posterUrl || VIDEO_PLACEHOLDER
  return firstMedia ? firstMedia.imageUrl : ''
}

async function hydrateProducts(products) {
  return Promise.all((products || []).map(async (product) => {
    const rawImageUrl = getProductImageUrl(product)
    const displayImageUrl = await cacheImage(rawImageUrl)
    return Object.assign({}, product, { rawImageUrl, displayImageUrl })
  }))
}

function mapProductsForDisplay(products) {
  return (products || []).map((product) => {
    const rawImageUrl = getProductImageUrl(product)
    return Object.assign({}, product, { rawImageUrl, displayImageUrl: rawImageUrl })
  })
}

async function hydrateImages(images) {
  return Promise.all((images || []).map(async (image) => {
    const rawImageUrl = image.imageUrl || ''
    const displayImageUrl = await cacheImage(rawImageUrl)
    return Object.assign({}, image, { rawImageUrl, imageUrl: displayImageUrl })
  }))
}

async function hydrateMedia(mediaList) {
  return Promise.all((mediaList || []).map(async (media, index) => {
    const mediaType = media.mediaType === 'VIDEO' ? 'VIDEO' : 'IMAGE'
    if (mediaType === 'VIDEO') {
      const rawPosterUrl = media.posterUrl || ''
      const posterUrl = rawPosterUrl ? await cacheImage(rawPosterUrl) : VIDEO_PLACEHOLDER
      return Object.assign({}, media, {
        mediaType,
        rawImageUrl: media.imageUrl || '',
        rawPosterUrl: rawPosterUrl || VIDEO_PLACEHOLDER,
        posterUrl,
        videoId: `media-video-${index}`
      })
    }
    const rawImageUrl = media.imageUrl || ''
    const displayImageUrl = await cacheImage(rawImageUrl)
    return Object.assign({}, media, { mediaType, rawImageUrl, imageUrl: displayImageUrl })
  }))
}

function mapMediaForDisplay(mediaList) {
  return (mediaList || []).map((media, index) => {
    const mediaType = media.mediaType === 'VIDEO' ? 'VIDEO' : 'IMAGE'
    if (mediaType === 'VIDEO') {
      const rawPosterUrl = media.posterUrl || ''
      return Object.assign({}, media, {
        mediaType,
        rawImageUrl: media.imageUrl || '',
        rawPosterUrl: rawPosterUrl || VIDEO_PLACEHOLDER,
        posterUrl: rawPosterUrl || VIDEO_PLACEHOLDER,
        videoId: `media-video-${index}`
      })
    }
    const rawImageUrl = media.imageUrl || ''
    return Object.assign({}, media, { mediaType, rawImageUrl, imageUrl: rawImageUrl })
  })
}

module.exports = {
  cacheImage,
  prepareShareImage,
  hydrateProducts,
  mapProductsForDisplay,
  hydrateImages,
  hydrateMedia,
  mapMediaForDisplay
}
