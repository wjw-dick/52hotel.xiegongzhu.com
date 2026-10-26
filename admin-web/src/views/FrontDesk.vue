<template>
  <div class="page">
    <h2 class="page-title">前台接待</h2>
    <p class="page-sub">今日预抵预离、入住登记与公安实名报送</p>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">今日预抵</div>
        <div class="stat-value">{{ todayData.arrivals?.length || 0 }}<span class="stat-unit">单</span></div>
        <div class="stat-foot">待办理入住</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日预离</div>
        <div class="stat-value">{{ todayData.departures?.length || 0 }}<span class="stat-unit">单</span></div>
        <div class="stat-foot">待退房结算</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">在住</div>
        <div class="stat-value">{{ todayData.inHouseCount || 0 }}<span class="stat-unit">间</span></div>
        <div class="stat-foot">空房 {{ todayData.vacantCount || 0 }} · 脏房 {{ todayData.dirtyCount || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日日期</div>
        <div class="stat-value" style="font-size:22px">{{ today }}</div>
        <div class="stat-foot">前台当班可自助交接</div>
      </div>
    </div>

    <el-tabs v-model="tab" class="card" style="margin-top:16px">
      <el-tab-pane label="今日接待" name="today">
        <div class="section-title">预抵订单</div>
        <el-table :data="todayData.arrivals || []" size="default">
          <el-table-column prop="orderNo" label="订单号" width="175" />
          <el-table-column prop="guestName" label="入住人" width="110" />
          <el-table-column prop="guestPhone" label="手机号" width="130" />
          <el-table-column prop="roomTypeName" label="房型" min-width="130" />
          <el-table-column prop="roomNo" label="房间" width="90" />
          <el-table-column label="金额" width="110" align="right">
            <template #default="{ row }">¥{{ money(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="95">
            <template #default="{ row }">
              <el-tag size="small" :type="orderStatus[row.status]?.type as any">{{ orderStatus[row.status]?.text }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="success" @click="openCheckin(row)">办理入住</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!(todayData.arrivals || []).length" description="今日暂无预抵" :image-size="70" />

        <div class="section-title" style="margin-top:24px">预离订单</div>
        <el-table :data="todayData.departures || []" size="default">
          <el-table-column prop="orderNo" label="订单号" width="175" />
          <el-table-column prop="guestName" label="入住人" width="110" />
          <el-table-column prop="roomNo" label="房间" width="90" />
          <el-table-column prop="roomTypeName" label="房型" min-width="130" />
          <el-table-column label="金额" width="110" align="right">
            <template #default="{ row }">¥{{ money(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="warning" @click="openCheckout(row)">退房结算</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!(todayData.departures || []).length" description="今日暂无预离" :image-size="70" />
      </el-tab-pane>

      <el-tab-pane label="入住登记" name="checkins">
        <div class="toolbar">
          <el-select v-model="checkinQuery.status" clearable placeholder="全部状态" style="width:140px" @change="loadCheckins">
            <el-option label="在住" value="IN_HOUSE" />
            <el-option label="已退房" value="CHECKED_OUT" />
          </el-select>
          <el-input v-model="checkinQuery.keyword" placeholder="入住人姓名" clearable style="width:180px" @keyup.enter="loadCheckins" />
          <el-button type="primary" @click="loadCheckins">查询</el-button>
        </div>
        <el-table v-loading="checkinsLoading" :data="checkins">
          <el-table-column prop="guestName" label="入住人" width="110" />
          <el-table-column prop="idNoMask" label="证件号" min-width="170" />
          <el-table-column prop="roomNo" label="房间" width="80" />
          <el-table-column prop="roomTypeName" label="房型" min-width="120" />
          <el-table-column prop="storeName" label="门店" min-width="140" />
          <el-table-column prop="checkInTime" label="入住时间" width="160">
            <template #default="{ row }">{{ dateTime(row.checkInTime) }}</template>
          </el-table-column>
          <el-table-column prop="expectCheckOut" label="预计离店" width="120">
            <template #default="{ row }">{{ dateTime(row.expectCheckOut) }}</template>
          </el-table-column>
          <el-table-column label="押金" width="100" align="right">
            <template #default="{ row }">¥{{ money(row.deposit) }}</template>
          </el-table-column>
          <el-table-column prop="cardNo" label="房卡" width="120" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'IN_HOUSE' ? 'success' : 'info'">
                {{ row.status === 'IN_HOUSE' ? '在住' : '已退房' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="primary" @click="showChanges(row)">变更记录</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="公安报送" name="police">
        <div class="toolbar">
          <el-input v-model="policeKeyword" placeholder="入住人姓名" clearable style="width:200px" @keyup.enter="loadPolice" />
          <el-button type="primary" @click="loadPolice">查询</el-button>
          <span class="muted">演示环境使用模拟报送；正式环境需配置公安旅馆业系统接口参数</span>
        </div>
        <el-table :data="police">
          <el-table-column prop="guestName" label="入住人" width="110" />
          <el-table-column prop="idNoMask" label="证件号" min-width="170" />
          <el-table-column prop="roomNo" label="房间" width="80" />
          <el-table-column prop="reportTime" label="报送时间" width="170">
            <template #default="{ row }">{{ dateTime(row.reportTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.reportStatus === 'SUCCESS' ? 'success' : 'danger'">
                {{ row.reportStatus === 'SUCCESS' ? '报送成功' : '报送失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="response" label="回执" min-width="240" show-overflow-tooltip />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button v-if="row.reportStatus !== 'SUCCESS'" link type="primary" @click="retry(row)">重新报送</el-button>
              <span v-else class="muted">已完成</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="checkinDialog" title="办理入住" width="520px">
      <el-form :model="checkinForm" label-width="90px">
        <el-form-item label="安排房间" required>
          <el-select v-model="checkinForm.roomId" style="width:100%">
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
      </el-form>
      <template #footer>
        <el-button @click="checkinDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCheckin">确认入住</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="checkoutDialog" title="退房结算" width="500px">
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
      </el-form>
      <template #footer>
        <el-button @click="checkoutDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCheckout">确认退房</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="changesDialog" title="在住变更记录" width="540px">
      <el-table :data="changes" size="small">
        <el-table-column prop="changeType" label="类型" width="100" />
        <el-table-column prop="oldValue" label="原值" width="110" />
        <el-table-column prop="newValue" label="新值" width="110" />
        <el-table-column prop="remark" label="备注" min-width="130" />
        <el-table-column prop="createTime" label="时间" width="160">
          <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!changes.length" description="暂无变更记录" :image-size="60" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '@/api/http'
import { dateTime, money, orderStatus, today } from '@/utils/format'

const tab = ref('today')
const todayData = ref<any>({})
const checkins = ref<any[]>([])
const checkinsLoading = ref(false)
const checkinQuery = reactive<any>({ status: '', keyword: '' })
const police = ref<any[]>([])
const policeKeyword = ref('')
const changes = ref<any[]>([])
const changesDialog = ref(false)
const checkinDialog = ref(false)
const checkoutDialog = ref(false)
const checkinRooms = ref<any[]>([])
const currentOrder = ref<any>(null)
const checkinForm = reactive<any>({})
const checkoutForm = reactive<any>({})

async function loadToday() {
  const res: any = await http.get('/admin/orders/today')
  todayData.value = res.data || {}
}

async function loadCheckins() {
  checkinsLoading.value = true
  try {
    const res: any = await http.get('/admin/checkins', { params: checkinQuery })
    checkins.value = res.data || []
  } finally {
    checkinsLoading.value = false
  }
}

async function loadPolice() {
  const res: any = await http.get('/admin/police-reports', { params: { keyword: policeKeyword.value } })
  police.value = res.data || []
}

async function openCheckin(row: any) {
  currentOrder.value = row
  Object.assign(checkinForm, {
    roomId: row.roomId || undefined, guestName: row.guestName, idNo: '',
    deposit: 300, depositPayType: 'WECHAT', cardNo: ''
  })
  const res: any = await http.get('/admin/rooms', { params: { roomTypeId: row.roomTypeId } })
  checkinRooms.value = (res.data || []).filter((r: any) => ['VACANT', 'RESERVED'].includes(r.status) || r.id === row.roomId)
  checkinDialog.value = true
}

async function submitCheckin() {
  if (!checkinForm.roomId) return ElMessage.warning('请选择房间')
  await http.post(`/admin/orders/${currentOrder.value.id}/checkin`, checkinForm)
  ElMessage.success('入住办理成功')
  checkinDialog.value = false
  await Promise.all([loadToday(), loadCheckins(), loadPolice()])
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
  await Promise.all([loadToday(), loadCheckins()])
}

async function showChanges(row: any) {
  const res: any = await http.get(`/admin/checkins/${row.id}/changes`)
  changes.value = res.data || []
  changesDialog.value = true
}

async function retry(row: any) {
  await http.post(`/admin/police-reports/${row.id}/retry`)
  ElMessage.success('重新报送成功')
  loadPolice()
}

onMounted(() => {
  loadToday()
  loadCheckins()
  loadPolice()
})
</script>
