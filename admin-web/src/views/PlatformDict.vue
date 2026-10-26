<template>
  <div class="page">
    <h2 class="page-title">全局字典</h2>
    <p class="page-sub">平台级基础字典，所有租户共享；租户可在系统配置中扩展自己的字典项</p>

    <div class="card">
      <div class="toolbar">
        <el-select v-model="type" placeholder="全部类型" clearable style="width:180px" @change="load">
          <el-option v-for="t in types" :key="t" :label="t" :value="t" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <div class="grow"></div>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增字典</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" size="default">
        <el-table-column prop="type" label="字典类型" width="160" />
        <el-table-column prop="code" label="编码" width="180" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑字典' : '新增字典'" width="480px">
      <el-form :model="form" label-width="86px">
        <el-form-item label="字典类型" required><el-input v-model="form.type" placeholder="如 room_type" /></el-form-item>
        <el-form-item label="编码" required><el-input v-model="form.code" placeholder="如 SUITE" /></el-form-item>
        <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'

const loading = ref(false)
const rows = ref<any[]>([])
const types = ref<string[]>([])
const type = ref('')
const dialog = ref(false)
const form = reactive<any>({ id: undefined, type: '', code: '', name: '', sort: 0, status: 1 })

async function load() {
  loading.value = true
  try {
    const [all, filtered]: any[] = await Promise.all([
      http.get('/platform/dicts'),
      http.get('/platform/dicts', { params: type.value ? { type: type.value } : {} })
    ])
    types.value = [...new Set((all.data || []).map((x: any) => x.type))] as string[]
    rows.value = filtered.data || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: undefined, type: type.value || '', code: '', name: '', sort: 0, status: 1 })
  dialog.value = true
}

function openEdit(row: any) {
  Object.assign(form, row)
  dialog.value = true
}

async function save() {
  if (!form.type || !form.code || !form.name) return ElMessage.warning('请填写完整')
  if (form.id) await http.put(`/platform/dicts/${form.id}`, form)
  else await http.post('/platform/dicts', form)
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除字典「${row.name}」？`, '提示', { type: 'warning' })
  await http.delete(`/platform/dicts/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
