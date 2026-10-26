<template>
  <div class="shell">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="brand" @click="router.push(homePath)">
        <span class="logo">云</span>
        <div v-show="!collapsed" class="brand-text">
          <b>云宿 SaaS</b>
          <span>{{ auth.isPlatform ? '平台运营中心' : auth.profile?.tenantName }}</span>
        </div>
      </div>

      <el-scrollbar class="menu-scroll">
        <div v-for="group in menuGroups" :key="group.name" class="menu-group">
          <div v-show="!collapsed" class="group-name">{{ group.name }}</div>
          <div
            v-for="item in group.items"
            :key="item.path"
            class="menu-item"
            :class="{ active: route.path === item.path }"
            @click="go(item.path)"
          >
            <el-icon :size="17"><component :is="item.icon" /></el-icon>
            <span v-show="!collapsed">{{ item.title }}</span>
          </div>
        </div>
      </el-scrollbar>

      <div class="sidebar-foot" v-show="!collapsed">
        <div class="tiny">当前版本 v1.0.0</div>
        <div class="tiny">SaaS 多租户 · utf8mb4</div>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="top-left">
          <el-icon class="fold" @click="collapsed = !collapsed"><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ auth.isPlatform ? '平台端' : '租户端' }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="top-right">
          <template v-if="auth.isPlatform">
            <el-select
              v-model="selectedTenant"
              placeholder="选择租户下钻"
              filterable
              clearable
              size="default"
              class="tenant-select"
              @change="onTenantChange"
            >
              <el-option v-for="t in tenants" :key="t.id" :label="`${t.name} · ${t.code}`" :value="t.id" />
            </el-select>
            <el-tag v-if="auth.drillTenantId" type="warning" effect="plain" closable @close="exitDrill">只读下钻中</el-tag>
          </template>
          <template v-else>
            <span class="tenant-badge"><el-icon><OfficeBuilding /></el-icon>{{ auth.profile?.tenantName }}</span>
          </template>

          <el-popover placement="bottom-end" :width="330" trigger="click">
            <template #reference>
              <el-badge :value="unread" :hidden="!unread" class="bell">
                <el-icon :size="19"><Bell /></el-icon>
              </el-badge>
            </template>
            <div class="notice-head">站内消息 <el-button link type="primary" size="small" @click="loadNotices">刷新</el-button></div>
            <div v-if="!notices.length" class="notice-empty">暂无新消息</div>
            <div v-for="n in notices" :key="n.id" class="notice-item" @click="readNotice(n)">
              <b>{{ n.title }}</b>
              <p>{{ n.content }}</p>
              <span>{{ n.createTime }}</span>
            </div>
          </el-popover>

          <el-dropdown trigger="click" @command="onUserCommand">
            <div class="avatar-wrap">
              <div class="avatar">{{ (auth.profile?.realName || auth.profile?.username || 'U').slice(0, 1) }}</div>
              <div class="who">
                <b>{{ auth.profile?.realName || auth.profile?.username }}</b>
                <span>{{ roleName }}</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <div v-if="auth.isPlatform && auth.drillTenantId" class="drill-banner">
          当前为只读下钻：{{ auth.drillTenantName }}。平台端不能修改租户业务数据，如需操作请使用该租户账号登录。
        </div>
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>

    <el-dialog v-model="pwdVisible" title="修改密码" width="420px">
      <el-form label-width="82px">
        <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="pwd.confirm" type="password" show-password /></el-form-item>
      </el-form>
      <p class="muted">密码至少 6 位；平台超管、租户管理员与各业务角色均可自助修改。</p>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="changePassword">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)
const tenants = ref<any[]>([])
const selectedTenant = ref<number | undefined>(auth.drillTenantId || undefined)
const notices = ref<any[]>([])
const pwdVisible = ref(false)
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const homePath = computed(() => (auth.isPlatform ? '/platform' : '/dashboard'))
const roleName = computed(() => {
  const map: Record<string, string> = {
    PLATFORM_ADMIN: '平台超级管理员', TENANT_ADMIN: '租户管理员', FRONT_DESK: '前台接待',
    HOUSEKEEPING: '客房服务', FINANCE: '财务收银', STORE_MANAGER: '店长', OTA_OPERATOR: 'OTA 运营'
  }
  return map[auth.profile?.role || ''] || auth.profile?.role || '用户'
})

