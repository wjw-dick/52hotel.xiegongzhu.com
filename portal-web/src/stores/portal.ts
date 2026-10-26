import { computed, reactive } from 'vue'
import http from '@/api/http'

export const store = reactive({
  tenantCode: localStorage.getItem('portal_tenant') || (import.meta as any).env.VITE_TENANT_CODE || 'YQ-HZ',
  lang: localStorage.getItem('portal_lang') || 'zh-CN',
  home: null as any,
  loading: false,
  member: JSON.parse(localStorage.getItem('portal_member_profile') || 'null'),
  notice: '' as string
})

export const isMember = computed(() => !!localStorage.getItem('portal_member_token'))

export async function loadHome(force = false) {
  if (store.home && !force) return store.home
  store.loading = true
  try {
    const res: any = await http.get(`/public/${store.tenantCode}/home`)
    store.home = res.data
    document.title = `${res.data?.config?.tenant_name || res.data?.tenant?.name || '云栖民宿'} · ${res.data?.config?.slogan || '山水之间，安心住下'}`
    return store.home
  } finally {
    store.loading = false
  }
}

export function t(key: string, fallback: string): string {
  const dict = store.home?.i18n || {}
  return dict[`${store.lang}:${key}`] || fallback
}

export function switchLang(lang: string) {
  store.lang = lang
  localStorage.setItem('portal_lang', lang)
  const meta: Record<string, string> = {
    'zh-CN': '云栖民宿 · 山水之间，安心住下',
    en: 'Yunqi Retreat · Stay between mountains and water'
  }
  document.title = meta[lang] || meta['zh-CN']
}

export async function wechatLogin(payload: { openId: string; unionId?: string; nickname: string; phone: string }) {
  const res: any = await http.post('/auth/wechat-login', { tenantCode: store.tenantCode, ...payload })
  localStorage.setItem('portal_member_token', res.data.token)
  localStorage.setItem('portal_member_profile', JSON.stringify(res.data))
  store.member = res.data
  return res.data
}

export function logoutMember() {
  localStorage.removeItem('portal_member_token')
  localStorage.removeItem('portal_member_profile')
  store.member = null
}
