import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '经营概览', icon: 'DataLine', group: '经营' } },
      { path: 'platform', name: 'platform', component: () => import('@/views/PlatformDashboard.vue'), meta: { title: '平台总览', icon: 'Monitor', platformOnly: true, group: '平台' } },
      { path: 'tenants', name: 'tenants', component: () => import('@/views/Tenants.vue'), meta: { title: '租户管理', icon: 'OfficeBuilding', platformOnly: true, group: '平台' } },
      { path: 'platform-dict', name: 'platformDict', component: () => import('@/views/PlatformDict.vue'), meta: { title: '全局字典', icon: 'Collection', platformOnly: true, group: '平台' } },
      { path: 'platform-audit', name: 'platformAudit', component: () => import('@/views/PlatformAudit.vue'), meta: { title: '平台审计', icon: 'Document', platformOnly: true, group: '平台' } },
      { path: 'stores', name: 'stores', component: () => import('@/views/Stores.vue'), meta: { title: '门店与房源', icon: 'House', tenant: true, group: '运营' } },
      { path: 'rooms', name: 'rooms', component: () => import('@/views/Rooms.vue'), meta: { title: '房型/房间/房价', icon: 'Grid', tenant: true, group: '运营' } },
      { path: 'orders', name: 'orders', component: () => import('@/views/Orders.vue'), meta: { title: '预订管理', icon: 'Tickets', tenant: true, group: '运营' } },
      { path: 'front-desk', name: 'frontDesk', component: () => import('@/views/FrontDesk.vue'), meta: { title: '前台接待', icon: 'Service', tenant: true, group: '运营' } },
      { path: 'housekeeping', name: 'housekeeping', component: () => import('@/views/Housekeeping.vue'), meta: { title: '房务保洁', icon: 'Brush', tenant: true, group: '运营' } },
      { path: 'finance', name: 'finance', component: () => import('@/views/Finance.vue'), meta: { title: '收银财务', icon: 'Money', tenant: true, group: '运营' } },
      { path: 'members', name: 'members', component: () => import('@/views/Members.vue'), meta: { title: '会员营销', icon: 'User', tenant: true, group: '运营' } },
      { path: 'reports', name: 'reports', component: () => import('@/views/Reports.vue'), meta: { title: '报表中心', icon: 'TrendCharts', tenant: true, group: '运营' } },
      { path: 'config', name: 'config', component: () => import('@/views/Config.vue'), meta: { title: '系统配置', icon: 'Setting', tenant: true, group: '系统' } },
      { path: 'users', name: 'users', component: () => import('@/views/Users.vue'), meta: { title: '用户与角色', icon: 'UserFilled', tenant: true, group: '系统' } },
      { path: 'audit', name: 'audit', component: () => import('@/views/Audit.vue'), meta: { title: '审计日志', icon: 'Document', tenant: true, group: '系统' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

const ROLE_PATHS: Record<string, string[]> = {
  '/orders': ['TENANT_ADMIN', 'STORE_MANAGER', 'FRONT_DESK', 'OTA_OPERATOR'],
  '/front-desk': ['TENANT_ADMIN', 'STORE_MANAGER', 'FRONT_DESK'],
  '/housekeeping': ['TENANT_ADMIN', 'STORE_MANAGER', 'FRONT_DESK', 'HOUSEKEEPING'],
  '/finance': ['TENANT_ADMIN', 'STORE_MANAGER', 'FINANCE'],
  '/members': ['TENANT_ADMIN', 'STORE_MANAGER', 'FRONT_DESK', 'OTA_OPERATOR'],
  '/config': ['TENANT_ADMIN'],
  '/users': ['TENANT_ADMIN'],
  '/audit': ['TENANT_ADMIN']
}

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    if (auth.isLogin && to.name === 'login') return { path: auth.isPlatform ? '/platform' : '/dashboard' }
    return true
  }
  if (!auth.isLogin) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.meta.platformOnly && !auth.isPlatform) return { path: '/dashboard' }
  if (to.meta.tenant && auth.isPlatform && !auth.drillTenantId) return { path: '/platform' }
  if (!auth.isPlatform) {
    const roles = ROLE_PATHS[to.path]
    if (roles && !roles.includes(auth.profile?.role || '')) return { path: '/dashboard' }
  }
  return true
})

router.afterEach((to) => {
  document.title = `${to.meta.title || '控制台'} · 云宿 SaaS`
})

export default router
