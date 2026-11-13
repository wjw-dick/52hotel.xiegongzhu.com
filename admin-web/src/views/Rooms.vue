<template>
  <div class="page">
    <h2 class="page-title">房型 / 房间 / 房价</h2>
    <p class="page-sub">维护门店房型、物理房间与每日房价日历</p>

    <el-tabs v-model="tab" class="card">
      <el-tab-pane label="房型管理" name="types">
        <div class="toolbar">
          <el-select v-model="typeStore" clearable placeholder="全部门店" style="width:180px" @change="loadTypes">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <div class="grow"></div>
          <el-button type="primary" :icon="Plus" @click="openType()">新增房型</el-button>
        </div>
        <el-table v-loading="loadingTypes" :data="types">
          <el-table-column label="封面" width="90">
            <template #default="{ row }">
              <img v-if="row.cover" :src="row.cover" class="cover-thumb" alt="封面" />
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="code" label="编码" width="130" />
          <el-table-column prop="name" label="房型名称" min-width="140" />
          <el-table-column label="所属门店" min-width="150">
            <template #default="{ row }">{{ storeName(row.storeId) }}</template>
          </el-table-column>
          <el-table-column prop="area" label="面积㎡" width="90" align="center" />
          <el-table-column prop="capacity" label="可住" width="70" align="center" />
          <el-table-column prop="bedType" label="床型" width="120" />
          <el-table-column label="门市价" width="110" align="right">
            <template #default="{ row }">¥{{ money(row.basePrice) }}</template>
          </el-table-column>
          <el-table-column label="设施" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <el-tag v-for="f in (row.facilities || '').split(',').filter(Boolean)" :key="f" size="small" effect="plain" style="margin:2px">{{ f }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '在售' : '下架' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openType(row)">编辑</el-button>
              <el-button link type="danger" @click="removeType(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="房间管理" name="rooms">
        <div class="toolbar">
          <el-select v-model="roomQuery.storeId" clearable placeholder="全部门店" style="width:160px" @change="loadRooms">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <el-select v-model="roomQuery.status" clearable placeholder="全部房态" style="width:130px" @change="loadRooms">
            <el-option v-for="(v, k) in roomStatus" :key="k" :label="v.text" :value="k" />
          </el-select>
          <el-input v-model="roomQuery.keyword" placeholder="房间号" clearable style="width:140px" @keyup.enter="loadRooms" />
          <el-button type="primary" @click="loadRooms">查询</el-button>
          <div class="grow"></div>
          <el-upload :show-file-list="false" accept=".csv" :http-request="importCsv">
            <el-button :icon="Upload">批量导入</el-button>
          </el-upload>
          <el-button :icon="Download" @click="exportCsv">导出 CSV</el-button>
          <el-button type="primary" :icon="Plus" @click="openRoom()">新增房间</el-button>
        </div>
        <el-table v-loading="loadingRooms" :data="rooms">
          <el-table-column prop="roomNo" label="房间号" width="100">
            <template #default="{ row }"><b>{{ row.roomNo }}</b></template>
          </el-table-column>
          <el-table-column prop="storeName" label="门店" min-width="150" />
          <el-table-column prop="roomTypeName" label="房型" min-width="130" />
          <el-table-column prop="floor" label="楼层" width="70" align="center" />
          <el-table-column label="房态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="roomStatus[row.status]?.type as any">{{ roomStatus[row.status]?.text || row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lockCode" label="门锁编号" width="140" />
          <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="230" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRoom(row)">编辑</el-button>
              <el-dropdown trigger="click" @command="(s: string) => changeStatus(row, s)">
                <el-button link type="primary">改房态</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-for="(v, k) in roomStatus" :key="k" :command="k" :disabled="k === row.status">{{ v.text }}</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button link type="primary" @click="showLogs(row)">房态记录</el-button>
              <el-button link type="danger" @click="removeRoom(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="房价日历" name="rates">
        <div class="toolbar">
          <el-select v-model="rateTypeId" placeholder="选择房型" filterable style="width:220px" @change="loadRates">
            <el-option v-for="t in types" :key="t.id" :label="`${t.name} · ${storeName(t.storeId)}`" :value="t.id" />
          </el-select>
          <el-date-picker v-model="rateMonth" type="month" value-format="YYYY-MM" style="width:160px" @change="loadRates" />
          <el-button @click="loadRates">刷新</el-button>
          <div class="grow"></div>
          <el-button type="primary" :icon="Plus" :disabled="!rateTypeId" @click="openBatch">批量调价</el-button>
        </div>
        <el-alert v-if="!rateTypeId" type="info" :closable="false" title="请选择房型查看每日房价" />
        <template v-else>
          <div class="rate-grid">
            <div v-for="d in rateDays" :key="d.date" class="rate-cell" :class="{ empty: !d.price }">
              <div class="d">{{ d.day }}</div>
              <div class="p">{{ d.price ? '¥' + money(d.price, 0) : '未设置' }}</div>
              <div class="muted">{{ d.week }}</div>
            </div>
          </div>
          <div style="margin-top:16px">
            <div class="section-title">调价记录</div>
            <el-table :data="rateLogs" size="small">
              <el-table-column prop="rateDate" label="日期" width="120" />
              <el-table-column prop="priceType" label="价格类型" width="110" />
              <el-table-column label="原价" width="110" align="right">
                <template #default="{ row }">{{ row.oldPrice == null ? '-' : '¥' + money(row.oldPrice) }}</template>
              </el-table-column>
              <el-table-column label="新价" width="110" align="right">
                <template #default="{ row }">¥{{ money(row.newPrice) }}</template>
              </el-table-column>
              <el-table-column prop="operator" label="操作人" width="110" />
              <el-table-column prop="createTime" label="时间" min-width="160">
                <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="typeDialog" :title="typeForm.id ? '编辑房型' : '新增房型'" width="620px">
      <el-form :model="typeForm" label-width="86px" class="form-grid">
        <el-form-item label="所属门店" required>
          <el-select v-model="typeForm.storeId" style="width:100%">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房型名称" required><el-input v-model="typeForm.name" /></el-form-item>
        <el-form-item label="房型编码" required><el-input v-model="typeForm.code" /></el-form-item>
        <el-form-item label="门市价" required><el-input-number v-model="typeForm.basePrice" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="面积㎡"><el-input-number v-model="typeForm.area" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="可住人数"><el-input-number v-model="typeForm.capacity" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="床型"><el-input v-model="typeForm.bedType" placeholder="如 1.8m 大床" /></el-form-item>
        <el-form-item label="封面图" class="full">
          <ImageUploader v-model="typeForm.cover" category="room-type" />
        </el-form-item>
        <el-form-item label="房型图集" class="full">
          <ImageUploader v-model="imageList" multiple category="room-type" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeForm.status">
            <el-radio :value="1">在售</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="设施" class="full"><el-input v-model="typeForm.facilities" placeholder="逗号分隔，如 独立卫浴,投影,免费WiFi" /></el-form-item>
        <el-form-item label="介绍" class="full"><el-input v-model="typeForm.intro" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialog = false">取消</el-button>
        <el-button type="primary" @click="saveType">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roomDialog" :title="roomForm.id ? '编辑房间' : '新增房间'" width="560px">
      <el-form :model="roomForm" label-width="86px" class="form-grid">
        <el-form-item label="所属门店" required>
          <el-select v-model="roomForm.storeId" style="width:100%">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房型" required>
          <el-select v-model="roomForm.roomTypeId" style="width:100%">
            <el-option v-for="t in types.filter(x => !roomForm.storeId || x.storeId === roomForm.storeId)" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房间号" required><el-input v-model="roomForm.roomNo" /></el-form-item>
        <el-form-item label="楼层"><el-input-number v-model="roomForm.floor" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="门锁编号"><el-input v-model="roomForm.lockCode" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="roomForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roomDialog = false">取消</el-button>
        <el-button type="primary" @click="saveRoom">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDialog" title="批量调价" width="440px">
      <el-form :model="batchForm" label-width="86px">
        <el-form-item label="价格类型">
          <el-select v-model="batchForm.priceType" style="width:100%">
            <el-option label="门市价 BAR" value="BAR" />
            <el-option label="渠道价 CHANNEL" value="CHANNEL" />
            <el-option label="协议价 AGREEMENT" value="AGREEMENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期区间">
          <el-date-picker v-model="batchRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" style="width:100%" />
        </el-form-item>
        <el-form-item label="每日价格"><el-input-number v-model="batchForm.price" :min="0" :precision="2" style="width:100%" /></el-form-item>
      </el-form>
      <p class="muted">将覆盖所选区间内该房型对应价格类型，单次最长 6 个月。</p>
      <template #footer>
        <el-button @click="batchDialog = false">取消</el-button>
        <el-button type="primary" @click="saveBatch">确认调价</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logsDialog" title="房态变更记录" width="560px">
      <el-table :data="statusLogs" size="small">
        <el-table-column prop="oldStatus" label="原房态" width="100" />
        <el-table-column prop="newStatus" label="新房态" width="100" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="remark" label="备注" min-width="140" />
        <el-table-column prop="createTime" label="时间" width="160">
          <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="importResult" title="导入结果" width="520px">
      <p>成功导入 <b>{{ importSummary.success }}</b> 间房。</p>
      <el-alert v-if="importSummary.errors?.length" type="warning" :closable="false" :title="`${importSummary.errors.length} 行失败`">
        <div v-for="e in importSummary.errors" :key="e" class="muted">{{ e }}</div>
      </el-alert>
      <template #footer><el-button type="primary" @click="importResult = false">知道了</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Download, Plus, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import ImageUploader from '@/components/ImageUploader.vue'
import { addDays, dateTime, money, monthStart, roomStatus, today } from '@/utils/format'

const tab = ref('types')
const stores = ref<any[]>([])
const types = ref<any[]>([])
const rooms = ref<any[]>([])
const loadingTypes = ref(false)
const loadingRooms = ref(false)
const typeStore = ref<number>()
const roomQuery = reactive<any>({ storeId: undefined, status: '', keyword: '' })
const typeDialog = ref(false)
const roomDialog = ref(false)
const batchDialog = ref(false)
const logsDialog = ref(false)
const importResult = ref(false)
const statusLogs = ref<any[]>([])
const importSummary = reactive<any>({ success: 0, errors: [] })
const rateTypeId = ref<number>()
const rateMonth = ref(today().slice(0, 7))
const rateCalendar = ref<Record<string, any>>({})
const rateLogs = ref<any[]>([])
const batchRange = ref<[string, string] | null>(null)

const emptyType = () => ({ id: undefined, storeId: undefined, name: '', code: '', area: 30, capacity: 2, bedType: '', facilities: '', cover: '', images: '', basePrice: 0, intro: '', status: 1, sort: 0 })
const typeForm = reactive<any>(emptyType())
const imageList = ref<string[]>([])
const emptyRoom = () => ({ id: undefined, storeId: undefined, roomTypeId: undefined, buildingId: 1, floor: 1, roomNo: '', status: 'VACANT', lockCode: '', remark: '' })
const roomForm = reactive<any>(emptyRoom())
const batchForm = reactive<any>({ priceType: 'BAR', price: 0 })

const rateDays = computed(() => {
  const [y, m] = rateMonth.value.split('-').map(Number)
  const days = new Date(y, m, 0).getDate()
  const week = ['日', '一', '二', '三', '四', '五', '六']
  return Array.from({ length: days }, (_, i) => {
    const date = `${rateMonth.value}-${String(i + 1).padStart(2, '0')}`
    const d = new Date(date + 'T00:00:00')
    const price = rateCalendar.value[date]?.BAR
    return { date, day: i + 1, week: '周' + week[d.getDay()], price }
  })
})

function storeName(id: number) {
  return stores.value.find(s => s.id === id)?.name || '-'
}

async function loadStores() {
  const res: any = await http.get('/admin/stores')
  stores.value = res.data || []
}

async function loadTypes() {
  loadingTypes.value = true
  try {
    const res: any = await http.get('/admin/room-types', { params: typeStore.value ? { storeId: typeStore.value } : {} })
    types.value = res.data || []
  } finally {
    loadingTypes.value = false
  }
}

async function loadRooms() {
  loadingRooms.value = true
  try {
    const res: any = await http.get('/admin/rooms', { params: roomQuery })
    rooms.value = res.data || []
  } finally {
    loadingRooms.value = false
  }
}

function parseImages(raw: any): string[] {
  if (!raw) return []
  if (Array.isArray(raw)) return raw.map(String)
  const s = String(raw).trim()
  if (!s) return []
  try {
    const arr = JSON.parse(s)
    return Array.isArray(arr) ? arr.map(String) : []
  } catch {
    return s.split(',').map(x => x.trim()).filter(Boolean)
  }
}

function openType(row?: any) {
  Object.assign(typeForm, emptyType())
  if (row) {
    Object.assign(typeForm, row)
    imageList.value = parseImages(row.images)
  } else {
    imageList.value = []
    typeForm.storeId = typeStore.value || stores.value[0]?.id
  }
  typeDialog.value = true
}

async function saveType() {
  if (!typeForm.name || !typeForm.code || !typeForm.storeId) return ElMessage.warning('请填写门店、房型名称与编码')
  typeForm.images = JSON.stringify(imageList.value)
  if (typeForm.id) await http.put(`/admin/room-types/${typeForm.id}`, typeForm)
  else await http.post('/admin/room-types', typeForm)
  ElMessage.success('保存成功')
  typeDialog.value = false
  loadTypes()
}

async function removeType(row: any) {
  await ElMessageBox.confirm(`确认删除房型「${row.name}」？`, '提示', { type: 'warning' })
  await http.delete(`/admin/room-types/${row.id}`)
  ElMessage.success('已删除')
  loadTypes()
}

function openRoom(row?: any) {
  Object.assign(roomForm, emptyRoom())
  if (row) Object.assign(roomForm, row)
  else {
    roomForm.storeId = roomQuery.storeId || stores.value[0]?.id
    roomForm.roomTypeId = types.value.find(t => t.storeId === roomForm.storeId)?.id
  }
  roomDialog.value = true
}

async function saveRoom() {
  if (!roomForm.roomNo || !roomForm.storeId || !roomForm.roomTypeId) return ElMessage.warning('请填写门店、房型与房间号')
  if (roomForm.id) await http.put(`/admin/rooms/${roomForm.id}`, roomForm)
  else await http.post('/admin/rooms', roomForm)
  ElMessage.success('保存成功')
  roomDialog.value = false
  loadRooms()
}

async function removeRoom(row: any) {
  await ElMessageBox.confirm(`确认删除房间「${row.roomNo}」？`, '提示', { type: 'warning' })
  await http.delete(`/admin/rooms/${row.id}`)
  ElMessage.success('已删除')
  loadRooms()
}

async function changeStatus(row: any, status: string) {
  await http.post(`/admin/rooms/${row.id}/status`, { status, remark: '后台手动调整' })
  ElMessage.success('房态已更新')
  loadRooms()
}

async function showLogs(row: any) {
  const res: any = await http.get(`/admin/rooms/${row.id}/status-logs`)
  statusLogs.value = res.data || []
  logsDialog.value = true
}

async function importCsv(options: any) {
  const fd = new FormData()
  fd.append('file', options.file)
  const res: any = await http.post('/admin/rooms/import', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  Object.assign(importSummary, res.data)
  importResult.value = true
  loadRooms()
}

async function exportCsv() {
  const blob: any = await http.get('/admin/rooms/export', { responseType: 'blob' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'rooms.csv'
  a.click()
  URL.revokeObjectURL(url)
}

async function loadRates() {
  if (!rateTypeId.value) return
  const [cal, logs]: any[] = await Promise.all([
    http.get('/admin/rates', { params: { roomTypeId: rateTypeId.value, month: rateMonth.value } }),
    http.get('/admin/rates/logs', { params: { roomTypeId: rateTypeId.value } })
  ])
  rateCalendar.value = cal.data?.calendar || {}
  rateLogs.value = logs.data || []
}

function openBatch() {
  batchForm.priceType = 'BAR'
  batchForm.price = 0
  const first = `${rateMonth.value}-01`
  batchRange.value = [first, addDays(first, 6)]
  batchDialog.value = true
}

async function saveBatch() {
  if (!batchRange.value || !batchForm.price) return ElMessage.warning('请选择日期并填写价格')
  const res: any = await http.post('/admin/rates/batch', {
    roomTypeId: rateTypeId.value,
    startDate: batchRange.value[0],
    endDate: batchRange.value[1],
    priceType: batchForm.priceType,
    price: batchForm.price
  })
  ElMessage.success(`已更新 ${res.data?.changed || 0} 天价格`)
  batchDialog.value = false
  loadRates()
}

watch(tab, (v) => {
  if (v === 'rooms' && !rooms.value.length) loadRooms()
  if (v === 'rates') {
    if (!types.value.length) loadTypes()
    if (!rateTypeId.value && types.value.length) { rateTypeId.value = types.value[0].id; loadRates() }
  }
})

onMounted(async () => {
  await loadStores()
  await Promise.all([loadTypes(), loadRooms()])
})
</script>

<style scoped>
.rate-grid { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 8px; }
.cover-thumb { width: 64px; height: 44px; object-fit: cover; border-radius: 6px; }
.rate-cell { border: 1px solid var(--border); border-radius: 6px; padding: 10px; background: #fbfcff; min-height: 74px; }
.rate-cell.empty { background: #f7f8fa; color: #9aa3b2; }
.rate-cell .d { font-size: 17px; font-weight: 700; }
.rate-cell .p { font-size: 14px; color: var(--primary); margin: 4px 0; font-weight: 600; }
.rate-cell .muted { font-size: 11px; }
@media (max-width: 900px) { .rate-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
</style>
