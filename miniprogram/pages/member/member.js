const app = getApp()
const util = require('../../utils/util')

Page({
  data: {
    member: null,
    level: {},
    avatarText: '微',
    couponCount: 0,
    orders: [],
    points: [],
    coupons: [],
    nickname: '演示会员',
    phone: '13900001004',
    logging: false
  },
  onShow() {
    this.setData({ member: app.globalData.member })
    if (app.globalData.member) {
      this.loadProfile()
      this.loadOrders()
      this.loadPoints()
      this.loadCoupons()
    }
  },
  onNickname(e) {
    this.setData({ nickname: e.detail.value })
  },
  onPhone(e) {
    this.setData({ phone: e.detail.value })
  },
  login() {
    this.setData({ logging: true })
    // 固定 openId，保证演示环境重复登录为同一会员
    let openId = wx.getStorageSync('demo_openid')
    if (!openId) {
      openId = 'mini-openid-' + Math.floor(Math.random() * 100000)
      wx.setStorageSync('demo_openid', openId)
    }
    util.request('/auth/wechat-login', {
      method: 'POST',
      data: {
        tenantCode: app.globalData.tenantCode,
        openId: openId,
        unionId: openId,
        nickname: this.data.nickname || '微信用户',
        phone: this.data.phone
      }
    }).then((data) => {
      app.globalData.memberToken = data.token
      app.globalData.member = data.member
      wx.setStorageSync('member_token', data.token)
      wx.setStorageSync('member_profile', data.member)
      this.setData({ member: data.member, logging: false })
      wx.showToast({ title: '登录成功', icon: 'success' })
      this.loadProfile()
      this.loadOrders()
      this.loadPoints()
      this.loadCoupons()
    }).catch(() => this.setData({ logging: false }))
  },
  loadProfile() {
    util.request('/member/profile').then((data) => {
      const member = data.member || {}
      this.setData({
        member: member,
        level: data.level || {},
        couponCount: data.couponCount || 0,
        avatarText: (member.name || '微').slice(0, 1)
      })
      app.globalData.member = member
      wx.setStorageSync('member_profile', member)
    }).catch(() => {})
  },
  loadOrders() {
    util.request('/member/orders').then((list) => {
      this.setData({ orders: (list || []).map((o) => {
        const status = o.status || ''
        return Object.assign({}, o, {
          statusText: util.statusMap[status] || status,
          statusClass: ['PENDING', 'CONFIRMED'].indexOf(status) >= 0 ? 'warn' : '',
          amountText: util.money(o.amount)
        })
      }) })
    }).catch(() => {})
  },
  loadPoints() {
    util.request('/member/points').then((list) => {
      this.setData({ points: (list || []).map((p) => {
        return Object.assign({}, p, {
          changeText: (p.changeValue > 0 ? '+' : '') + p.changeValue,
          timeText: String(p.createTime || '').replace('T', ' ').slice(0, 16)
        })
      }) })
    }).catch(() => {})
  },
  loadCoupons() {
    util.request('/member/coupons').then((list) => {
      this.setData({ coupons: (list || []).map((c) => Object.assign({}, c, { amountText: util.money(c.amount) })) })
    }).catch(() => {})
  },
  cancelOrder(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '取消订单',
      content: '确认取消该订单？',
      success: (res) => {
        if (!res.confirm) return
        util.request('/member/orders/' + id + '/cancel', { method: 'POST', data: { reason: '会员自助取消' } })
          .then(() => {
            wx.showToast({ title: '已取消', icon: 'success' })
            this.loadOrders()
          })
      }
    })
  },
  logout() {
    app.globalData.memberToken = ''
    app.globalData.member = null
    wx.removeStorageSync('member_token')
    wx.removeStorageSync('member_profile')
    this.setData({ member: null, orders: [], points: [], coupons: [] })
    wx.showToast({ title: '已退出', icon: 'none' })
  }
})
