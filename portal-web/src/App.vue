<template>
  <div class="app">
    <header class="nav">
      <router-link to="/" class="brand">
        <span class="mark">云</span>
        <span class="brand-text">
          <b>{{ store.home?.config?.tenant_name || store.home?.tenant?.name || '云栖民宿' }}</b>
          <small>{{ store.home?.config?.slogan || '山水之间，安心住下' }}</small>
        </span>
      </router-link>
      <nav class="links">
        <router-link to="/">首页</router-link>
        <router-link to="/booking">在线预订</router-link>
        <router-link to="/query">订单查询</router-link>
        <router-link to="/member">会员中心</router-link>
      </nav>
      <div class="actions">
        <button class="lang" @click="toggleLang">{{ store.lang === 'zh-CN' ? 'EN' : '中文' }}</button>
        <a class="phone" :href="`tel:${store.home?.config?.service_phone || '400-800-8888'}`">
          {{ store.home?.config?.service_phone || '400-800-8888' }}
        </a>
      </div>
    </header>

    <main>
      <router-view />
    </main>

    <footer class="foot">
      <div class="foot-inner">
        <div>
          <b>{{ store.home?.config?.tenant_name || '云栖民宿' }}</b>
          <p>{{ store.home?.config?.slogan || '山水之间，安心住下' }}</p>
        </div>
        <div class="foot-links">
          <router-link to="/booking">在线预订</router-link>
          <router-link to="/query">订单查询</router-link>
          <router-link to="/member">会员中心</router-link>
          <a :href="`tel:${store.home?.config?.service_phone || ''}`">客服电话</a>
        </div>
      </div>
      <p class="copyright">© {{ year }} 云宿 SaaS · 民宿数字化运营平台 演示站点</p>
    </footer>

    <transition name="toast">
      <div v-if="store.notice" class="toast">{{ store.notice }}</div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { loadHome, store, switchLang } from '@/stores/portal'

const year = new Date().getFullYear()

function toggleLang() {
  switchLang(store.lang === 'zh-CN' ? 'en' : 'zh-CN')
}

onMounted(() => {
  loadHome().catch(() => { /* 页面内展示错误 */ })
})
</script>