const ALL_ROLES = ['TENANT_ADMIN', 'STORE_MANAGER', 'FRONT_DESK', 'HOUSEKEEPING', 'FINANCE', 'OTA_OPERATOR']
const allItems = [
  { path: '/dashboard', title: '经营概览', icon: 'DataLine', tenant: true, roles: ALL_ROLES },
  { path: '/stores', title: '门店与房源', icon: 'House', tenant: true, roles: ALL_ROLES },
  { path: '/rooms', title: '房型 / 房间', icon: 'Grid', tenant: true, roles: ALL_ROLES },
  { path: '/orders', title: '预订管理', icon: 'Tickets', tenant: true, roles: ['TENANT_ADMIN', 'STORE_MANAGER', 'FRONT_DESK', 'OTA_OPERATOR'] },
  { path: '/front-desk', title: '前台接待', icon: 'Service', tenant: true, roles: ['TENANT_ADMIN', 'STORE_MANAGER', 'FRONT_DESK'] },
  { path: '/housekeeping', title: '房务保洁', icon: 'Brush', tenant: true, roles: ['TENANT_ADMIN', 'STORE_MANAGER', 'FRONT_DESK', 'HOUSEKEEPING'] },
  { path: '/finance', title: '收银财务', icon: 'Money', tenant: true, roles: ['TENANT_ADMIN', 'STORE_MANAGER', 'FINANCE'] },
  { path: '/members', title: '会员营销', icon: 'User', tenant: true, roles: ['TENANT_ADMIN', 'STORE_MANAGER', 'FRONT_DESK', 'OTA_OPERATOR'] },
  { path: '/reports', title: '报表中心', icon: 'TrendCharts', tenant: true, roles: ALL_ROLES },
  { path: '/config', title: '系统配置', icon: 'Setting', tenant: true, roles: ['TENANT_ADMIN'] },
  { path: '/users', title: '用户与角色', icon: 'UserFilled', tenant: true, roles: ['TENANT_ADMIN'] },
  { path: '/audit', title: '审计日志', icon: 'Document', tenant: true, roles: ['TENANT_ADMIN'] },
  { path: '/platform', title: '平台总览', icon: 'Monitor', platform: true },
  { path: '/tenants', title: '租户管理', icon: 'OfficeBuilding', platform: true },
  { path: '/platform-dict', title: '全局字典', icon: 'Collection', platform: true },
  { path: '/platform-audit', title: '平台审计', icon: 'Document', platform: true }
]

const menuGroups = computed(() => {
  const groups: any[] = []
  const showTenant = !auth.isPlatform || !!auth.drillTenantId
  if (showTenant) {
    const role = auth.profile?.role || ''
    groups.push({
      name: auth.isPlatform ? '租户下钻' : '运营中心',
      items: allItems.filter(i => i.tenant && (!i.roles || auth.isPlatform || i.roles.includes(role)))
    })
  }
  if (auth.isPlatform) {
    groups.push({ name: '平台管理', items: allItems.filter(i => i.platform) })
  }
  return groups
})

const unread = computed(() => notices.value.filter(n => n.status !== 'READ').length)

function go(path: string) {
  router.push(path)
}

async function loadTenants() {
  if (!auth.isPlatform) return
  try {
    const res: any = await http.get('/platform/tenants/options')
    tenants.value = res.data
  } catch { /* ignore */ }
}

function onTenantChange(id: number | undefined) {
  if (!id) {
    exitDrill()
    return
  }
  const t = tenants.value.find(x => x.id === id)
  auth.setDrillTenant(id, t?.name || '')
  ElMessage.success(`已切换至 ${t?.name}（平台下钻只读）`)
  selectedTenant.value = id
  router.push('/dashboard')
  loadNotices()
}

function exitDrill() {
  auth.clearDrill()
  selectedTenant.value = undefined
  router.push('/platform')
  notices.value = []
}

async function loadNotices() {
  if (auth.isPlatform && !auth.drillTenantId) return
  try {
    const res: any = await http.get('/admin/notifications')
    notices.value = res.data || []
  } catch { /* ignore */ }
}

async function readNotice(n: any) {
  if (n.status === 'READ') return
  await http.post(`/admin/notifications/${n.id}/read`)
  n.status = 'READ'
}

function onUserCommand(cmd: string) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确认退出当前账号？', '提示', { type: 'warning' }).then(() => {
      auth.logout()
      router.push('/login')
    }).catch(() => {})
  } else if (cmd === 'password') {
    pwd.oldPassword = ''
    pwd.newPassword = ''
    pwd.confirm = ''
    pwdVisible.value = true
  }
}

