const config = {
  // 本地联调时请改为开发机局域网 IP，并在开发者工具中关闭“校验合法域名”
  baseUrl: 'http://127.0.0.1:8080/api',
  tenantCode: 'YQ-HZ'
}

App({
  globalData: {
    baseUrl: config.baseUrl,
    tenantCode: config.tenantCode,
    home: null,
    memberToken: wx.getStorageSync('member_token') || '',
    member: wx.getStorageSync('member_profile') || null
  },
  onLaunch() {
    this.loadHome()
  },
  loadHome() {
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${this.globalData.baseUrl}/public/${this.globalData.tenantCode}/home`,
        success: (res) => {
          if (res.data && res.data.code === 0) {
            this.globalData.home = res.data.data
            resolve(res.data.data)
          } else {
            reject(new Error((res.data && res.data.msg) || '加载失败'))
          }
        },
        fail: reject
      })
    })
  }
})
