<template>
  <div class="page">
    <h2 class="page-title">审计日志</h2>
    <p class="page-sub">租户内关键操作留痕，便于追溯与合规检查</p>

    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="账号 / 动作 / 目标" clearable style="width:240px" @keyup.enter="load(1)" />
        <el-input v-model="query.module" placeholder="模块，如 admin" clearable style="width:140px" @keyup.enter="load(1)" />
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" size="small">
        <el-table-column prop="createTime" label="时间" width="170">
          <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="操作人" width="110" />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="action" label="动作" min-width="230" show-overflow-tooltip />
        <el-table-column prop="target" label="目标" min-width="230" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="detail" label="结果" width="90" />
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
const query = reactive<any>({ page: 1, size: 20, keyword: '', module: '' })

async function load(page?: number) {
  if (page) query.page = page
  loading.value = true
  try {
    const res: any = await http.get('/admin/audits', { params: query })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function reset() {
  query.keyword = ''
  query.module = ''
  load(1)
}

onMounted(() => load())
</script>
