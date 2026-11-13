const app = getApp()

function request(path, options) {
  options = options || {}
  return new Promise((resolve, reject) => {
    const header = Object.assign({ 'Content-Type': 'application/json' }, options.header || {})
    if (app.globalData.memberToken) header.Authorization = 'Bearer ' + app.globalData.memberToken
    wx.request({
      url: app.globalData.baseUrl + path,
      method: options.method || 'GET',
      data: options.data || {},
      header: header,
      success: (res) => {
        const body = res.data || {}
        if (body.code === 0) {
          resolve(body.data)
        } else {
          if (body.code === 401) {
            app.globalData.memberToken = ''
            app.globalData.member = null
            wx.removeStorageSync('member_token')
            wx.removeStorageSync('member_profile')
          }
          wx.showToast({ title: body.msg || '请求失败', icon: 'none' })
          reject(new Error(body.msg || '请求失败'))
        }
      },
      fail: (err) => {
        wx.showToast({ title: '网络异常，请检查 API 地址', icon: 'none' })
        reject(err)
      }
    })
  })
}

function publicPath(path) {
  return '/public/' + app.globalData.tenantCode + path
}

function money(v) {
  return Number(v || 0).toLocaleString()
}

function today() {
  const d = new Date()
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
}

function addDays(date, days) {
  const d = new Date(date.replace(/-/g, '/'))
  d.setDate(d.getDate() + days)
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
}

function pad(n) {
  return n < 10 ? '0' + n : '' + n
}

function parseImages(raw) {
  if (!raw) return []
  if (Array.isArray(raw)) return raw.map(String).filter(Boolean)
  const s = String(raw).trim()
  if (!s) return []
  if (s.charAt(0) === '[') {
    try {
      const arr = JSON.parse(s)
      return Array.isArray(arr) ? arr.map(String).filter(Boolean) : []
    } catch (e) {}
  }
  return s.split(',').map(x => x.trim()).filter(Boolean)
}

const statusMap = {
  PENDING: '待确认', CONFIRMED: '已确认', CHECKED_IN: '在住',
  CHECKED_OUT: '已退房', CANCELLED: '已取消', NO_SHOW: '未到店'
}

module.exports = { request, publicPath, money, today, addDays, statusMap, parseImages }
