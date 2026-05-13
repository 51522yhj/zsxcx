App({
  globalData: {
    settings: null
  },
  onLaunch() {
    wx.cloud.init({
      env: '',
      traceUser: true
    })
  }
})

