export function money(v: unknown, digits = 2): string {
  const n = Number(v ?? 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

export function today(): string {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

export function addDays(date: string, days: number): string {
  const d = new Date(date + 'T00:00:00')
  d.setDate(d.getDate() + days)
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

export function monthStart(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
}

export function dateTime(v?: string): string {
  return v ? String(v).replace('T', ' ').slice(0, 16) : '-'
}

export const orderStatus: Record<string, { text: string; type: string }> = {
  PENDING: { text: '待确认', type: 'warning' },
  CONFIRMED: { text: '已确认', type: 'primary' },
  CHECKED_IN: { text: '在住', type: 'success' },
  CHECKED_OUT: { text: '已退房', type: 'info' },
  CANCELLED: { text: '已取消', type: 'danger' },
  NO_SHOW: { text: '未到店', type: 'danger' }
}

export const roomStatus: Record<string, { text: string; type: string; color: string }> = {
  VACANT: { text: '空房', type: 'success', color: '#2eb67d' },
  RESERVED: { text: '预订', type: 'primary', color: '#1f6feb' },
  OCCUPIED: { text: '在住', type: 'warning', color: '#e6a23c' },
  DIRTY: { text: '脏房', type: 'danger', color: '#f56c6c' },
  REPAIR: { text: '维修', type: 'info', color: '#909399' },
  CLOSED: { text: '停用', type: 'info', color: '#606266' }
}

export const sourceMap: Record<string, string> = {
  WALK_IN: '门店散客',
  ONLINE: '官网/小程序',
  OTA: 'OTA 渠道',
  MEMBER: '会员预订',
  PHONE: '电话预订',
  WAITLIST: '候补'
}

export const payTypeMap: Record<string, string> = {
  ARRIVAL: '到店支付',
  WECHAT: '微信支付',
  ALIPAY: '支付宝',
  CASH: '现金',
  CARD: '银行卡',
  AR: '协议挂账',
  ONLINE: '在线支付'
}

export const hkStatus: Record<string, { text: string; type: string }> = {
  PENDING: { text: '待处理', type: 'warning' },
  PROCESSING: { text: '处理中', type: 'primary' },
  DONE: { text: '已完成', type: 'success' },
  REPORTED: { text: '已报修', type: 'danger' },
  ASSIGNED: { text: '已派工', type: 'warning' },
  REPAIRING: { text: '维修中', type: 'primary' },
  ACCEPTING: { text: '待验收', type: 'info' }
}

export const taskTypeMap: Record<string, string> = {
  CHECKOUT: '退房清扫',
  CONTINUE: '续住清扫',
  MAINTAIN: '日常保洁',
  DEEP: '深度清洁',
  INSPECT: '查房'
}
