<template>
  <div class="page">
    <h2 class="page-title">房务保洁</h2>
    <p class="page-sub">清扫任务、报修工单、布草管理与客诉处理</p>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">脏房</div>
        <div class="stat-value">{{ overview.dirty || 0 }}<span class="stat-unit">间</span></div>
        <div class="stat-foot">待清扫 / 待检查</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">待办任务</div>
        <div class="stat-value">{{ overview.pendingTasks || 0 }}<span class="stat-unit">项</span></div>
        <div class="stat-foot">含续住、退房与日常保洁</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">维修中</div>
        <div class="stat-value">{{ overview.repairing || 0 }}<span class="stat-unit">单</span></div>
        <div class="stat-foot">维修完成自动恢复空房</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">待处理客诉</div>
        <div class="stat-value">{{ overview.complaints || 0 }}<span class="stat-unit">件</span></div>
        <div class="stat-foot">空房 {{ overview.vacant || 0 }} 间</div>
      </div>
    </div>

    <el-tabs v-model="tab" class="card" style="margin-top:16px">
      <el-tab-pane label="清扫任务" name="tasks">
        <div class="toolbar">
          <el-select v-model="taskStatus" clearable placeholder="全部状态" style="width:140px" @change="loadTasks">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已完成" value="DONE" />
          </el-select>
          <div class="grow"></div>
          <el-button type="primary" :icon="Plus" @click="openTask">新建任务</el-button>
        </div>
        <el-table :data="tasks">
          <el-table-column label="房间" width="110">
            <template #default="{ row }"><b>{{ row.roomNo }}</b></template>
          </el-table-column>
          <el-table-column prop="roomTypeName" label="房型" width="130" />
          <el-table-column prop="storeName" label="门店" min-width="150" />
          <el-table-column label="任务类型" width="120">
            <template #default="{ row }">{{ taskTypeMap[row.type] || row.type }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="hkStatus[row.status]?.type as any">{{ hkStatus[row.status]?.text || row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="assignee" label="负责人" width="100">
            <template #default="{ row }">{{ row.assignee || '待分配' }}</template>
          </el-table-column>
          <el-table-column prop="note" label="备注" min-width="180" show-overflow-tooltip />
          <el-table-column prop="createTime" label="创建时间" width="160">
            <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button v-if="row.status === 'PENDING'" link type="primary" @click="taskAction(row, 'PROCESSING')">开始</el-button>
              <el-button v-if="row.status !== 'DONE'" link type="success" @click="taskAction(row, 'DONE')">完成清扫</el-button>
              <span v-else class="muted">{{ dateTime(row.finishTime) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="报修工单" name="repairs">
        <div class="toolbar">
          <div class="grow"></div>
          <el-button type="primary" :icon="Plus" @click="openRepair">报修登记</el-button>
        </div>
        <el-table :data="repairs">
          <el-table-column prop="roomNo" label="房间" width="90" />
          <el-table-column prop="title" label="报修内容" min-width="200" />
          <el-table-column prop="reportUser" label="报修人" width="110" />
          <el-table-column prop="assignee" label="维修人" width="120">
            <template #default="{ row }">{{ row.assignee || '待派工' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="hkStatus[row.status]?.type as any">{{ hkStatus[row.status]?.text || row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="报修时间" width="160">
            <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-dropdown trigger="click" @command="(s: string) => repairAction(row, s)">
                <el-button link type="primary">更新状态</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="ASSIGNED">已派工</el-dropdown-item>
                    <el-dropdown-item command="REPAIRING">维修中</el-dropdown-item>
                    <el-dropdown-item command="ACCEPTING">待验收</el-dropdown-item>
                    <el-dropdown-item command="DONE" divided>维修完成</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <span v-if="row.status === 'DONE'" class="muted">已完成</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="布草管理" name="linen">
        <div class="toolbar">
          <div class="grow"></div>
          <el-button type="primary" :icon="Plus" @click="openLinen">登记收发</el-button>
        </div>
        <el-table :data="linen">
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.type === 'WASH' ? 'warning' : 'success'">
                {{ row.type === 'WASH' ? '送洗' : '收回' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="item" label="布草名称" width="140" />
          <el-table-column prop="quantity" label="数量" width="90" align="center" />
          <el-table-column prop="operator" label="操作人" width="120" />
          <el-table-column prop="createTime" label="时间" width="170">
            <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="客诉处理" name="complaints">
        <div class="toolbar">
          <div class="grow"></div>
          <el-button type="primary" :icon="Plus" @click="openComplaint">登记客诉</el-button>
        </div>
        <el-table :data="complaints">
          <el-table-column prop="roomNo" label="房间" width="90" />
          <el-table-column prop="guest" label="客人" width="110" />
          <el-table-column prop="content" label="客诉内容" min-width="240" show-overflow-tooltip />
          <el-table-column prop="handler" label="处理人" width="110" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="hkStatus[row.status]?.type as any">{{ hkStatus[row.status]?.text || row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="160">
            <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button v-if="row.status === 'PENDING'" link type="primary" @click="complaintAction(row, 'PROCESSING')">受理</el-button>
              <el-button v-if="row.status !== 'DONE'" link type="success" @click="complaintAction(row, 'DONE')">办结</el-button>
              <span v-else class="muted">已办结</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="taskDialog" title="新建清扫任务" width="480px">
      <el-form :model="taskForm" label-width="86px">
        <el-form-item label="房间" required>
          <el-select v-model="taskForm.roomId" filterable style="width:100%">
            <el-option v-for="r in rooms" :key="r.id" :label="`${r.roomNo} · ${r.roomTypeName}（${roomStatus[r.status]?.text || r.status}）`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务类型">
          <el-select v-model="taskForm.type" style="width:100%">
            <el-option v-for="(v, k) in taskTypeMap" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人"><el-input v-model="taskForm.assignee" placeholder="如 王阿姨" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="taskForm.note" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTask">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="repairDialog" title="报修登记" width="480px">
      <el-form :model="repairForm" label-width="86px">
        <el-form-item label="房间" required>
          <el-select v-model="repairForm.roomId" filterable style="width:100%">
            <el-option v-for="r in rooms" :key="r.id" :label="`${r.roomNo} · ${r.roomTypeName}`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="报修内容" required><el-input v-model="repairForm.title" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="维修人"><el-input v-model="repairForm.assignee" placeholder="可稍后派工" /></el-form-item>
      </el-form>
      <p class="muted">报修后房间自动置为「维修」状态，维修完成恢复空房。</p>
      <template #footer>
        <el-button @click="repairDialog = false">取消</el-button>
        <el-button type="primary" @click="saveRepair">提交报修</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="linenDialog" title="布草收发登记" width="440px">
      <el-form :model="linenForm" label-width="86px">
        <el-form-item label="类型">
          <el-radio-group v-model="linenForm.type">
            <el-radio-button value="TAKE">收回</el-radio-button>
            <el-radio-button value="WASH">送洗</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="布草名称" required><el-input v-model="linenForm.item" placeholder="如 床单 / 浴巾" /></el-form-item>
        <el-form-item label="数量" required><el-input-number v-model="linenForm.quantity" :min="1" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="linenDialog = false">取消</el-button>
        <el-button type="primary" @click="saveLinen">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="complaintDialog" title="客诉登记" width="480px">
      <el-form :model="complaintForm" label-width="86px">
        <el-form-item label="房间号"><el-input v-model="complaintForm.roomNo" /></el-form-item>
        <el-form-item label="客人"><el-input v-model="complaintForm.guest" /></el-form-item>
        <el-form-item label="客诉内容" required><el-input v-model="complaintForm.content" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="complaintDialog = false">取消</el-button>
        <el-button type="primary" @click="saveComplaint">登记</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '@/api/http'
import { dateTime, hkStatus, roomStatus, taskTypeMap } from '@/utils/format'

const tab = ref('tasks')
const overview = ref<any>({})
const tasks = ref<any[]>([])
const repairs = ref<any[]>([])
const linen = ref<any[]>([])
const complaints = ref<any[]>([])
const rooms = ref<any[]>([])
const taskStatus = ref('')
const taskDialog = ref(false)
const repairDialog = ref(false)
const linenDialog = ref(false)
const complaintDialog = ref(false)
const taskForm = reactive<any>({ roomId: undefined, type: 'CONTINUE', assignee: '', note: '' })
const repairForm = reactive<any>({ roomId: undefined, title: '', assignee: '' })
const linenForm = reactive<any>({ type: 'TAKE', item: '', quantity: 1 })
const complaintForm = reactive<any>({ roomNo: '', guest: '', content: '' })

async function loadOverview() {
  const res: any = await http.get('/admin/hk/overview')
  overview.value = res.data || {}
}

async function loadTasks() {
  const res: any = await http.get('/admin/hk/tasks', { params: { status: taskStatus.value } })
  tasks.value = res.data || []
}

async function loadRepairs() {
  const res: any = await http.get('/admin/hk/repairs')
  repairs.value = res.data || []
}

async function loadLinen() {
  const res: any = await http.get('/admin/hk/linen')
  linen.value = res.data || []
}

async function loadComplaints() {
  const res: any = await http.get('/admin/hk/complaints')
  complaints.value = res.data || []
}

async function loadRooms() {
  if (rooms.value.length) return
  const res: any = await http.get('/admin/rooms')
  rooms.value = res.data || []
}

function openTask() {
  loadRooms()
  Object.assign(taskForm, { roomId: undefined, type: 'CONTINUE', assignee: '', note: '' })
  taskDialog.value = true
}

async function saveTask() {
  if (!taskForm.roomId) return ElMessage.warning('请选择房间')
  await http.post('/admin/hk/tasks', taskForm)
  ElMessage.success('任务已创建')
  taskDialog.value = false
  loadTasks()
  loadOverview()
}

async function taskAction(row: any, status: string) {
  await http.post(`/admin/hk/tasks/${row.id}/status`, { status, markClean: status === 'DONE' })
  ElMessage.success(status === 'DONE' ? '清扫完成，房间已置为空房' : '任务已开始')
  loadTasks()
  loadOverview()
}

function openRepair() {
  loadRooms()
  Object.assign(repairForm, { roomId: undefined, title: '', assignee: '' })
  repairDialog.value = true
}

async function saveRepair() {
  if (!repairForm.roomId || !repairForm.title) return ElMessage.warning('请选择房间并填写报修内容')
  await http.post('/admin/hk/repairs', repairForm)
  ElMessage.success('报修已提交，房间已置为维修')
  repairDialog.value = false
  loadRepairs()
  loadOverview()
}

async function repairAction(row: any, status: string) {
  await http.post(`/admin/hk/repairs/${row.id}/status`, { status })
  ElMessage.success('工单状态已更新')
  loadRepairs()
  loadOverview()
}

function openLinen() {
  Object.assign(linenForm, { type: 'TAKE', item: '', quantity: 1 })
  linenDialog.value = true
}

async function saveLinen() {
  if (!linenForm.item) return ElMessage.warning('请填写布草名称')
  await http.post('/admin/hk/linen', linenForm)
  ElMessage.success('收发记录已保存')
  linenDialog.value = false
  loadLinen()
}

function openComplaint() {
  Object.assign(complaintForm, { roomNo: '', guest: '', content: '' })
  complaintDialog.value = true
}

async function saveComplaint() {
  if (!complaintForm.content) return ElMessage.warning('请填写客诉内容')
  await http.post('/admin/hk/complaints', complaintForm)
  ElMessage.success('客诉已登记')
  complaintDialog.value = false
  loadComplaints()
  loadOverview()
}

async function complaintAction(row: any, status: string) {
  await http.post(`/admin/hk/complaints/${row.id}/status`, { status })
  ElMessage.success('客诉状态已更新')
  loadComplaints()
  loadOverview()
}

watch(tab, (v) => {
  if (v === 'repairs') loadRepairs()
  if (v === 'linen') loadLinen()
  if (v === 'complaints') loadComplaints()
})

onMounted(() => {
  loadOverview()
  loadTasks()
  loadRepairs()
  loadLinen()
  loadComplaints()
})
</script>
