export function money(v: unknown, digits = 0): string {
  return Number(v ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

export function today(): string {
  return new Date().toISOString().slice(0, 10)
}

export function addDays(date: string, days: number): string {
  const d = new Date(date + 'T00:00:00')
  d.setDate(d.getDate() + days)
  return d.toISOString().slice(0, 10)
}

export function dateTime(v?: string): string {
  return v ? String(v).replace('T', ' ').slice(0, 16) : '-'
}

export const statusMap: Record<string, string> = {
  PENDING: '待确认', CONFIRMED: '已确认', CHECKED_IN: '在住', CHECKED_OUT: '已退房',
  CANCELLED: '已取消', NO_SHOW: '未到店'
}

export function toast(msg: string) {
  // 由 App.vue 监听 store.notice，简单延迟清除
  import('@/stores/portal').then(({ store }) => {
    store.notice = msg
    setTimeout(() => { if (store.notice === msg) store.notice = '' }, 2600)
  })
}
