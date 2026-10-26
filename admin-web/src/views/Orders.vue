<template>
  <div class="page">
    <h2 class="page-title">预订管理</h2>
    <p class="page-sub">全渠道订单、入住人信息与订单全生命周期操作</p>

    <div class="card">
      <div class="toolbar">
        <el-select v-model="query.status" clearable placeholder="全部状态" style="width:130px" @change="load(1)">
          <el-option v-for="(v, k) in orderStatus" :key="k" :label="v.text" :value="k" />
        </el-select>
        <el-select v-model="query.source" clearable placeholder="全部来源" style="width:140px" @change="load(1)">
          <el-option v-for="(v, k) in sourceMap" :key="k" :label="v" :value="k" />
        </el-select>
        <el-select v-model="query.storeId" clearable placeholder="全部门店" style="width:170px" @change="load(1)">
          <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="入住起" end-placeholder="离店止" style="width:250px" @change="load(1)" />
        <el-input v-model="query.keyword" placeholder="订单号 / 姓名 / 手机" clearable style="width:200px" @keyup.enter="load(1)" />
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <div class="grow"></div>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建订单</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" size="default">
        <el-table-column prop="orderNo" label="订单号" width="175" fixed>
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ row.orderNo }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="guestName" label="入住人" width="100" />
        <el-table-column prop="guestPhone" label="手机号" width="125" />
        <el-table-column prop="storeName" label="门店" min-width="140" show-overflow-tooltip />
        <el-table-column prop="roomTypeName" label="房型" width="120" show-overflow-tooltip />
        <el-table-column prop="roomNo" label="房间" width="80" />
        <el-table-column label="入住/离店" width="185">
          <template #default="{ row }">
            <span>{{ row.checkIn }} → {{ row.checkOut }}</span>
            <span class="muted"> · {{ row.nights }} 晚</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }">¥{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="95">
          <template #default="{ row }">
            <el-tag size="small" :type="orderStatus[row.status]?.type as any">{{ orderStatus[row.status]?.text || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="110">
          <template #default="{ row }">{{ sourceMap[row.source] || row.source }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="['PENDING', 'CONFIRMED'].includes(row.status)" link type="success" @click="openCheckin(row)">入住</el-button>
            <el-button v-if="row.status === 'CHECKED_IN'" link type="success" @click="openCheckout(row)">退房</el-button>
            <el-dropdown v-if="['PENDING', 'CONFIRMED', 'CHECKED_IN'].includes(row.status)" trigger="click" @command="(c: string) => more(row, c)">
              <el-button link type="primary">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="change" :disabled="row.status !== 'CHECKED_IN'">换房</el-dropdown-item>
                  <el-dropdown-item command="extend" :disabled="row.status !== 'CHECKED_IN'">续住</el-dropdown-item>
                  <el-dropdown-item command="deposit" :disabled="row.status !== 'CHECKED_IN'">押金</el-dropdown-item>
                  <el-dropdown-item command="cancel" divided>取消订单</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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

    <el-dialog v-model="createDialog" title="新建订单" width="700px" @closed="resetCreate">
      <el-form :model="createForm" label-width="90px" class="form-grid">
        <el-form-item label="门店" required>
          <el-select v-model="createForm.storeId" style="width:100%" @change="onStoreChange">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房型" required>
          <el-select v-model="createForm.roomTypeId" style="width:100%" @change="queryAvailability">
            <el-option v-for="t in createTypes" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入住日期" required>
          <el-date-picker v-model="createForm.checkIn" type="date" value-format="YYYY-MM-DD" style="width:100%" @change="queryAvailability" />
        </el-form-item>
        <el-form-item label="离店日期" required>
          <el-date-picker v-model="createForm.checkOut" type="date" value-format="YYYY-MM-DD" style="width:100%" @change="queryAvailability" />
        </el-form-item>
        <el-form-item label="指定房间">
          <el-select v-model="createForm.roomId" clearable placeholder="系统自动分配" style="width:100%">
            <el-option v-for="r in availableRooms" :key="r.id" :label="`${r.roomNo} 房 · ¥${money(r.price)}/晚`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单来源">
          <el-select v-model="createForm.source" style="width:100%">
            <el-option v-for="(v, k) in sourceMap" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="入住人" required><el-input v-model="createForm.guestName" /></el-form-item>
        <el-form-item label="手机号" required><el-input v-model="createForm.guestPhone" /></el-form-item>
        <el-form-item label="证件号"><el-input v-model="createForm.idNo" placeholder="用于实名登记（加密存储）" /></el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="createForm.payType" style="width:100%">
            <el-option v-for="(v, k) in payTypeMap" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="协议单位"><el-input v-model="createForm.company" /></el-form-item>
        <el-form-item label="优惠金额"><el-input-number v-model="createForm.discount" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="备注" class="full"><el-input v-model="createForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <el-alert v-if="availTip" type="info" :closable="false" :title="availTip" style="margin-bottom:10px" />
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">创建订单</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="订单详情" size="620px">
      <template v-if="detail.order">
        <div class="detail-head">
          <div>
            <h3 style="margin:0">{{ detail.order.orderNo }}</h3>
            <p class="muted" style="margin:4px 0 0">{{ detail.order.storeName }} · {{ detail.order.roomTypeName }} · {{ detail.order.roomNo }}</p>
          </div>
          <el-tag :type="orderStatus[detail.order.status]?.type as any" effect="dark">{{ orderStatus[detail.order.status]?.text }}</el-tag>
        </div>
        <el-descriptions :column="2" border style="margin-top:14px">
          <el-descriptions-item label="入住人">{{ detail.order.guestName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.order.guestPhone }}</el-descriptions-item>
          <el-descriptions-item label="入住">{{ detail.order.checkIn }}</el-descriptions-item>
          <el-descriptions-item label="离店">{{ detail.order.checkOut }}（{{ detail.order.nights }} 晚）</el-descriptions-item>
          <el-descriptions-item label="金额">¥{{ money(detail.order.amount) }}</el-descriptions-item>
          <el-descriptions-item label="已付">¥{{ money(detail.order.paidAmount) }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ sourceMap[detail.order.source] || detail.order.source }}</el-descriptions-item>
          <el-descriptions-item label="支付">{{ payTypeMap[detail.order.payType] || detail.order.payType }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.order.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="detail.checkin" style="margin-top:18px">
          <div class="section-title">入住登记</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="证件号">{{ detail.checkin.idNoMask }}</el-descriptions-item>
            <el-descriptions-item label="房卡">{{ detail.checkin.cardNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="入住时间">{{ dateTime(detail.checkin.checkInTime) }}</el-descriptions-item>
            <el-descriptions-item label="押金">¥{{ money(detail.checkin.deposit) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div v-if="detail.guests?.length" style="margin-top:18px">
          <div class="section-title">同住人</div>
          <el-table :data="detail.guests" size="small">
            <el-table-column prop="name" label="姓名" width="100" />
            <el-table-column prop="phone" label="手机" width="130" />
            <el-table-column prop="idNoMask" label="证件号" min-width="160" />
            <el-table-column label="未成年" width="80">
              <template #default="{ row }">{{ row.isMinor ? '是' : '否' }}</template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="detail.deposits?.length" style="margin-top:18px">
          <div class="section-title">押金流水</div>
          <el-table :data="detail.deposits" size="small">
            <el-table-column prop="type" label="类型" width="90">
              <template #default="{ row }">{{ row.type === 'REFUND' ? '退还' : '收取' }}</template>
            </el-table-column>
            <el-table-column label="金额" width="110" align="right">
              <template #default="{ row }">¥{{ money(row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="payType" label="方式" width="100" />
            <el-table-column prop="createTime" label="时间" min-width="150">
              <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="detail.settlement" style="margin-top:18px">
          <div class="section-title">退房结算</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="房费">¥{{ money(detail.settlement.roomFee) }}</el-descriptions-item>
            <el-descriptions-item label="其他费用">¥{{ money(detail.settlement.otherFee) }}</el-descriptions-item>
            <el-descriptions-item label="优惠">¥{{ money(detail.settlement.discount) }}</el-descriptions-item>
            <el-descriptions-item label="押金扣减">¥{{ money(detail.settlement.depositDeduct) }}</el-descriptions-item>
            <el-descriptions-item label="实收">¥{{ money(detail.settlement.total) }}</el-descriptions-item>
            <el-descriptions-item label="支付">{{ payTypeMap[detail.settlement.payType] || detail.settlement.payType }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div style="margin-top:18px">
          <div class="section-title">状态日志</div>
          <el-timeline>
            <el-timeline-item v-for="l in detail.logs || []" :key="l.id" :timestamp="dateTime(l.createTime)">
              <b>{{ l.fromStatus || '-' }} → {{ l.toStatus }}</b>
              <span class="muted"> · {{ l.operator || 'system' }} {{ l.remark || '' }}</span>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-if="!(detail.logs || []).length" description="暂无状态记录" :image-size="60" />
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="checkinDialog" title="办理入住" width="520px">
      <el-form :model="checkinForm" label-width="90px">
        <el-form-item label="安排房间" required>
          <el-select v-model="checkinForm.roomId" style="width:100%" placeholder="选择可用房间">
            <el-option v-for="r in checkinRooms" :key="r.id" :label="`${r.roomNo} · ${r.roomTypeName}`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入住人"><el-input v-model="checkinForm.guestName" /></el-form-item>
        <el-form-item label="证件号"><el-input v-model="checkinForm.idNo" placeholder="身份证号（加密存储）" /></el-form-item>
        <el-form-item label="押金"><el-input-number v-model="checkinForm.deposit" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="押金方式">
          <el-select v-model="checkinForm.depositPayType" style="width:100%">
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="现金" value="CASH" />
            <el-option label="银行卡" value="CARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="房卡号"><el-input v-model="checkinForm.cardNo" /></el-form-item>
        <el-form-item label="同住/未成年"><el-input v-model="checkinForm.minorInfo" placeholder="如有未成年人请填写监护人信息" /></el-form-item>
      </el-form>
      <p class="muted">提交后自动生成入住登记、公安报送演示回执与迎宾消息。</p>
      <template #footer>
        <el-button @click="checkinDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCheckin">确认入住</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="checkoutDialog" title="退房结算" width="520px">
      <el-form :model="checkoutForm" label-width="96px">
        <el-form-item label="优惠金额"><el-input-number v-model="checkoutForm.discount" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="其他费用"><el-input-number v-model="checkoutForm.otherFee" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="押金扣减"><el-input-number v-model="checkoutForm.depositDeduct" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="结算方式">
          <el-select v-model="checkoutForm.payType" style="width:100%">
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="现金" value="CASH" />
            <el-option label="银行卡" value="CARD" />
            <el-option label="协议挂账" value="AR" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="checkoutForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <p class="muted">退房后自动生成结算单、押金退还记录、清扫任务与退房提醒。</p>
      <template #footer>
        <el-button @click="checkoutDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCheckout">确认退房</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="changeDialog" title="换房" width="460px">
      <el-form label-width="86px">
        <el-form-item label="目标房间" required>
          <el-select v-model="changeForm.roomId" style="width:100%">
            <el-option v-for="r in vacantRooms" :key="r.id" :label="`${r.roomNo} · ${r.roomTypeName}`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因"><el-input v-model="changeForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeDialog = false">取消</el-button>
        <el-button type="primary" @click="submitChange">确认换房</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="extendDialog" title="续住" width="440px">
      <el-form label-width="96px">
        <el-form-item label="新离店日期" required>
          <el-date-picker v-model="extendForm.newCheckOut" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="extendForm.remark" /></el-form-item>
      </el-form>
      <p class="muted">续住将自动补算房费并检查房间可用性。</p>
      <template #footer>
        <el-button @click="extendDialog = false">取消</el-button>
        <el-button type="primary" @click="submitExtend">确认续住</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="depositDialog" title="押金操作" width="420px">
      <el-form label-width="86px">
        <el-form-item label="操作类型">
          <el-radio-group v-model="depositForm.type">
            <el-radio-button value="COLLECT">收取</el-radio-button>
            <el-radio-button value="REFUND">退还</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额"><el-input-number v-model="depositForm.amount" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="depositForm.payType" style="width:100%">
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="现金" value="CASH" />
            <el-option label="银行卡" value="CARD" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="depositDialog = false">取消</el-button>
        <el-button type="primary" @click="submitDeposit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import { addDays, dateTime, money, orderStatus, payTypeMap, sourceMap, today } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const stores = ref<any[]>([])
const types = ref<any[]>([])
const rooms = ref<any[]>([])
const dateRange = ref<[string, string] | null>(null)
const query = reactive<any>({ page: 1, size: 10, status: '', source: '', storeId: undefined, keyword: '' })

const createDialog = ref(false)
const createTypes = computed(() => types.value.filter(t => t.storeId === createForm.storeId))
const availableRooms = ref<any[]>([])
const availTip = ref('')
const createForm = reactive<any>({})

const detailVisible = ref(false)
const detail = ref<any>({})
const currentOrder = ref<any>(null)

const checkinDialog = ref(false)
const checkinForm = reactive<any>({})
const checkinRooms = ref<any[]>([])
const checkoutDialog = ref(false)
const checkoutForm = reactive<any>({})
const changeDialog = ref(false)
const changeForm = reactive<any>({})
const vacantRooms = ref<any[]>([])
const extendDialog = ref(false)
const extendForm = reactive<any>({})
const depositDialog = ref(false)
const depositForm = reactive<any>({})

function resetCreate() {
  Object.assign(createForm, {
    storeId: stores.value[0]?.id, roomTypeId: undefined, roomId: undefined,
    guestName: '', guestPhone: '', idNo: '', checkIn: today(), checkOut: addDays(today(), 1),
    source: 'WALK_IN', payType: 'ARRIVAL', company: '', remark: '', discount: 0, roomCount: 1
  })
  availableRooms.value = []
  availTip.value = ''
  queryAvailability()
}

async function load(page?: number) {
  if (page) query.page = page
  loading.value = true
  try {
    const params: any = { ...query }
    if (dateRange.value?.length) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res: any = await http.get('/admin/orders', { params })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function reset() {
  query.status = ''
  query.source = ''
  query.storeId = undefined
  query.keyword = ''
  dateRange.value = null
  load(1)
}

function onStoreChange() {
  createForm.roomTypeId = createTypes.value[0]?.id
  queryAvailability()
}

async function queryAvailability() {
  availTip.value = ''
  availableRooms.value = []
  if (!createForm.roomTypeId || !createForm.checkIn || !createForm.checkOut) return
  if (createForm.checkOut <= createForm.checkIn) {
    availTip.value = '离店日期必须晚于入住日期'
    return
  }
  try {
    const res: any = await http.get('/admin/orders/availability', {
      params: { roomTypeId: createForm.roomTypeId, checkIn: createForm.checkIn, checkOut: createForm.checkOut }
    })
    availableRooms.value = res.data || []
    availTip.value = availableRooms.value.length
      ? `该房型当前有 ${availableRooms.value.length} 间可订，参考价 ¥${money(availableRooms.value[0]?.price)} / 晚`
      : '该日期区间暂无可订房间，可继续创建候补订单'
  } catch { /* handled globally */ }
}

function openCreate() {
  resetCreate()
  createDialog.value = true
}

async function submitCreate() {
  if (!createForm.guestName || !createForm.guestPhone) return ElMessage.warning('请填写入住人与手机号')
  if (!createForm.roomTypeId || !createForm.checkIn || !createForm.checkOut) return ElMessage.warning('请选择房型与日期')
  if (createForm.checkOut <= createForm.checkIn) return ElMessage.warning('离店日期必须晚于入住日期')
  saving.value = true
  try {
    await http.post('/admin/orders', createForm)
    ElMessage.success('订单创建成功')
    createDialog.value = false
    load(1)
  } finally {
    saving.value = false
  }
}

async function openDetail(row: any) {
  const res: any = await http.get(`/admin/orders/${row.id}`)
  detail.value = res.data || {}
  detailVisible.value = true
}

function openCheckin(row: any) {
  currentOrder.value = row
  Object.assign(checkinForm, {
    roomId: row.roomId || undefined, guestName: row.guestName, idNo: '',
    deposit: 300, depositPayType: 'WECHAT', cardNo: '', minorInfo: ''
  })
  http.get('/admin/rooms', { params: { roomTypeId: row.roomTypeId } }).then((res: any) => {
    checkinRooms.value = (res.data || []).filter((r: any) => ['VACANT', 'RESERVED'].includes(r.status) || r.id === row.roomId)
  })
  checkinDialog.value = true
}

async function submitCheckin() {
  if (!checkinForm.roomId) return ElMessage.warning('请选择房间')
  await http.post(`/admin/orders/${currentOrder.value.id}/checkin`, checkinForm)
  ElMessage.success('入住办理成功')
  checkinDialog.value = false
  detailVisible.value = false
  load()
}

function openCheckout(row: any) {
  currentOrder.value = row
  Object.assign(checkoutForm, { discount: 0, otherFee: 0, depositDeduct: 0, payType: 'WECHAT', remark: '' })
  checkoutDialog.value = true
}

async function submitCheckout() {
  await http.post(`/admin/orders/${currentOrder.value.id}/checkout`, checkoutForm)
  ElMessage.success('退房结算完成')
  checkoutDialog.value = false
  detailVisible.value = false
  load()
}

async function more(row: any, cmd: string) {
  currentOrder.value = row
  if (cmd === 'cancel') {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单', {
      inputPlaceholder: '如 客人行程变更', confirmButtonText: '确认取消', cancelButtonText: '返回', type: 'warning'
    })
    await http.post(`/admin/orders/${row.id}/cancel`, { reason: value })
    ElMessage.success('订单已取消')
    load()
  } else if (cmd === 'change') {
    const res: any = await http.get('/admin/rooms', { params: { status: 'VACANT' } })
    vacantRooms.value = res.data || []
    Object.assign(changeForm, { roomId: undefined, remark: '' })
    changeDialog.value = true
  } else if (cmd === 'extend') {
    Object.assign(extendForm, { newCheckOut: addDays(row.checkOut, 1), remark: '' })
    extendDialog.value = true
  } else if (cmd === 'deposit') {
    Object.assign(depositForm, { type: 'COLLECT', amount: 200, payType: 'WECHAT' })
    depositDialog.value = true
  }
}

async function submitChange() {
  if (!changeForm.roomId) return ElMessage.warning('请选择目标房间')
  await http.post(`/admin/orders/${currentOrder.value.id}/change-room`, changeForm)
  ElMessage.success('换房成功')
  changeDialog.value = false
  load()
}

async function submitExtend() {
  if (!extendForm.newCheckOut) return ElMessage.warning('请选择新的离店日期')
  await http.post(`/admin/orders/${currentOrder.value.id}/extend`, extendForm)
  ElMessage.success('续住成功，房费已补算')
  extendDialog.value = false
  load()
}

async function submitDeposit() {
  if (!depositForm.amount) return ElMessage.warning('请输入金额')
  await http.post(`/admin/orders/${currentOrder.value.id}/deposit`, depositForm)
  ElMessage.success('押金操作成功')
  depositDialog.value = false
  load()
}

onMounted(async () => {
  const [s, t, r]: any[] = await Promise.all([
    http.get('/admin/stores'), http.get('/admin/room-types'), http.get('/admin/rooms')
  ])
  stores.value = s.data || []
  types.value = t.data || []
  rooms.value = r.data || []
  resetCreate()
  load()
})
</script>

<style scoped>
.detail-head { display: flex; justify-content: space-between; align-items: flex-start; }
</style>
