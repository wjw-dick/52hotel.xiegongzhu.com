<template>
  <div class="page">
    <h2 class="page-title">用户与角色</h2>
    <p class="page-sub">租户员工账号、角色分配与密码重置</p>

    <div class="card">
      <div class="toolbar">
        <span class="muted">当前租户用户数受套餐配额限制</span>
        <div class="grow"></div>
        <el-button type="primary" :icon="Plus" @click="openUser()">新增用户</el-button>
      </div>
      <el-table :data="users">
        <el-table-column prop="username" label="账号" width="130" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="角色" width="170">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.roleName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所属门店" min-width="160">
          <template #default="{ row }">{{ storeName(row.storeId) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210">
          <template #default="{ row }">
            <el-button link type="primary" @click="openUser(row)">编辑</el-button>
            <el-button link type="warning" @click="resetPassword(row)">重置密码</el-button>
            <el-button link type="danger" @click="removeUser(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card" style="margin-top:16px">
      <div class="section-title">角色权限说明</div>
      <el-table :data="roles" size="small">
        <el-table-column prop="code" label="角色编码" width="170" />
        <el-table-column prop="name" label="角色名称" width="160" />
        <el-table-column prop="permissions" label="权限范围" min-width="260" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑用户' : '新增用户'" width="540px">
      <el-form :model="form" label-width="86px" class="form-grid">
        <el-form-item label="账号" required>
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item v-if="!form.id" label="初始密码" required>
          <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="form.roleCode" style="width:100%">
            <el-option v-for="r in roles" :key="r.code" :label="r.name" :value="r.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属门店">
          <el-select v-model="form.storeId" clearable style="width:100%">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
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
import { dateTime } from '@/utils/format'

const users = ref<any[]>([])
const roles = ref<any[]>([])
const stores = ref<any[]>([])
const dialog = ref(false)
const form = reactive<any>({})

function storeName(id?: number) {
  return stores.value.find(s => s.id === id)?.name || '全部门店'
}

async function load() {
  const [u, r, s]: any[] = await Promise.all([
    http.get('/admin/users'), http.get('/admin/roles'), http.get('/admin/stores')
  ])
  users.value = u.data || []
  roles.value = r.data || []
  stores.value = s.data || []
}

function openUser(row?: any) {
  Object.assign(form, {
    id: undefined, username: '', password: '', realName: '', phone: '',
    roleCode: 'FRONT_DESK', storeId: undefined, status: 1
  })
  if (row) Object.assign(form, row)
  dialog.value = true
}

async function save() {
  if (!form.username) return ElMessage.warning('请填写账号')
  if (!form.id && (!form.password || form.password.length < 6)) return ElMessage.warning('初始密码至少 6 位')
  if (form.id) await http.put(`/admin/users/${form.id}`, form)
  else await http.post('/admin/users', form)
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}

async function resetPassword(row: any) {
  const { value } = await ElMessageBox.prompt(`为「${row.username}」设置新密码`, '重置密码', {
    inputPlaceholder: '至少 6 位，默认 123456', confirmButtonText: '确认重置', cancelButtonText: '取消'
  })
  const pwd = value || '123456'
  if (pwd.length < 6) return ElMessage.warning('密码至少 6 位')
  await http.post(`/admin/users/${row.id}/reset-password`, { password: pwd })
  ElMessage.success('密码已重置')
}

async function removeUser(row: any) {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', { type: 'warning' })
  await http.delete(`/admin/users/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
