module.exports = {
  // local: use local Spring Boot through wx.request.
  // cloud: use WeChat CloudBase Run through wx.cloud.callContainer.
  mode: 'local',
  // DevTools on this computer: http://127.0.0.1:8080
  // Real device on the same LAN: replace with your computer LAN IP, for example http://192.168.2.3:8080
  // Remote server: for example http://119.45.151.87
  localBaseUrl: 'https://www.fuzhuanghoutencentonline.icu/',
  env: 'prod-5g2o11ud660eff91',
  service: 'xiaoyu-yinran-server'
}
