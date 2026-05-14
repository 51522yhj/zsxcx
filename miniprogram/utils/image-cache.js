const memory = {}

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

function getProductImageUrl(product) {
  return product.coverUrl || (product.images && product.images[0] ? product.images[0].imageUrl : '')
}

async function hydrateProducts(products) {
  return Promise.all((products || []).map(async (product) => {
    const rawImageUrl = getProductImageUrl(product)
    const displayImageUrl = await cacheImage(rawImageUrl)
    return { ...product, rawImageUrl, displayImageUrl }
  }))
}

async function hydrateImages(images) {
  return Promise.all((images || []).map(async (image) => {
    const rawImageUrl = image.imageUrl || ''
    const displayImageUrl = await cacheImage(rawImageUrl)
    return { ...image, rawImageUrl, imageUrl: displayImageUrl }
  }))
}

module.exports = {
  cacheImage,
  hydrateProducts,
  hydrateImages
}
