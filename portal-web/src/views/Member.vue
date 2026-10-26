<template>
  <div>
    <section class="page-hero">
      <div style="max-width:1080px;margin:0 auto">
        <h1>会员中心</h1>
        <p>微信登录后查看会员等级、积分、优惠券与全部订单</p>
      </div>
    </section>

    <div class="page-body">
      <div v-if="!isLogin" class="panel" style="max-width:560px;margin:0 auto">
        <h2>微信登录（演示）</h2>
        <p class="hint" style="margin-top:-8px;margin-bottom:18px">
          演示环境不跳转微信授权，直接使用 OpenId 模拟登录；正式环境由小程序 / 公众号静默授权获取。
        </p>
        <div class="form-grid">
          <div class="form-field full">
            <label>OpenId</label>
            <input v-model="loginForm.openId" placeholder="演示标识，如 demo-openid-001" />
          </div>
          <div class="form-field">
            <label>昵称</label>
            <input v-model="loginForm.nickname" placeholder="微信昵称" />
          </div>
          <div class="form-field">
            <label>手机号</label>
            <input v-model="loginForm.phone" inputmode="tel" placeholder="选填，用于订单匹配" />
          </div>
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <button class="btn block" style="margin-top:16px" :disabled="logging" @click="login">{{ logging ? '登录中…' : '微信一键登录' }}</button>
        <p class="hint" style="margin-top:12px">登录即代表同意会员服务协议与隐私政策。演示环境仅用于功能体验。</p>
      </div>

      <template v-else>
        <div class="panel">
          <div class="member-head">
            <div class="avatar">{{ (profile.member?.name || 'U').slice(0, 1) }}</div>
            <div style="flex:1">
              <h2 style="margin:0 0 5px">{{ profile.member?.name || '微信用户' }}</h2>
              <p class="hint" style="margin:0">
                {{ profile.member?.phone || '未绑定手机号' }} · {{ profile.level?.name || '普通会员' }}
                <span v-if="profile.level?.discount" class="tag" style="margin-left:6px">{{ Number(profile.level.discount) * 10 }} 折</span>
              </p>
            </div>
            <button class="btn outline" style="padding:8px 16px" @click="logout">退出</button>
          </div>
          <div class="member-stats">
            <div><b>{{ profile.member?.points || 0 }}</b><span>可用积分</span></div>
            <div><b>¥{{ money(profile.member?.balance) }}</b><span>储值余额</span></div>
            <div><b>{{ profile.couponCount || 0 }}</b><span>可用优惠券</span></div>
          </div>
          <p class="hint">{{ profile.level?.benefits || '积分累积，会员专享房价' }}</p>
        </div>

        <div class="panel">
          <div class="tabs">
            <button v-for="tab in tabs" :key="tab.key" :class="{ on: active === tab.key }" @click="switchTab(tab.key)">{{ tab.label }}</button>
          </div>

          <div v-if="active === 'orders'">
            <div v-if="!orders.length" class="empty">还没有订单，去 <router-link to="/booking" style="color:var(--brass)">预订一间</router-link> 吧</div>
            <div v-for="o in orders" :key="o.id" class="order-card">
              <div style="display:flex;justify-content:space-between;gap:10px;align-items:flex-start;flex-wrap:wrap">
                <div>
                  <h4>{{ o.orderNo }}</h4>
                  <p class="hint" style="margin:0">{{ o.checkIn }} → {{ o.checkOut }} · {{ o.nights }} 晚 · {{ o.guestName }}</p>
                </div>
                <span class="tag" :class="{ warn: ['PENDING', 'CONFIRMED'].includes(o.status), muted: o.status === 'CANCELLED' }">{{ statusMap[o.status] || o.status }}</span>
              </div>
              <div class="row" style="margin-top:12px">
                <span class="amount">¥{{ money(o.amount) }}</span>
                <button v-if="['PENDING', 'CONFIRMED'].includes(o.status)" class="btn outline" style="padding:6px 12px;font-size:12px" @click="cancel(o)">取消订单</button>
              </div>
            </div>
          </div>

          <div v-else-if="active === 'points'">
            <div v-if="!points.length" class="empty">暂无积分记录</div>
            <div v-for="p in points" :key="p.id" class="order-card">
              <div class="row">
                <b :style="{ color: p.changeValue > 0 ? 'var(--pine)' : 'var(--danger)' }">{{ p.changeValue > 0 ? '+' : '' }}{{ p.changeValue }} 积分</b>
                <span>{{ dateTime(p.createTime) }}</span>
              </div>
              <p class="hint" style="margin:6px 0 0">{{ p.reason }}</p>
            </div>
          </div>

          <div v-else-if="active === 'coupons'">
            <div v-if="!coupons.length" class="empty">暂无优惠券</div>
            <div class="room-grid" style="grid-template-columns:repeat(2,1fr)">
              <div v-for="c in coupons" :key="c.id" class="order-card" style="margin:0">
                <div style="display:flex;justify-content:space-between;align-items:center">
                  <div>
                    <h4 style="margin-bottom:4px">{{ c.name }}</h4>
                    <p class="hint" style="margin:0">有效期至 {{ c.expireDate || '长期' }}</p>
                  </div>
                  <div class="amount" style="font-size:24px">¥{{ money(c.amount) }}</div>
                </div>
                <span class="tag" style="margin-top:10px">{{ c.status === 'UNUSED' ? '未使用' : c.status }}</span>
              </div>
            </div>
          </div>

          <div v-else-if="active === 'notifications'">
            <div v-if="!notifications.length" class="empty">暂无消息</div>
            <div v-for="n in notifications" :key="n.id" class="order-card">
              <h4>{{ n.title }}</h4>
              <p class="hint" style="margin:0 0 6px">{{ n.content }}</p>
              <span class="hint">{{ dateTime(n.createTime) }}</span>
            </div>
          </div>

          <div v-else-if="active === 'profile'">
            <div class="form-grid" style="max-width:560px">
              <div class="form-field">
                <label>昵称</label>
                <input v-model="profileForm.name" />
              </div>
              <div class="form-field">
                <label>手机号</label>
                <input v-model="profileForm.phone" />
              </div>
              <div class="form-field">
                <label>性别</label>
                <select v-model="profileForm.gender">
                  <option value="M">男</option>
                  <option value="F">女</option>
                  <option value="U">保密</option>
                </select>
              </div>
            </div>
            <button class="btn" style="margin-top:16px" @click="saveProfile">保存资料</button>

            <h3 style="margin-top:30px">修改密码</h3>
            <div class="form-grid" style="max-width:560px">
              <div class="form-field">
                <label>短信验证码</label>
                <input v-model="pwdForm.code" placeholder="演示环境填写手机号后 6 位" />
              </div>
              <div class="form-field">
                <label>新密码</label>
                <input v-model="pwdForm.newPassword" type="password" placeholder="至少 6 位" />
              </div>
            </div>
            <button class="btn outline" style="margin-top:16px" @click="changePassword">修改密码</button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import http from '@/api/http'
