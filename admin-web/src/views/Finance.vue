<template>
  <div class="page">
    <h2 class="page-title">收银财务</h2>
    <p class="page-sub">营收结算、押金、应收挂账与交接班管理</p>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">今日营收</div>
        <div class="stat-value">¥{{ money(overview.todayRevenue) }}</div>
        <div class="stat-foot">累计 ¥{{ money(overview.totalRevenue) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">结算单</div>
        <div class="stat-value">{{ overview.settlementCount || 0 }}<span class="stat-unit">笔</span></div>
        <div class="stat-foot">含退房与手工结算</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">押金余额</div>
        <div class="stat-value">¥{{ money(overview.depositBalance) }}</div>
        <div class="stat-foot">在住押金净额</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">应收挂账</div>
        <div class="stat-value">¥{{ money(overview.arBalance) }}</div>
        <div class="stat-foot">协议单位未结金额</div>
      </div>
    </div>

    <el-tabs v-model="tab" class="card" style="margin-top:16px">
      <el-tab-pane label="结算单" name="settlements">
        <div class="toolbar">
          <el-date-picker v-model="settleRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" style="width:250px" @change="loadSettlements" />
          <el-button type="primary" @click="loadSettlements">查询</el-button>
          <div class="grow"></div>
          <el-button :icon="Download" @click="exportSummary">导出订单明细</el-button>
        </div>
        <el-table :data="settlements">
          <el-table-column prop="orderNo" label="订单号" width="175" />
          <el-table-column prop="guestName" label="客人" width="110" />
          <el-table-column label="房费" width="110" align="right">
            <template #default="{ row }">¥{{ money(row.roomFee) }}</template>
          </el-table-column>
          <el-table-column label="其他" width="100" align="right">
            <template #default="{ row }">¥{{ money(row.otherFee) }}</template>
          </el-table-column>
          <el-table-column label="优惠" width="100" align="right">
            <template #default="{ row }">¥{{ money(row.discount) }}</template>
          </el-table-column>
          <el-table-column label="押金扣减" width="110" align="right">
            <template #default="{ row }">¥{{ money(row.depositDeduct) }}</template>
          </el-table-column>
          <el-table-column label="实收" width="120" align="right">
            <template #default="{ row }"><b>¥{{ money(row.total) }}</b></template>
          </el-table-column>
          <el-table-column label="支付方式" width="110">
            <template #default="{ row }">{{ payTypeMap[row.payType] || row.payType }}</template>
          </el-table-column>
          <el-table-column prop="operator" label="收银员" width="100" />
          <el-table-column prop="createTime" label="结算时间" width="160">
            <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="收银流水" name="cashier">
        <el-table :data="cashier">
          <el-table-column prop="orderNo" label="关联订单" width="175" />
          <el-table-column label="业务类型" width="110">
            <template #default="{ row }">{{ ({ ROOM: '房费', DINING: '餐饮', OTHER: '其他' } as any)[row.bizType] || row.bizType }}</template>
          </el-table-column>
          <el-table-column label="金额" width="120" align="right">
            <template #default="{ row }">¥{{ money(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="支付方式" width="110">
            <template #default="{ row }">{{ payTypeMap[row.payType] || row.payType }}</template>
          </el-table-column>
          <el-table-column prop="operator" label="操作人" width="110" />
          <el-table-column prop="createTime" label="时间" width="170">
            <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="押金流水" name="deposits">
        <el-table :data="deposits">
          <el-table-column prop="guestName" label="客人" width="120" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.type === 'REFUND' ? 'info' : 'success'">{{ row.type === 'REFUND' ? '退还' : '收取' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="120" align="right">
            <template #default="{ row }">¥{{ money(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="支付方式" width="110">
            <template #default="{ row }">{{ payTypeMap[row.payType] || row.payType }}</template>
          </el-table-column>
          <el-table-column prop="operator" label="操作人" width="110" />
          <el-table-column prop="createTime" label="时间" width="170">
            <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="应收挂账" name="ar">
        <div class="toolbar">
          <div class="grow"></div>
          <el-button type="primary" :icon="Plus" @click="openAr">新增挂账单位</el-button>
        </div>
        <el-table :data="arList">
          <el-table-column prop="company" label="挂账单位" min-width="200" />
          <el-table-column prop="contact" label="联系人" width="110" />
          <el-table-column prop="phone" label="电话" width="130" />
          <el-table-column label="挂账余额" width="130" align="right">
            <template #default="{ row }"><b style="color:#d33">¥{{ money(row.balance) }}</b></template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'NORMAL' ? 'success' : 'info'">{{ row.status === 'NORMAL' ? '正常' : row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button link type="primary" @click="showLedger(row)">明细</el-button>
              <el-button link type="success" @click="openTrade(row, 'CHARGE')">挂账</el-button>
              <el-button link type="warning" @click="openTrade(row, 'VERIFY')">核销</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="交接班" name="shifts">
        <div class="toolbar">
          <span class="muted">同一时间仅允许一个未关闭班次</span>
          <div class="grow"></div>
          <el-button type="primary" :icon="Plus" @click="openShift">开班</el-button>
        </div>
        <el-table :data="shifts">
          <el-table-column prop="shiftName" label="班次" width="110" />
          <el-table-column prop="operator" label="收银员" width="110" />
          <el-table-column label="备用金" width="120" align="right">
            <template #default="{ row }">¥{{ money(row.cashAmount) }}</template>
          </el-table-column>
          <el-table-column prop="startTime" label="开始时间" width="170">
            <template #default="{ row }">{{ dateTime(row.startTime) }}</template>
          </el-table-column>
          <el-table-column prop="endTime" label="结束时间" width="170">
            <template #default="{ row }">{{ dateTime(row.endTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'OPEN' ? 'success' : 'info'">{{ row.status === 'OPEN' ? '当班中' : '已交班' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button v-if="row.status === 'OPEN'" link type="primary" @click="closeShift(row)">交班</el-button>
              <span v-else class="muted">已完成</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="arDialog" title="新增挂账单位" width="460px">
      <el-form :model="arForm" label-width="86px">
        <el-form-item label="单位名称" required><el-input v-model="arForm.company" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="arForm.contact" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="arForm.phone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="arDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAr">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="tradeDialog" :title="tradeForm.type === 'VERIFY' ? '挂账核销' : '新增挂账'" width="420px">
      <el-form :model="tradeForm" label-width="86px">
        <el-form-item label="单位"><el-input :model-value="tradeRow?.company" disabled /></el-form-item>
        <el-form-item label="当前余额">¥{{ money(tradeRow?.balance) }}</el-form-item>
        <el-form-item label="金额" required><el-input-number v-model="tradeForm.amount" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="tradeForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tradeDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTrade">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="ledgerDialog" title="挂账明细" width="600px">
      <el-table :data="ledger" size="small">
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ row.type === 'VERIFY' ? '核销' : '挂账' }}</template>
        </el-table-column>
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }">¥{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="createTime" label="时间" width="160">
          <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!ledger.length" description="暂无流水" :image-size="60" />
    </el-dialog>

    <el-dialog v-model="shiftDialog" title="开班" width="420px">
      <el-form :model="shiftForm" label-width="86px">
        <el-form-item label="门店">
          <el-select v-model="shiftForm.storeId" style="width:100%">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班次">
          <el-select v-model="shiftForm.shiftName" style="width:100%">
            <el-option label="早班" value="早班" />
            <el-option label="中班" value="中班" />
            <el-option label="晚班" value="晚班" />
          </el-select>
        </el-form-item>
        <el-form-item label="备用金"><el-input-number v-model="shiftForm.cashAmount" :min="0" :precision="2" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shiftDialog = false">取消</el-button>
        <el-button type="primary" @click="saveShift">开班</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { Download, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import { dateTime, money, payTypeMap, today } from '@/utils/format'

const tab = ref('settlements')
const overview = ref<any>({})
const settlements = ref<any[]>([])
const cashier = ref<any[]>([])
const deposits = ref<any[]>([])
const arList = ref<any[]>([])
const ledger = ref<any[]>([])
const shifts = ref<any[]>([])
const stores = ref<any[]>([])
const settleRange = ref<[string, string] | null>(null)
const arDialog = ref(false)
const tradeDialog = ref(false)
const ledgerDialog = ref(false)
const shiftDialog = ref(false)
const tradeRow = ref<any>(null)
const arForm = reactive<any>({ company: '', contact: '', phone: '' })
const tradeForm = reactive<any>({ type: 'CHARGE', amount: 0, remark: '' })
const shiftForm = reactive<any>({ storeId: undefined, shiftName: '早班', cashAmount: 1000 })

async function loadOverview() {
  const res: any = await http.get('/admin/finance/overview')
  overview.value = res.data || {}
}

async function loadSettlements() {
  const params: any = {}
  if (settleRange.value?.length) {
    params.startDate = settleRange.value[0]
    params.endDate = settleRange.value[1]
  }
  const res: any = await http.get('/admin/finance/settlements', { params })
  settlements.value = res.data || []
}

async function loadCashier() {
  const res: any = await http.get('/admin/finance/cashier')
  cashier.value = res.data || []
}

async function loadDeposits() {
  const res: any = await http.get('/admin/finance/deposits')
  deposits.value = res.data || []
}

async function loadAr() {
  const res: any = await http.get('/admin/finance/ar')
  arList.value = res.data || []
}

async function loadShifts() {
  const res: any = await http.get('/admin/finance/shifts')
  shifts.value = res.data || []
}

async function loadStores() {
  const res: any = await http.get('/admin/finance/stores')
  stores.value = res.data || []
}

function openAr() {
  Object.assign(arForm, { company: '', contact: '', phone: '' })
  arDialog.value = true
}

async function saveAr() {
  if (!arForm.company) return ElMessage.warning('请填写单位名称')
  await http.post('/admin/finance/ar', arForm)
  ElMessage.success('挂账单位已创建')
  arDialog.value = false
  loadAr()
}

function openTrade(row: any, type: string) {
  tradeRow.value = row
  Object.assign(tradeForm, { type, amount: 0, remark: '' })
  tradeDialog.value = true
}

async function saveTrade() {
  if (!tradeForm.amount) return ElMessage.warning('请输入金额')
  await http.post(`/admin/finance/ar/${tradeRow.value.id}/trade`, tradeForm)
  ElMessage.success(tradeForm.type === 'VERIFY' ? '核销成功' : '挂账成功')
  tradeDialog.value = false
  loadAr()
  loadOverview()
}

async function showLedger(row: any) {
  const res: any = await http.get(`/admin/finance/ar/${row.id}/ledger`)
  ledger.value = res.data || []
  ledgerDialog.value = true
}

function openShift() {
  Object.assign(shiftForm, { storeId: stores.value[0]?.id, shiftName: '早班', cashAmount: 1000 })
  shiftDialog.value = true
}

async function saveShift() {
  await http.post('/admin/finance/shifts/open', shiftForm)
  ElMessage.success('开班成功')
  shiftDialog.value = false
  loadShifts()
}

async function closeShift(row: any) {
  const { value } = await ElMessageBox.prompt('请输入交班现金金额', '交班', {
    inputValue: String(row.cashAmount || 0), confirmButtonText: '确认交班', cancelButtonText: '取消'
  })
  await http.post(`/admin/finance/shifts/${row.id}/close`, { cashAmount: Number(value || 0) })
  ElMessage.success('交班完成')
  loadShifts()
}

async function exportSummary() {
  const params: any = {}
  if (settleRange.value?.length) {
    params.startDate = settleRange.value[0]
    params.endDate = settleRange.value[1]
  }
  const blob: any = await http.get('/admin/reports/export', { params, responseType: 'blob' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `report-${today()}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

watch(tab, (v) => {
  if (v === 'cashier') loadCashier()
  if (v === 'deposits') loadDeposits()
  if (v === 'ar') loadAr()
  if (v === 'shifts') { loadShifts(); loadStores() }
})

onMounted(() => {
  loadOverview()
  loadSettlements()
  loadCashier()
  loadDeposits()
  loadAr()
  loadShifts()
  loadStores()
})
</script>
