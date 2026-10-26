const app = getApp()
const util = require('../../utils/util')

Page({
  data: {
    home: { config: {}, stores: [] },
    rooms: [],
    checkIn: util.today(),
    checkOut: util.addDays(util.today(), 2),
    today: util.today()
  },
  onLoad() {
    this.load()
  },
  onShow() {
    if (app.globalData.home && this.data.rooms.length === 0) this.apply(app.globalData.home)
  },
  onPullDownRefresh() {
    this.load(true)
  },
  load(force) {
    const done = (home) => {
      this.apply(home)
      wx.stopPullDownRefresh()
    }
    if (app.globalData.home && !force) return done(app.globalData.home)
    app.loadHome().then(done).catch((e) => {
      wx.showToast({ title: e.message || '加载失败', icon: 'none' })
      wx.stopPullDownRefresh()
    })
  },
  apply(home) {
    this.setData({ home: home, rooms: home.roomTypes || [] })
    wx.setNavigationBarTitle({ title: (home.config && home.config.tenant_name) || '云栖民宿' })
  },
  onCheckIn(e) {
    const checkIn = e.detail.value
    this.setData({ checkIn: checkIn, checkOut: checkIn >= this.data.checkOut ? util.addDays(checkIn, 1) : this.data.checkOut })
  },
  onCheckOut(e) {
    this.setData({ checkOut: e.detail.value })
  },
  goBooking() {
    wx.navigateTo({ url: `/pages/booking/booking?checkIn=${this.data.checkIn}&checkOut=${this.data.checkOut}` })
  },
  goRoom(e) {
    wx.navigateTo({ url: `/pages/room/room?id=${e.currentTarget.dataset.id}&checkIn=${this.data.checkIn}&checkOut=${this.data.checkOut}` })
  }
})
