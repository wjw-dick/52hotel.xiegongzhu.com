<template>
  <div class="page">
    <div class="head-row">
      <div>
        <h2 class="page-title">平台总览</h2>
        <p class="page-sub">全平台租户、房源、订单与营收实时汇总</p>
      </div>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">租户数</div>
        <div class="stat-value">{{ data.tenantCount || 0 }}<span class="stat-unit">家</span></div>
        <div class="stat-foot">正常经营 {{ data.activeTenantCount || 0 }} 家</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">房源 / 出租率</div>
        <div class="stat-value">{{ data.roomCount || 0 }}<span class="stat-unit">间 · {{ data.occupancy || 0 }}%</span></div>
        <div class="stat-foot">在住 {{ data.occupied || 0 }} 间</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">累计订单</div>
        <div class="stat-value">{{ data.orderCount || 0 }}<span class="stat-unit">单</span></div>
        <div class="stat-foot">今日预抵 {{ data.todayOrders || 0 }} 单</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">累计营收</div>
        <div class="stat-value">¥{{ money(data.revenue) }}</div>
        <div class="stat-foot">本月 ¥{{ money(data.monthRevenue) }}</div>
      </div>
    </div>

    <el-alert
      v-if="(data.alerts || []).length"
      type="warning" show-icon :closable="false" style="margin-top:16px"
      :title="`${data.alerts.length} 家租户将在 30 天内到期，请及时跟进续费`"
    >
      <div style="margin-top:6px">
        <el-tag v-for="a in data.alerts" :key="a.tenantId" type="warning" effect="plain" style="margin:0 8px 6px 0">
          {{ a.name }} · {{ a.expireTime }}（{{ a.days }} 天）
        </el-tag>
      </div>
    </el-alert>

    <div class="split" style="margin-top:16px">
      <div class="card">
        <div class="section-title">近 7 日平台交易趋势</div>
        <ChartBox :option="trendOption" :height="300" />
      </div>
      <div class="card">
        <div class="section-title">租户营收分布</div>
        <ChartBox :option="tenantOption" :height="300" />
      </div>
    </div>

    <div class="card" style="margin-top:16px">
      <div class="section-title">租户经营看板</div>
      <el-table :data="data.tenants || []" size="small">
        <el-table-column prop="code" label="编码" width="100" />
        <el-table-column prop="name" label="租户" min-width="160" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rooms" label="房间" width="70" align="center" />
        <el-table-column prop="occupancy" label="出租率" width="90" align="center">
          <template #default="{ row }">{{ row.occupancy }}%</template>
        </el-table-column>
        <el-table-column prop="orders" label="订单" width="70" align="center" />
        <el-table-column prop="members" label="会员" width="70" align="center" />
        <el-table-column label="营收" width="120" align="right">
          <template #default="{ row }">¥{{ money(row.revenue) }}</template>
        </el-table-column>
        <el-table-column prop="expireTime" label="到期日" width="110" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click="drill(row)">下钻</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '@/api/http'
import ChartBox from '@/components/ChartBox.vue'
import { useAuthStore } from '@/stores/auth'
import { money } from '@/utils/format'

const router = useRouter()
const auth = useAuthStore()
const data = ref<any>({})

const trendOption = computed(() => {
  const trend = data.value.trend || []
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['营收', '订单'], right: 0 },
    grid: { left: 8, right: 12, bottom: 6, top: 40, containLabel: true },
    xAxis: { type: 'category', data: trend.map((x: any) => x.date.slice(5)) },
    yAxis: [{ type: 'value', name: '营收' }, { type: 'value', name: '订单', splitLine: { show: false } }],
    series: [
      { name: '营收', type: 'bar', barWidth: 18, data: trend.map((x: any) => Number(x.revenue || 0)), itemStyle: { color: '#4d9bff', borderRadius: [4, 4, 0, 0] } },
      { name: '订单', type: 'line', yAxisIndex: 1, smooth: true, data: trend.map((x: any) => x.orders || 0), itemStyle: { color: '#2eb67d' } }
    ]
  }
})

const tenantOption = computed(() => {
  const rows = data.value.tenants || []
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, type: 'scroll' },
    series: [{
      type: 'pie', radius: ['40%', '66%'], center: ['50%', '44%'],
      data: rows.map((r: any) => ({ name: r.name, value: Number(r.revenue || 0) }))
    }]
  }
})

function statusText(s: string) {
  return ({ TRIAL: '试用', NORMAL: '正常', DISABLED: '停用' } as Record<string, string>)[s] || s
}
function statusType(s: string) {
  return ({ TRIAL: 'warning', NORMAL: 'success', DISABLED: 'danger' } as Record<string, string>)[s] || 'info'
}

function drill(row: any) {
  auth.setDrillTenant(row.tenantId, row.name)
  ElMessage.success(`已切换至 ${row.name}（只读下钻）`)
  router.push('/dashboard')
}

async function load() {
  const res: any = await http.get('/platform/dashboard')
  data.value = res.data || {}
}

onMounted(load)
</script>

<style scoped>
.head-row { display: flex; justify-content: space-between; align-items: flex-start; }
</style>