async function changePassword() {
  if (!pwd.oldPassword || !pwd.newPassword) return ElMessage.warning('请填写完整')
  if (pwd.newPassword.length < 6) return ElMessage.warning('新密码至少 6 位')
  if (pwd.newPassword !== pwd.confirm) return ElMessage.warning('两次输入的新密码不一致')
  await http.post('/auth/password', { oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
  ElMessage.success('密码修改成功，请妥善保管')
  pwdVisible.value = false
}

onMounted(() => {
  loadTenants()
  loadNotices()
})
</script>

<style scoped>
.drill-banner {
  margin: 0 0 16px;
  padding: 10px 14px;
  border: 1px solid #f0d7a8;
  background: #fdf6e8;
  color: #8a6427;
  border-radius: 6px;
  font-size: 13px;
}

.shell { display: flex; height: 100%; background: var(--bg); }
.sidebar { width: 224px; flex: 0 0 224px; background: #101b2e; color: #d6dfef; display: flex; flex-direction: column; transition: .22s; }
.sidebar.collapsed { width: 68px; flex-basis: 68px; }
.brand { height: 64px; display: flex; align-items: center; gap: 10px; padding: 0 16px; cursor: pointer; flex: 0 0 64px; border-bottom: 1px solid rgba(255,255,255,.06); }
.logo { width: 34px; height: 34px; flex: 0 0 34px; border-radius: 9px; display: grid; place-items: center; background: linear-gradient(145deg, #4d9bff, #1f6feb); font-weight: 700; color: #fff; box-shadow: 0 5px 14px rgba(31,111,235,.4); }
.brand-text { display: flex; flex-direction: column; gap: 3px; overflow: hidden; white-space: nowrap; }
.brand-text b { font-size: 14px; letter-spacing: .5px; }
.brand-text span { font-size: 11px; color: #7f91ac; }
.menu-scroll { flex: 1; padding: 12px 10px; }
.menu-group { margin-bottom: 14px; }
.group-name { font-size: 10px; letter-spacing: 1.6px; color: #63758f; padding: 6px 12px; }
.menu-item { height: 40px; display: flex; align-items: center; gap: 11px; padding: 0 12px; margin: 2px 0; border-radius: 7px; cursor: pointer; color: #aab8cc; font-size: 13.5px; white-space: nowrap; transition: .18s; }
.menu-item:hover { background: rgba(255,255,255,.055); color: #fff; }
.menu-item.active { background: linear-gradient(90deg, rgba(31,111,235,.9), rgba(31,111,235,.62)); color: #fff; box-shadow: 0 6px 16px rgba(31,111,235,.3); }
.sidebar-foot { padding: 14px 18px; border-top: 1px solid rgba(255,255,255,.06); }
.tiny { font-size: 10px; color: #5c6d86; line-height: 1.9; }
.main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.topbar { height: 64px; flex: 0 0 64px; background: rgba(255,255,255,.86); backdrop-filter: blur(10px); border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between; padding: 0 22px; gap: 14px; }
.top-left, .top-right { display: flex; align-items: center; gap: 14px; }
.fold { font-size: 19px; cursor: pointer; color: #596273; }
.tenant-select { width: 240px; }
.tenant-badge { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: #445; background: #eef3fb; padding: 5px 10px; border-radius: 99px; }
.bell { cursor: pointer; display: flex; align-items: center; color: #596273; margin-top: 3px; }
.avatar-wrap { display: flex; align-items: center; gap: 9px; cursor: pointer; }
.avatar { width: 34px; height: 34px; border-radius: 50%; background: linear-gradient(145deg,#4d9bff,#1f6feb); color: #fff; display: grid; place-items: center; font-weight: 600; }
.who { display: flex; flex-direction: column; line-height: 1.25; }
.who b { font-size: 13px; }
.who span { font-size: 11px; color: var(--muted); }
.content { flex: 1; overflow: auto; }
.notice-head { display: flex; justify-content: space-between; align-items: center; font-weight: 600; margin-bottom: 8px; }
.notice-empty { font-size: 12px; color: #98a2b3; padding: 18px 0; text-align: center; }
.notice-item { padding: 9px 4px; border-bottom: 1px dashed #eef1f6; cursor: pointer; }
.notice-item b { font-size: 13px; }
.notice-item p { margin: 3px 0; font-size: 12px; color: #667085; line-height: 1.6; }
.notice-item span { font-size: 10px; color: #a2aab8; }
.fade-enter-active, .fade-leave-active { transition: opacity .18s, transform .18s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(4px); }
@media (max-width: 900px) {
  .sidebar { width: 68px; flex-basis: 68px; }
  .brand-text, .who, .tenant-badge { display: none; }
  .menu-item span, .group-name, .sidebar-foot { display: none; }
  .topbar { padding: 0 12px; }
  .tenant-select { width: 150px; }
}
</style>
