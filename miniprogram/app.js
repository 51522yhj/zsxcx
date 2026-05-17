const config = require('./utils/config')

App({
  globalData: {
    settings: null
  },
  onLaunch() {
    wx.cloud.init({
      env: config.env,
      traceUser: true
    })
  }
})
