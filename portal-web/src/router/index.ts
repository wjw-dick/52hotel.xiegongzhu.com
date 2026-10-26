import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/', name: 'home', component: () => import('@/views/Home.vue'), meta: { title: '民宿预订' } },
  { path: '/rooms/:id', name: 'room', component: () => import('@/views/RoomDetail.vue'), meta: { title: '房型详情' } },
  { path: '/booking', name: 'booking', component: () => import('@/views/Booking.vue'), meta: { title: '在线预订' } },
  { path: '/query', name: 'query', component: () => import('@/views/OrderQuery.vue'), meta: { title: '订单查询' } },
  { path: '/member', name: 'member', component: () => import('@/views/Member.vue'), meta: { title: '会员中心' } }
]

const router = createRouter({ history: createWebHistory('/'), routes, scrollBehavior: () => ({ top: 0 }) })

router.afterEach((to) => {
  document.title = `${to.meta.title || '云栖民宿'} · 云栖民宿`
})

export default router