import { isMember, loadHome, logoutMember, store, wechatLogin } from '@/stores/portal'
import { dateTime, money, statusMap, toast } from '@/utils'

const active = ref('orders')
const profile = ref<any>({})
const orders = ref<any[]>([])
const points = ref<any[]>([])
const coupons = ref<any[]>([])
const notifications = ref<any[]>([])
const logging = ref(false)
const error = ref('')
const loginForm = reactive({ openId: 'demo-openid-001', nickname: '演示会员', phone: '13900001004' })
const profileForm = reactive<any>({ name: '', phone: '', gender: 'U' })
const pwdForm = reactive({ code: '', newPassword: '' })
const tabs = [
  { key: 'orders', label: '我的订单' },
  { key: 'points', label: '积分明细' },
  { key: 'coupons', label: '优惠券' },
  { key: 'notifications', label: '消息通知' },
  { key: 'profile', label: '个人资料' }
]
const isLogin = computed(() => isMember.value)

async function login() {
  error.value = ''
  if (!loginForm.openId) return error.value = '请填写 OpenId'
  logging.value = true
  try {
    await wechatLogin({ openId: loginForm.openId, unionId: loginForm.openId, nickname: loginForm.nickname, phone: loginForm.phone })
    await loadProfile()
    toast('登录成功，欢迎回来')
  } catch (e: any) {
    error.value = e.message || '登录失败'
  } finally {
    logging.value = false
  }
}

async function loadProfile() {
  const res: any = await http.get('/member/profile')
  profile.value = res.data || {}
  const m = profile.value.member || {}
  Object.assign(profileForm, { name: m.name || '', phone: m.phone || '', gender: m.gender || 'U' })
}

async function loadTab(key: string) {
  if (key === 'orders') orders.value = ((await http.get('/member/orders')) as any).data || []
  if (key === 'points') points.value = ((await http.get('/member/points')) as any).data || []
  if (key === 'coupons') coupons.value = ((await http.get('/member/coupons')) as any).data || []
  if (key === 'notifications') notifications.value = ((await http.get('/member/notifications')) as any).data || []
}

async function switchTab(key: string) {
  active.value = key
  await loadTab(key)
}

async function saveProfile() {
  await http.put('/member/profile', profileForm)
  toast('资料已保存')
  await loadProfile()
}

async function changePassword() {
  try {
    await http.post('/member/password', pwdForm)
    toast('密码修改成功')
    pwdForm.code = ''
    pwdForm.newPassword = ''
  } catch (e: any) {
    toast(e.message || '修改失败')
  }
}

async function cancel(o: any) {
  if (!confirm(`确认取消订单 ${o.orderNo}？`)) return
  try {
    await http.post(`/member/orders/${o.id}/cancel`, { reason: '会员自助取消' })
    toast('订单已取消')
    await loadTab('orders')
  } catch (e: any) {
    toast(e.message || '取消失败')
  }
}

function logout() {
  logoutMember()
  profile.value = {}
  toast('已退出登录')
}

onMounted(async () => {
  await loadHome()
  if (isLogin.value) {
    await loadProfile()
    await loadTab('orders')
  }
})
</script>
