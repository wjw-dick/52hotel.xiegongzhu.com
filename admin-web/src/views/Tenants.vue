<template>
  <div class="page">
    <h2 class="page-title">租户管理</h2>
    <p class="page-sub">SaaS 租户开通、套餐配额、状态与续费管理</p>

    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="租户名称 / 编码" clearable style="width:220px" @keyup.enter="load(1)" />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width:130px">
          <el-option label="试用" value="TRIAL" />
          <el-option label="正常" value="NORMAL" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <div class="grow"></div>
        <el-button type="primary" :icon="Plus" @click="openCreate">开通租户</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" size="default">
        <el-table-column prop="code" label="租户编码" width="110" />
        <el-table-column prop="name" label="租户名称" min-width="170" />
        <el-table-column prop="type" label="类型" width="90">
          <template #default="{ row }">{{ ({ HOTEL: '酒店', HOMESTAY: '民宿', HOSTEL: '客栈' } as any)[row.type] || row.type }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contact" label="联系人" width="90" />
        <el-table-column prop="phone" label="联系电话" width="125" />
        <el-table-column label="配额（人/房）" width="120" align="center">
          <template #default="{ row }">{{ row.userCount }}/{{ row.maxUsers }} · {{ row.roomCount }}/{{ row.maxRooms }}</template>
        </el-table-column>
        <el-table-column prop="orderCount" label="订单" width="70" align="center" />
        <el-table-column prop="memberCount" label="会员" width="70" align="center" />
        <el-table-column label="到期日" width="130">
          <template #default="{ row }">
            <span :style="{ color: row.expiring ? '#d33' : undefined }">{{ row.expireTime }}</span>
            <el-tag v-if="row.expiring" size="small" type="danger" effect="plain" style="margin-left:4px">临期</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openRenew(row)">续费</el-button>
            <el-button link :type="row.status === 'DISABLED' ? 'success' : 'danger'" @click="toggle(row)">
              {{ row.status === 'DISABLED' ? '启用' : '停用' }}
            </el-button>
            <el-button link type="primary" @click="drill(row)">下钻</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top:16px;justify-content:flex-end"
        layout="total, prev, pager, next, sizes"
        :total="total" v-model:current-page="query.page" v-model:page-size="query.size"
        @current-change="load()" @size-change="load(1)"
      />
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑租户' : '开通租户'" width="640px">
      <el-form :model="form" label-width="92px" class="form-grid">
        <el-form-item label="租户编码" required>
          <el-input v-model="form.code" :disabled="!!form.id" placeholder="如 YQ-HZ" />
        </el-form-item>
        <el-form-item label="租户名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="业态类型">
          <el-select v-model="form.type" style="width:100%">
            <el-option label="酒店" value="HOTEL" />
            <el-option label="民宿" value="HOMESTAY" />
            <el-option label="客栈" value="HOSTEL" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contact" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="到期日">
          <el-date-picker v-model="form.expireTime" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="用户上限"><el-input-number v-model="form.maxUsers" :min="1" :max="9999" style="width:100%" /></el-form-item>
        <el-form-item label="房间上限"><el-input-number v-model="form.maxRooms" :min="1" :max="99999" style="width:100%" /></el-form-item>
        <el-form-item v-if="!form.id" label="管理员账号"><el-input v-model="form.adminUsername" placeholder="默认 admin" /></el-form-item>
        <el-form-item v-if="!form.id" label="初始密码"><el-input v-model="form.adminPassword" placeholder="默认 admin123" /></el-form-item>
        <el-form-item label="备注" class="full"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="renewVisible" title="租户续费" width="380px">
      <p class="muted" style="margin-top:0">为 <b>{{ renewRow?.name }}</b> 延长有效期（当前到期 {{ renewRow?.expireTime }}）</p>
      <el-radio-group v-model="renewMonths">
        <el-radio-button :value="1">1 个月</el-radio-button>
        <el-radio-button :value="3">3 个月</el-radio-button>
        <el-radio-button :value="6">6 个月</el-radio-button>
        <el-radio-button :value="12">12 个月</el-radio-button>
      </el-radio-group>
      <template #footer>
        <el-button @click="renewVisible = false">取消</el-button>
        <el-button type="primary" @click="doRenew">确认续费</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const dialog = ref(false)
const renewVisible = ref(false)
const renewMonths = ref(12)
const renewRow = ref<any>(null)
const query = reactive({ page: 1, size: 10, keyword: '', status: '' })
const emptyForm = () => ({
  id: undefined as number | undefined, code: '', name: '', type: 'HOMESTAY', contact: '', phone: '',
  openTime: '', expireTime: '', maxUsers: 20, maxRooms: 100, remark: '', adminUsername: 'admin', adminPassword: 'admin123'
})
const form = reactive<any>(emptyForm())

function statusText(s: string) {
  return ({ TRIAL: '试用', NORMAL: '正常', DISABLED: '停用' } as Record<string, string>)[s] || s
}
function statusType(s: string) {
  return ({ TRIAL: 'warning', NORMAL: 'success', DISABLED: 'danger' } as Record<string, string>)[s] || 'info'
}

async function load(page?: number) {
  if (page) query.page = page
  loading.value = true
  try {
    const res: any = await http.get('/platform/tenants', { params: query })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function reset() {
  query.keyword = ''
  query.status = ''
  load(1)
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialog.value = true
}

function openEdit(row: any) {
  Object.assign(form, emptyForm(), row)
  dialog.value = true
}

async function save() {
  if (!form.code || !form.name) return ElMessage.warning('请填写租户编码与名称')
  saving.value = true
  try {
    if (form.id) await http.put(`/platform/tenants/${form.id}`, form)
    else await http.post('/platform/tenants', form)
    ElMessage.success('保存成功')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggle(row: any) {
  const target = row.status === 'DISABLED' ? 'NORMAL' : 'DISABLED'
  await ElMessageBox.confirm(`确认${target === 'DISABLED' ? '停用' : '启用'}租户「${row.name}」？`, '提示', { type: 'warning' })
  await http.post(`/platform/tenants/${row.id}/status`, { status: target })
  ElMessage.success('操作成功')
  load()
}

function openRenew(row: any) {
  renewRow.value = row
  renewMonths.value = 12
  renewVisible.value = true
}

async function doRenew() {
  await http.post(`/platform/tenants/${renewRow.value.id}/renew`, { months: renewMonths.value })
  ElMessage.success('续费成功')
  renewVisible.value = false
  load()
}

function drill(row: any) {
  auth.setDrillTenant(row.id, row.name)
  ElMessage.success(`已切换至 ${row.name}（只读下钻）`)
  router.push('/dashboard')
}

onMounted(() => load())
</script>
