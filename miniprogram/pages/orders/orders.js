const app = getApp()
const util = require('../../utils/util')

Page({
  data: {
    orderNo: '',
    phone: '',
    order: null,
    loading: false,
    member: null,
    myOrders: []
  },
  onShow() {
    this.setData({ member: app.globalData.member })
    if (app.globalData.member) this.loadMyOrders()
    const last = wx.getStorageSync('last_order')
    if (last && last.orderNo && !this.data.orderNo) {
      this.setData({ orderNo: last.orderNo, phone: last.phone })
      wx.removeStorageSync('last_order')
      this.query()
    }
  },
  onOrderNo(e) {
    this.setData({ orderNo: e.detail.value })
  },
  onPhone(e) {
    this.setData({ phone: e.detail.value })
  },
  format(order) {
    const status = order.status || ''
    const warn = ['PENDING', 'CONFIRMED'].indexOf(status) >= 0
    const muted = ['CANCELLED', 'NO_SHOW'].indexOf(status) >= 0
    return Object.assign({}, order, {
      statusText: util.statusMap[status] || status,
      statusClass: warn ? 'warn' : (muted ? 'muted' : ''),
      amountText: util.money(order.amount)
    })
  },
  query() {
    const orderNo = this.data.orderNo.trim()
    const phone = this.data.phone.trim()
    if (!orderNo || !phone) return wx.showToast({ title: '请填写订单号与手机号', icon: 'none' })
    this.setData({ loading: true })
    util.request(util.publicPath('/orders/query'), { data: { orderNo: orderNo, phone: phone } })
      .then((data) => {
        this.setData({ order: this.format(data.order || {}) })
      })
      .catch(() => this.setData({ order: null }))
      .finally(() => this.setData({ loading: false }))
  },
  loadMyOrders() {
    util.request('/member/orders').then((list) => {
      this.setData({ myOrders: (list || []).map((o) => {
        const item = this.format(o)
        item.checkIn = o.checkIn
        item.checkOut = o.checkOut
        item.nights = o.nights
        return item
      }) })
    }).catch(() => {})
  },
  cancelOrder(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '取消订单',
      content: '确认取消该订单？取消后不可恢复。',
      success: (res) => {
        if (!res.confirm) return
        util.request('/member/orders/' + id + '/cancel', { method: 'POST', data: { reason: '会员自助取消' } })
          .then(() => {
            wx.showToast({ title: '已取消', icon: 'success' })
            this.loadMyOrders()
          })
      }
    })
  }
})
