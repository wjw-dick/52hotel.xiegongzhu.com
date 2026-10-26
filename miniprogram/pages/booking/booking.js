const app = getApp()
const util = require('../../utils/util')

Page({
  data: {
    stores: [],
    types: [],
    storeIndex: 0,
    typeIndex: 0,
    available: null,
    unitPrice: 0,
    nights: 2,
    total: 0,
    submitting: false,
    today: util.today(),
    form: {
      storeId: '',
      roomTypeId: '',
      checkIn: util.today(),
      checkOut: util.addDays(util.today(), 2),
      roomCount: 1,
      payType: 'ARRIVAL',
      guestName: '',
      guestPhone: '',
      idNo: '',
      company: '',
      remark: '',
      source: 'ONLINE'
    }
  },
  onLoad(options) {
    const form = Object.assign({}, this.data.form)
    if (options.checkIn) form.checkIn = options.checkIn
    if (options.checkOut) form.checkOut = options.checkOut
    if (options.roomTypeId) form.roomTypeId = options.roomTypeId
    this.setData({ form: form })
    app.loadHome().then((home) => this.apply(home)).catch(() => {
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },
  apply(home) {
    const stores = home.stores || []
    const types = (home.roomTypes || []).map((r) => {
      return Object.assign({}, r, { label: r.name + ' · ¥' + r.basePrice + ' 起' })
    })
    let storeIndex = 0
    let typeIndex = 0
    const form = Object.assign({}, this.data.form)
    if (form.roomTypeId) {
      typeIndex = Math.max(0, types.findIndex((t) => String(t.id) === String(form.roomTypeId)))
      if (typeIndex >= 0 && types[typeIndex]) {
        storeIndex = Math.max(0, stores.findIndex((s) => s.id === types[typeIndex].storeId))
        form.storeId = String(stores[storeIndex].id)
      }
    } else if (stores.length) {
      form.storeId = String(stores[0].id)
    }
    if (!form.roomTypeId && types.length) {
      form.roomTypeId = String(types[0].id)
      form.storeId = String(types[0].storeId)
      storeIndex = Math.max(0, stores.findIndex((s) => s.id === types[0].storeId))
    }
    this.setData({ stores: stores, types: types, storeIndex: storeIndex, typeIndex: typeIndex, form: form })
    this.calc()
    this.checkAvailability()
  },
  onStore(e) {
    const storeIndex = Number(e.detail.value)
    const storeId = String(this.data.stores[storeIndex].id)
    const types = (app.globalData.home.roomTypes || []).filter((r) => String(r.storeId) === storeId)
      .map((r) => Object.assign({}, r, { label: r.name + ' · ¥' + r.basePrice + ' 起' }))
    const form = Object.assign({}, this.data.form, { storeId: storeId, roomTypeId: types.length ? String(types[0].id) : '' })
    this.setData({ storeIndex: storeIndex, typeIndex: 0, types: types, form: form })
    this.calc()
    this.checkAvailability()
  },
  onType(e) {
    const typeIndex = Number(e.detail.value)
    const type = this.data.types[typeIndex]
    const form = Object.assign({}, this.data.form, { roomTypeId: type ? String(type.id) : '' })
    this.setData({ typeIndex: typeIndex, form: form })
    this.calc()
    this.checkAvailability()
  },
  onCheckIn(e) {
    const checkIn = e.detail.value
    const checkOut = checkIn >= this.data.form.checkOut ? util.addDays(checkIn, 1) : this.data.form.checkOut
    this.setData({ 'form.checkIn': checkIn, 'form.checkOut': checkOut })
    this.calc()
    this.checkAvailability()
  },
  onCheckOut(e) {
    this.setData({ 'form.checkOut': e.detail.value })
    this.calc()
    this.checkAvailability()
  },
  onRoomCount(e) {
    this.setData({ 'form.roomCount': Number(e.detail.value) + 1 })
    this.calc()
  },
  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ ['form.' + field]: e.detail.value })
  },
  calc() {
    const f = this.data.form
    const a = new Date(f.checkIn.replace(/-/g, '/')).getTime()
    const b = new Date(f.checkOut.replace(/-/g, '/')).getTime()
    const nights = Math.max(1, Math.round((b - a) / 86400000))
    const type = this.data.types[this.data.typeIndex]
    const unitPrice = Number((type && type.basePrice) || this.data.unitPrice || 0)
    this.setData({ nights: nights, unitPrice: unitPrice, total: unitPrice * nights * f.roomCount })
  },
  checkAvailability() {
    const f = this.data.form
    if (!f.roomTypeId) return
    util.request(util.publicPath('/availability'), {
      data: { roomTypeId: f.roomTypeId, checkIn: f.checkIn, checkOut: f.checkOut }
    }).then((data) => {
      this.setData({ available: (data.rooms || []).length, unitPrice: Number(data.price || this.data.unitPrice) })
      this.calc()
    }).catch(() => this.setData({ available: 0 }))
  },
  submit() {
    const f = this.data.form
    if (!f.roomTypeId) return wx.showToast({ title: '请选择房型', icon: 'none' })
    if (!f.guestName) return wx.showToast({ title: '请填写入住人', icon: 'none' })
    if (!/^1\d{10}$/.test(f.guestPhone)) return wx.showToast({ title: '请填写正确手机号', icon: 'none' })
    if (!f.idNo || f.idNo.length < 15) return wx.showToast({ title: '请填写身份证号', icon: 'none' })
    if (this.data.available === 0) return wx.showToast({ title: '所选日期暂无可订房', icon: 'none' })
    this.setData({ submitting: true })
    const payload = {
      storeId: Number(f.storeId),
      roomTypeId: Number(f.roomTypeId),
      checkIn: f.checkIn,
      checkOut: f.checkOut,
      roomCount: f.roomCount,
      payType: f.payType,
      guestName: f.guestName,
      guestPhone: f.guestPhone,
      idNo: f.idNo,
      company: f.company,
      remark: f.remark,
      source: 'ONLINE',
      channel: 'MINI'
    }
    util.request(util.publicPath('/orders'), {
      method: 'POST',
      data: payload,
      header: { 'X-Request-Id': 'mini-' + Date.now() + '-' + Math.floor(Math.random() * 9999) }
    }).then((order) => {
      this.setData({ submitting: false })
      wx.showModal({
        title: '预订成功',
        content: '订单号 ' + order.orderNo + '，请保持手机畅通。',
        showCancel: false,
        success: () => {
          wx.setStorageSync('last_order', { orderNo: order.orderNo, phone: f.guestPhone })
          wx.switchTab({ url: '/pages/orders/orders' })
        }
      })
    }).catch(() => this.setData({ submitting: false }))
  }
})
