<template>
  <div class="login-wrap">
    <section class="hero">
      <div class="brand"><span class="logo">云</span> 云宿 SaaS</div>
      <h1>让每一间房<br />都被认真经营</h1>
      <p>房态、预订、入住、房务、结算、会员与多端渠道，<br />一套系统贯穿住宿经营全流程。</p>
      <div class="hero-points">
        <div><b>多租户隔离</b><span>门店数据互不可见</span></div>
        <div><b>公安实名</b><span>入住登记合规留痕</span></div>
        <div><b>多端互通</b><span>小程序 / H5 / App</span></div>
      </div>
      <div class="glow g1"></div>
      <div class="glow g2"></div>
    </section>

    <section class="panel">
      <div class="panel-inner">
        <h2>登录运营后台</h2>
        <p class="tip">平台端与租户端共用入口，按账号自动分流</p>
        <el-segmented v-model="mode" :options="modeOptions" block />
        <el-form :model="form" @submit.prevent="submit" class="form">
          <el-form-item v-if="mode === 'tenant'">
            <el-input v-model="form.tenantCode" size="large" placeholder="租户编码，如 YQ-HZ">
              <template #prefix><el-icon><OfficeBuilding /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.username" size="large" placeholder="账号">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="密码" @keyup.enter="submit">
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">
            进入控制台
          </el-button>
        </el-form>

        <div class="demo">
          <div class="demo-title">演示账号</div>
          <div class="demo-row">
            <span @click="fill('platform')">平台超管 admin / admin123</span>
            <span @click="fill('tenant')">租户 admin / admin123 · YQ-HZ</span>
          </div>
          <div class="demo-row">
            <span @click="fill('front')">前台 front / front123</span>
            <span @click="fill('house')">保洁 house / house123</span>
          </div>
        </div>
        <p class="foot">登录即代表同意《用户协议》与《隐私政策》</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const mode = ref<'tenant' | 'platform'>('tenant')
const modeOptions = [
  { label: '租户账号', value: 'tenant' },
  { label: '平台超管', value: 'platform' }
]
const loading = ref(false)
const form = reactive({ tenantCode: 'YQ-HZ', username: 'admin', password: 'admin123' })

function fill(type: string) {
  mode.value = 'tenant'
  form.tenantCode = 'YQ-HZ'
  if (type === 'platform') {
    mode.value = 'platform'
    form.username = 'admin'
    form.password = 'admin123'
  } else if (type === 'front') {
    form.username = 'front'
    form.password = 'front123'
  } else if (type === 'house') {
    form.username = 'house'
    form.password = 'house123'
  } else {
    form.username = 'admin'
    form.password = 'admin123'
  }
}

async function submit() {
  if (!form.username || !form.password) return ElMessage.warning('请输入账号和密码')
  if (mode.value === 'tenant' && !form.tenantCode) return ElMessage.warning('请输入租户编码')
  loading.value = true
  try {
    const data: any = await auth.login({
      username: form.username,
      password: form.password,
      tenantCode: mode.value === 'platform' ? '' : form.tenantCode.trim().toUpperCase()
    })
    ElMessage.success(`欢迎回来，${data.realName || data.username}`)
    const redirect = route.query.redirect as string
    if (redirect) router.replace(redirect)
    else router.replace(data.platform ? '/platform' : '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap { min-height: 100%; display: grid; grid-template-columns: 1.15fr .85fr; background: #0d1728; }
.hero { position: relative; overflow: hidden; padding: 56px 64px; color: #fff; background: radial-gradient(circle at 20% 15%, #1d4f9c 0, #0d1728 55%); display: flex; flex-direction: column; }
.brand { display: flex; align-items: center; gap: 10px; font-weight: 700; letter-spacing: 1px; font-size: 17px; opacity: .95; }
.logo { width: 34px; height: 34px; display: grid; place-items: center; border-radius: 9px; background: linear-gradient(145deg, #4d9bff, #1f6feb); font-size: 18px; box-shadow: 0 6px 18px rgba(31,111,235,.45); }
.hero h1 { margin: 70px 0 20px; font-size: clamp(34px, 4vw, 54px); line-height: 1.22; letter-spacing: -1px; }
.hero p { color: #a9bad3; line-height: 1.9; font-size: 15px; }
.hero-points { display: flex; gap: 40px; margin-top: auto; padding-top: 50px; }
.hero-points div { display: flex; flex-direction: column; gap: 6px; }
.hero-points b { font-size: 15px; }
.hero-points span { font-size: 12px; color: #8194b1; }
.glow { position: absolute; border-radius: 50%; filter: blur(2px); opacity: .55; }
.g1 { width: 320px; height: 320px; right: -100px; top: -80px; background: radial-gradient(circle, rgba(64,143,255,.5), transparent 65%); }
.g2 { width: 420px; height: 420px; left: -160px; bottom: -200px; background: radial-gradient(circle, rgba(25,196,168,.3), transparent 65%); }
.panel { background: #f7f9fd; display: grid; place-items: center; padding: 40px; }
.panel-inner { width: 100%; max-width: 380px; }
h2 { margin: 0 0 6px; font-size: 26px; letter-spacing: -.4px; }
.tip { color: #7b8496; font-size: 13px; margin: 0 0 22px; }
.form { margin-top: 22px; }
.submit { width: 100%; height: 44px; margin-top: 4px; font-weight: 600; letter-spacing: 1px; }
.demo { margin-top: 26px; border-top: 1px dashed #dde3ee; padding-top: 18px; }
.demo-title { font-size: 12px; color: #98a2b3; margin-bottom: 8px; }
.demo-row { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 8px; }
.demo-row span { cursor: pointer; font-size: 12px; color: #3f6fb5; background: #eaf1fd; padding: 5px 9px; border-radius: 5px; transition: .2s; }
.demo-row span:hover { background: #dbe8fc; }
.foot { margin-top: 22px; font-size: 11px; color: #a2aab8; text-align: center; }
@media (max-width: 900px) {
  .login-wrap { grid-template-columns: 1fr; }
  .hero { display: none; }
  .panel { min-height: 100vh; }
}
</style>
