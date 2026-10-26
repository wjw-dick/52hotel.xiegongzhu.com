<template>
  <div class="page">
    <h2 class="page-title">报表中心</h2>
    <p class="page-sub">营收、出租率、客源结构与房型贡献分析</p>

    <div class="card">
      <div class="toolbar">
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" style="width:260px" />
        <el-button type="primary" @click="load">生成报表</el-button>
        <div class="grow"></div>
        <el-button :icon="Download" @click="exportCsv">导出 CSV</el-button>
      </div>
    </div>

    <div class="stat-grid" style="margin-top:16px">
      <div class="stat-card">
        <div class="stat-label">区间营收</div>
        <div class="stat-value">¥{{ money(data.revenue) }}</div>
        <div class="stat-foot">{{ data.startDate }} ~ {{ data.endDate }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">订单 / 间夜</div>
        <div class="stat-value">{{ data.orderCount || 0 }}<span class="stat-unit">/ {{ data.roomNights || 0 }} 间夜</span></div>
        <div class="stat-foot">会员 {{ data.memberCount || 0 }} 人</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">出租率</div>
        <div class="stat-value">{{ data.occupancy || 0 }}<span class="stat-unit">%</span></div>
        <div class="stat-foot">总房量 {{ data.roomCount || 0 }} 间</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">ADR / RevPAR</div>
        <div class="stat-value" style="font-size:22px">¥{{ money(data.adr) }} / ¥{{ money(data.revpar) }}</div>
        <div class="stat-foot">平均房价 / 每间可售房收入</div>
      </div>
    </div>

    <div class="split" style="margin-top:16px">
      <div class="card">
        <div class="section-title">营收与订单趋势</div>
        <ChartBox :option="trendOption" :height="310" />
      </div>
      <div class="card">
        <div class="section-title">客源结构</div>
        <ChartBox :option="sourceOption" :height="310" />
      </div>
    </div>

    <div class="split" style="margin-top:16px">
      <div class="card">
        <div class="section-title">房型贡献</div>
        <el-table :data="data.roomTypeStats || []" size="small">
          <el-table-column prop="roomTypeName" label="房型" min-width="140" />
          <el-table-column prop="orders" label="订单" width="80" align="center" />
          <el-table-column label="营收" width="120" align="right">
            <template #default="{ row }">¥{{ money(row.revenue) }}</template>
          </el-table-column>
          <el-table-column label="占比" width="100" align="right">
            <template #default="{ row }">{{ share(row.revenue) }}%</template>
          </el-table-column>
        </el-table>
      </div>
      <div class="card">
        <div class="section-title">门店表现</div>
        <el-table :data="data.storeStats || []" size="small">
          <el-table-column prop="storeName" label="门店" min-width="140" />
          <el-table-column prop="orders" label="订单" width="80" align="center" />
          <el-table-column label="营收" width="120" align="right">
            <template #default="{ row }">¥{{ money(row.revenue) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Download } from '@element-plus/icons-vue'
import http from '@/api/http'
import ChartBox from '@/components/ChartBox.vue'
import { money, monthStart, sourceMap, today } from '@/utils/format'

const range = ref<[string, string]>([monthStart(), today()])
const data = ref<any>({})

const trendOption = computed(() => {
  const trend = data.value.trend || []
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['营收', '订单'], right: 0 },
    grid: { left: 8, right: 12, bottom: 6, top: 40, containLabel: true },
    xAxis: { type: 'category', data: trend.map((x: any) => x.date.slice(5)), axisLabel: { interval: 'auto' } },
    yAxis: [{ type: 'value', name: '营收' }, { type: 'value', name: '订单', splitLine: { show: false } }],
    series: [
      { name: '营收', type: 'line', smooth: true, areaStyle: { opacity: .12 }, data: trend.map((x: any) => Number(x.revenue || 0)), itemStyle: { color: '#1f6feb' } },
      { name: '订单', type: 'bar', yAxisIndex: 1, barWidth: 10, data: trend.map((x: any) => x.orders || 0), itemStyle: { color: '#2eb67d', borderRadius: [3, 3, 0, 0] } }
    ]
  }
})

const sourceOption = computed(() => {
  const source = data.value.source || {}
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['42%', '68%'], center: ['50%', '44%'],
      label: { formatter: '{b}\n{c} 单' },
      data: Object.keys(source).map(k => ({ name: sourceMap[k] || k, value: source[k] }))
    }]
  }
})

function share(v: number) {
  const total = Number(data.value.revenue || 0)
  return total ? (Number(v || 0) * 100 / total).toFixed(1) : '0.0'
}

async function load() {
  if (!range.value?.length) return
  const res: any = await http.get('/admin/reports/summary', {
    params: { startDate: range.value[0], endDate: range.value[1] }
  })
  data.value = res.data || {}
}

async function exportCsv() {
  const blob: any = await http.get('/admin/reports/export', {
    params: { startDate: range.value?.[0], endDate: range.value?.[1] },
    responseType: 'blob'
  })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `report-${today()}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(load)
</script>
