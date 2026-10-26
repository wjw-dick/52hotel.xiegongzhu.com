<template>
  <div class="page">
    <div class="head-row">
      <div>
        <h2 class="page-title">经营概览</h2>
        <p class="page-sub">{{ data.tenantName || '当前租户' }} · 数据截至 {{ today }}，租户到期 {{ data.expireTime || '-' }}</p>
      </div>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">今日营收</div>
        <div class="stat-value">¥{{ money(data.todayRevenue) }}</div>
        <div class="stat-foot">本月累计 ¥{{ money(data.monthRevenue) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日预抵 / 在住</div>
        <div class="stat-value">{{ data.todayArrivals || 0 }}<span class="stat-unit">/ {{ data.inHouse || 0 }} 间</span></div>
        <div class="stat-foot">今日预离 {{ data.todayDepartures || 0 }} 间</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">出租率</div>
        <div class="stat-value">{{ data.occupancy || 0 }}<span class="stat-unit">%</span></div>
        <div class="stat-foot">在住 {{ data.occupied || 0 }} / 总房 {{ data.roomCount || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">会员 / 待处理</div>
        <div class="stat-value">{{ data.memberCount || 0 }}<span class="stat-unit">人</span></div>
        <div class="stat-foot">脏房 {{ data.dirty || 0 }} · 待办 {{ data.pendingTasks || 0 }}</div>
      </div>
    </div>

    <div class="split" style="margin-top:16px">
      <div class="card">
        <div class="section-title">近 7 日营收与订单</div>
        <ChartBox :option="trendOption" :height="300" />
      </div>
      <div class="card">
        <div class="section-title">实时房态</div>
        <ChartBox :option="statusOption" :height="300" />
      </div>
    </div>

    <div class="card" style="margin-top:16px">
      <div class="section-title">今日预抵名单</div>
      <el-table :data="data.arrivals || []" size="small">
        <el-table-column prop="orderNo" label="订单号" min-width="160" />
        <el-table-column prop="guestName" label="入住人" width="110" />
        <el-table-column prop="guestPhone" label="手机号" width="130" />
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }">¥{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="orderStatus[row.status]?.type as any">{{ orderStatus[row.status]?.text || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push({ path: '/front-desk', query: { orderId: row.id } })">办理入住</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!(data.arrivals || []).length" description="今日暂无预抵订单" :image-size="70" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import http from '@/api/http'
import ChartBox from '@/components/ChartBox.vue'
import { money, orderStatus, roomStatus, today } from '@/utils/format'

const router = useRouter()
const data = ref<any>({})

const trendOption = computed(() => {
  const trend = data.value.trend || []
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['营收', '订单'], right: 0, top: 0 },
    grid: { left: 8, right: 12, bottom: 6, top: 40, containLabel: true },
    xAxis: { type: 'category', data: trend.map((x: any) => x.date.slice(5)), axisLine: { lineStyle: { color: '#dfe4ed' } } },
    yAxis: [
      { type: 'value', name: '营收', splitLine: { lineStyle: { color: '#f0f3f8' } } },
      { type: 'value', name: '订单', splitLine: { show: false } }
    ],
    series: [
      { name: '营收', type: 'bar', barWidth: 18, data: trend.map((x: any) => Number(x.revenue || 0)), itemStyle: { color: '#4d9bff', borderRadius: [4, 4, 0, 0] } },
      { name: '订单', type: 'line', yAxisIndex: 1, smooth: true, data: trend.map((x: any) => x.orders || 0), itemStyle: { color: '#2eb67d' } }
    ]
  }
})

const statusOption = computed(() => {
  const sc = data.value.statusCount || {}
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['48%', '70%'], center: ['50%', '44%'], avoidLabelOverlap: true,
      label: { formatter: '{b}\n{c} 间' },
      data: Object.keys(sc).map((k: string) => ({ name: roomStatus[k]?.text || k, value: sc[k], itemStyle: { color: roomStatus[k]?.color } }))
    }]
  }
})

async function load() {
  const res: any = await http.get('/admin/dashboard')
  data.value = res.data || {}
}

onMounted(load)
</script>

<style scoped>
.head-row { display: flex; justify-content: space-between; align-items: flex-start; }
</style>
