<template>
  <div>
    <section class="page-hero">
      <div style="max-width:1080px;margin:0 auto">
        <h1>订单查询</h1>
        <p>输入订单号与预订手机号，查看订单状态与入住信息</p>
      </div>
    </section>

    <div class="page-body">
      <div class="panel" style="max-width:620px;margin:0 auto">
        <h2>查询我的订单</h2>
        <div class="form-grid">
          <div class="form-field full">
            <label>订单号</label>
            <input v-model="form.orderNo" placeholder="如 YQ-HZ-2026-0001" />
          </div>
          <div class="form-field full">
            <label>预订手机号</label>
            <input v-model="form.phone" inputmode="tel" placeholder="下单时填写的手机号" />
          </div>
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <button class="btn block" style="margin-top:16px" :disabled="loading" @click="query">{{ loading ? '查询中…' : '查询订单' }}</button>
        <p class="hint" style="margin-top:12px">也可以前往 <router-link to="/member" style="color:var(--brass)">会员中心</router-link>，使用微信登录后查看全部订单。</p>
      </div>

      <div v-if="order" class="panel" style="max-width:760px;margin:22px auto 0">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:12px;flex-wrap:wrap">
          <div>
            <h2 style="margin:0">{{ order.orderNo }}</h2>
            <p class="hint" style="margin:6px 0 0">{{ order.storeName || '云栖民宿' }} · {{ order.roomTypeName }}</p>
          </div>
          <span class="tag" :class="{ warn: order.status === 'PENDING' || order.status === 'CONFIRMED', muted: order.status === 'CANCELLED' }">
            {{ statusMap[order.status] || order.status }}
          </span>
        </div>
        <div class="summary-line" style="color:#4e5a53;border-color:var(--line)"><span>入住人</span><b>{{ order.guestName }}</b></div>
        <div class="summary-line" style="color:#4e5a53;border-color:var(--line)"><span>入住 / 离店</span><b>{{ order.checkIn }} → {{ order.checkOut }}（{{ order.nights }} 晚）</b></div>
        <div class="summary-line" style="color:#4e5a53;border-color:var(--line)"><span>房间</span><b>{{ order.roomNo || '待分配' }}</b></div>
        <div class="summary-line" style="color:#4e5a53;border-color:var(--line)"><span>金额</span><b>¥{{ money(order.amount) }}</b></div>
        <div class="summary-line" style="color:#4e5a53;border-color:var(--line)"><span>备注</span><b>{{ order.remark || '-' }}</b></div>
        <div style="display:flex;gap:10px;margin-top:20px;flex-wrap:wrap">
          <a class="btn outline" :href="`tel:${store.home?.config?.service_phone || '400-800-8888'}`">联系门店</a>
          <router-link class="btn" to="/booking">再订一间</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '@/api/http'
import { loadHome, store } from '@/stores/portal'
import { money, statusMap } from '@/utils'

const route = useRoute()
const loading = ref(false)
const error = ref('')
const order = ref<any>(null)
const form = reactive({ orderNo: String(route.query.orderNo || ''), phone: String(route.query.phone || '') })

async function query() {
  error.value = ''
  order.value = null
  if (!form.orderNo || !form.phone) return error.value = '请填写订单号与手机号'
  loading.value = true
  try {
    const res: any = await http.get(`/public/${store.tenantCode}/orders/query`, { params: form })
    order.value = res.data?.order
  } catch (e: any) {
    error.value = e.message || '未查询到订单'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadHome()
  if (form.orderNo && form.phone) query()
})
</script>
