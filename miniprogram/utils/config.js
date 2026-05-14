module.exports = {
  // local: use local Spring Boot through wx.request.
  // cloud: use WeChat CloudBase Run through wx.cloud.callContainer.
  mode: 'cloud',
  localBaseUrl: 'http://192.168.2.3:8080',
  env: 'prod-5g2o11ud660eff91',
  service: 'xiaoyu-yinran-server'
}
