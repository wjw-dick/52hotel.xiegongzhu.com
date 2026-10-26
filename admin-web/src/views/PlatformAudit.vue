<template>
  <div class="page">
    <h2 class="page-title">平台审计</h2>
    <p class="page-sub">全平台操作留痕，支持按租户与关键字检索</p>

    <div class="card">
      <div class="toolbar">
        <el-select v-model="query.tenantId" placeholder="全部租户" clearable filterable style="width:220px" @change="load(1)">
          <el-option v-for="t in tenants" :key="t.id" :label="`${t.name} · ${t.code}`" :value="t.id" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="账号 / 动作" clearable style="width:220px" @keyup.enter="load(1)" />
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" size="small">
        <el-table-column prop="createTime" label="时间" width="170">
          <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="tenantId" label="租户ID" width="80" align="center" />
        <el-table-column prop="username" label="操作人" width="110" />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="action" label="动作" min-width="220" show-overflow-tooltip />
        <el-table-column prop="target" label="目标" min-width="220" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="detail" label="结果" width="100" />
      </el-table>
      <el-pagination
        style="margin-top:16px;justify-content:flex-end"
        layout="total, prev, pager, next, sizes"
        :total="total" v-model:current-page="query.page" v-model:page-size="query.size"
        @current-change="load()" @size-change="load(1)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import http from '@/api/http'
import { dateTime } from '@/utils/format'

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const tenants = ref<any[]>([])
const query = reactive<any>({ page: 1, size: 20, keyword: '', tenantId: undefined })

async function load(page?: number) {
  if (page) query.page = page
  loading.value = true
  try {
    const res: any = await http.get('/platform/audits', { params: query })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function reset() {
  query.keyword = ''
  query.tenantId = undefined
  load(1)
}

onMounted(async () => {
  try {
    const res: any = await http.get('/platform/tenants/options')
    tenants.value = res.data || []
  } catch { /* ignore */ }
  load()
})
</script>
