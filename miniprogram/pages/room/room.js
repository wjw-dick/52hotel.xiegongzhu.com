const util = require('../../utils/util')

Page({
  data: {
    id: null,
    room: {},
    gallery: [],
    calendar: [],
    checkIn: util.today(),
    checkOut: util.addDays(util.today(), 2),
    today: util.today(),
    available: null,
    nights: 2,
    total: 0
  },
  onLoad(options) {
    this.setData({
      id: options.id,
      checkIn: options.checkIn || util.today(),
      checkOut: options.checkOut || util.addDays(util.today(), 2)
    })
    this.load()
  },
  load() {
    util.request(util.publicPath('/room-types/' + this.data.id)).then((data) => {
      const calendar = (data.calendar || []).map((d) => {
        return Object.assign({}, d, { shortDate: d.date.slice(5) })
      })
      const rt = data.roomType || {}
      const images = util.parseImages(rt.images)
      const gallery = images.length ? images : (rt.cover ? [rt.cover] : [])
      this.setData({ room: rt, gallery: gallery, calendar: calendar })
      wx.setNavigationBarTitle({ title: (rt && rt.name) || '房型详情' })
      this.calc()
      this.checkAvailability()
    })
  },
  calc() {
    const a = new Date(this.data.checkIn.replace(/-/g, '/')).getTime()
    const b = new Date(this.data.checkOut.replace(/-/g, '/')).getTime()
    const nights = Math.max(1, Math.round((b - a) / 86400000))
    const total = Number(this.data.room.basePrice || 0) * nights
    this.setData({ nights: nights, total: total })
  },
  checkAvailability() {
    util.request(util.publicPath('/availability'), {
      data: { roomTypeId: this.data.id, checkIn: this.data.checkIn, checkOut: this.data.checkOut }
    }).then((data) => {
      this.setData({ available: (data.rooms || []).length })
    }).catch(() => this.setData({ available: 0 }))
  },
  onCheckIn(e) {
    const checkIn = e.detail.value
    this.setData({ checkIn: checkIn, checkOut: checkIn >= this.data.checkOut ? util.addDays(checkIn, 1) : this.data.checkOut })
    this.calc()
    this.checkAvailability()
  },
  onCheckOut(e) {
    this.setData({ checkOut: e.detail.value })
    this.calc()
    this.checkAvailability()
  },
  goBook() {
    wx.navigateTo({
      url: `/pages/booking/booking?roomTypeId=${this.data.id}&checkIn=${this.data.checkIn}&checkOut=${this.data.checkOut}`
    })
  }
})
