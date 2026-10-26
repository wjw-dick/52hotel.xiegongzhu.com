<template>
  <div class="page">
    <h2 class="page-title">门店与房源</h2>
    <p class="page-sub">门店档案、渠道库存同步与餐饮挂账管理</p>

    <el-tabs v-model="tab" class="card">
      <el-tab-pane label="门店管理" name="stores">
        <div class="toolbar">
          <span class="muted">共 {{ stores.length }} 家门店</span>
          <div class="grow"></div>
          <el-button type="primary" :icon="Plus" @click="openCreate">新增门店</el-button>
        </div>
        <el-table v-loading="loading" :data="stores">
          <el-table-column prop="code" label="门店编码" width="130" />
          <el-table-column prop="name" label="门店名称" min-width="160" />
          <el-table-column prop="city" label="城市" width="90" />
          <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
          <el-table-column prop="phone" label="电话" width="140" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '营业中' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="经营快照" width="190">
            <template #default="{ row }">
              <span v-if="summary[row.id]" class="muted">房 {{ summary[row.id].roomCount }} · 住 {{ summary[row.id].occupied }} · 脏 {{ summary[row.id].dirty }}</span>
              <el-button v-else link type="primary" @click="loadSummary(row)">查看</el-button>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="OTA 渠道" name="channels">
        <div class="toolbar">
          <span class="muted">渠道房态/房价同步为演示适配器，可在正式环境替换为携程/美团开放平台接口</span>
          <div class="grow"></div>
          <el-button type="primary" :icon="Plus" @click="channelDialog = true">接入渠道</el-button>
        </div>
        <el-table :data="channels">
          <el-table-column prop="name" label="渠道" width="140" />
          <el-table-column prop="code" label="编码" width="120" />
          <el-table-column label="库存同步" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.syncStock === 1 ? 'success' : 'info'">{{ row.syncStock === 1 ? '开启' : '关闭' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastSync" label="最近同步" width="180">
            <template #default="{ row }">{{ dateTime(row.lastSync) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" :loading="syncing === row.id" @click="sync(row)">立即同步</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="餐饮挂账" name="dining">
        <div class="toolbar">
          <span class="muted">餐饮消费可挂入在住房间账单，退房时统一结算</span>
          <div class="grow"></div>
          <el-button type="primary" :icon="Plus" @click="diningDialog = true">录入消费</el-button>
        </div>
        <el-table :data="dining">
          <el-table-column prop="roomNo" label="房间" width="100" />
          <el-table-column prop="guestName" label="客人" width="120" />
          <el-table-column prop="item" label="消费项目" min-width="180" />
          <el-table-column label="金额" width="120" align="right">
            <template #default="{ row }">¥{{ money(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'CHARGED' ? 'success' : 'warning'">
                {{ row.status === 'CHARGED' ? '已挂账' : '待挂账' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="170">
            <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="110">
            <template #default="{ row }">
              <el-button v-if="row.status !== 'CHARGED'" link type="primary" @click="charge(row)">挂入房账</el-button>
              <span v-else class="muted">已入账</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialog" :title="form.id ? '编辑门店' : '新增门店'" width="560px">
      <el-form :model="form" label-width="86px" class="form-grid">
        <el-form-item label="门店编码" required><el-input v-model="form.code" placeholder="如 YQ-HZ-03" /></el-form-item>
        <el-form-item label="门店名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="所在城市"><el-input v-model="form.city" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="详细地址" class="full"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="门店简介" class="full"><el-input v-model="form.intro" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">营业中</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="channelDialog" title="接入 OTA 渠道" width="440px">
      <el-form :model="channelForm" label-width="80px">
        <el-form-item label="渠道名称" required><el-input v-model="channelForm.name" placeholder="如 携程" /></el-form-item>
        <el-form-item label="渠道编码" required><el-input v-model="channelForm.code" placeholder="如 CTRIP" /></el-form-item>
        <el-form-item label="库存同步">
          <el-switch v-model="channelForm.syncStock" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="channelDialog = false">取消</el-button>
        <el-button type="primary" @click="saveChannel">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="diningDialog" title="录入餐饮消费" width="460px">
      <el-form :model="diningForm" label-width="86px">
        <el-form-item label="房间" required>
          <el-select v-model="diningForm.roomId" filterable placeholder="选择房间" style="width:100%">
            <el-option v-for="r in occupiedRooms" :key="r.id" :label="`${r.roomNo} · ${r.roomTypeName}`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="客人"><el-input v-model="diningForm.guestName" /></el-form-item>
        <el-form-item label="消费项目" required><el-input v-model="diningForm.item" placeholder="如 晚餐套餐" /></el-form-item>
        <el-form-item label="金额" required><el-input-number v-model="diningForm.amount" :min="0" :precision="2" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="diningDialog = false">取消</el-button>
        <el-button type="primary" @click="saveDining">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import { dateTime, money } from '@/utils/format'

const tab = ref('stores')
const loading = ref(false)
const stores = ref<any[]>([])
const channels = ref<any[]>([])
const dining = ref<any[]>([])
const rooms = ref<any[]>([])
const summary = reactive<Record<number, any>>({})
const syncing = ref<number | null>(null)
const dialog = ref(false)
const channelDialog = ref(false)
const diningDialog = ref(false)
const form = reactive<any>({ id: undefined, code: '', name: '', city: '', address: '', phone: '', intro: '', status: 1 })
const channelForm = reactive<any>({ name: '', code: '', syncStock: 1 })
const diningForm = reactive<any>({ roomId: undefined, guestName: '', item: '', amount: 0 })

const occupiedRooms = computed(() => rooms.value.filter(r => r.status === 'OCCUPIED'))

async function loadStores() {
  loading.value = true
  try {
    const res: any = await http.get('/admin/stores')
    stores.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function loadSummary(row: any) {
  const res: any = await http.get(`/admin/stores/${row.id}/summary`)
  summary[row.id] = res.data
}

async function loadChannels() {
  const res: any = await http.get('/admin/enhance/channels')
  channels.value = res.data || []
}

async function loadDining() {
  const res: any = await http.get('/admin/enhance/dining')
  dining.value = res.data || []
}

async function loadRooms() {
  const res: any = await http.get('/admin/rooms')
  rooms.value = res.data || []
}

function openCreate() {
  Object.assign(form, { id: undefined, code: '', name: '', city: '', address: '', phone: '', intro: '', status: 1 })
  dialog.value = true
}

function openEdit(row: any) {
  Object.assign(form, row)
  dialog.value = true
}

async function save() {
  if (!form.code || !form.name) return ElMessage.warning('请填写门店编码与名称')
  if (form.id) await http.put(`/admin/stores/${form.id}`, form)
  else await http.post('/admin/stores', form)
  ElMessage.success('保存成功')
  dialog.value = false
  loadStores()
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除门店「${row.name}」？门店下需无房间`, '提示', { type: 'warning' })
  await http.delete(`/admin/stores/${row.id}`)
  ElMessage.success('已删除')
  loadStores()
}

async function sync(row: any) {
  syncing.value = row.id
  try {
    const res: any = await http.post(`/admin/enhance/channels/${row.id}/sync`)
    ElMessage.success(res.data?.message || '同步完成')
    loadChannels()
  } finally {
    syncing.value = null
  }
}

async function saveChannel() {
  if (!channelForm.name || !channelForm.code) return ElMessage.warning('请填写渠道名称与编码')
  await http.post('/admin/enhance/channels', channelForm)
  ElMessage.success('渠道已接入')
  channelDialog.value = false
  Object.assign(channelForm, { name: '', code: '', syncStock: 1 })
  loadChannels()
}

async function saveDining() {
  if (!diningForm.roomId || !diningForm.item) return ElMessage.warning('请选择房间并填写消费项目')
  await http.post('/admin/enhance/dining', diningForm)
  ElMessage.success('已录入，可挂入房账')
  diningDialog.value = false
  Object.assign(diningForm, { roomId: undefined, guestName: '', item: '', amount: 0 })
  loadDining()
}

async function charge(row: any) {
  await http.post(`/admin/enhance/dining/${row.id}/charge-room`)
  ElMessage.success('已挂入房间账单')
  loadDining()
}

watch(tab, (v) => {
  if (v === 'channels' && !channels.value.length) loadChannels()
  if (v === 'dining') { loadDining(); loadRooms() }
})

onMounted(loadStores)
</script>
