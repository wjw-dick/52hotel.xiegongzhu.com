import { defineStore } from 'pinia'
import http from '@/api/http'

export interface Profile {
  userId: number
  username: string
  realName?: string
  role: string
  tenantId: number
  tenantCode: string
  tenantName?: string
  platform: boolean
  storeId?: number
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('hotel_token') || '',
    profile: JSON.parse(localStorage.getItem('hotel_profile') || 'null') as Profile | null,
    drillTenantId: Number(localStorage.getItem('hotel_drill_tenant') || 0),
    drillTenantName: localStorage.getItem('hotel_drill_tenant_name') || ''
  }),
  getters: {
    isLogin: (s) => !!s.token,
    isPlatform: (s) => !!s.profile?.platform,
    effectiveTenantId: (s) => (s.profile?.platform ? s.drillTenantId : s.profile?.tenantId || 0)
  },
  actions: {
    async login(payload: { username: string; password: string; tenantCode?: string }) {
      const res: any = await http.post('/auth/login', payload)
      const data = res.data
      this.token = data.token
      this.profile = data
      localStorage.setItem('hotel_token', data.token)
      localStorage.setItem('hotel_profile', JSON.stringify(data))
      return data
    },
    async loadProfile() {
      const res: any = await http.get('/auth/me')
      this.profile = res.data
      localStorage.setItem('hotel_profile', JSON.stringify(res.data))
      return res.data
    },
    setDrillTenant(id: number, name: string) {
      this.drillTenantId = id
      this.drillTenantName = name
      localStorage.setItem('hotel_drill_tenant', String(id))
      localStorage.setItem('hotel_drill_tenant_name', name)
    },
    clearDrill() {
      this.drillTenantId = 0
      this.drillTenantName = ''
      localStorage.removeItem('hotel_drill_tenant')
      localStorage.removeItem('hotel_drill_tenant_name')
    },
    logout() {
      this.token = ''
      this.profile = null
      localStorage.removeItem('hotel_token')
      localStorage.removeItem('hotel_profile')
      this.clearDrill()
    }
  }
})
