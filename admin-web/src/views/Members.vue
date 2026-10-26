<template>
  <div class="page">
    <h2 class="page-title">会员营销</h2>
    <p class="page-sub">会员档案、等级权益、积分与优惠券管理</p>

    <el-tabs v-model="tab" class="card">
      <el-tab-pane label="会员列表" name="members">
        <div class="toolbar">
          <el-input v-model="query.keyword" placeholder="姓名 / 手机 / UnionId" clearable style="width:220px" @keyup.enter="load" />
          <el-select v-model="query.levelId" clearable placeholder="全部等级" style="width:150px" @change="load">
            <el-option v-for="l in levels" :key="l.id" :label="l.name" :value="l.id" />
          </el-select>
          <el-button type="primary" @click="load">查询</el-button>
          <div class="grow"></div>
          <el-button :icon="Refresh" @click="refreshLevels">刷新等级</el-button>
          <el-button type="primary" :icon="Plus" @click="openMember()">新增会员</el-button>
        </div>
        <el-table v-loading="loading" :data="members">
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="phone" label="手机号" width="125" />
          <el-table-column label="等级" width="120">
            <template #default="{ row }">
              <el-tag size="small" effect="plain" :type="row.levelId === 3 ? 'warning' : row.levelId === 2 ? 'primary' : 'info'">{{ row.levelName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="points" label="积分" width="90" align="right" />
          <el-table-column label="储值余额" width="110" align="right">
            <template #default="{ row }">¥{{ money(row.balance) }}</template>
          </el-table-column>
          <el-table-column label="累计消费" width="120" align="right">
            <template #default="{ row }">¥{{ money(row.totalAmount) }}</template>
          </el-table-column>
          <el-table-column prop="tags" label="标签" min-width="150">
            <template #default="{ row }">
              <el-tag v-for="t in (row.tags || '').split(',').filter(Boolean)" :key="t" size="small" style="margin-right:4px">{{ t }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="入会时间" width="160">
            <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button link type="primary" @click="openMember(row)">编辑</el-button>
              <el-button link type="success" @click="openPoints(row)">积分</el-button>
              <el-button link type="warning" @click="openCoupon(row)">发券</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="会员等级" name="levels">
        <el-table :data="levels">
          <el-table-column prop="name" label="等级名称" width="150" />
          <el-table-column label="升级门槛" width="140">
            <template #default="{ row }">累计消费 ¥{{ money(row.minAmount) }}</template>
          </el-table-column>
          <el-table-column label="折扣" width="100">
            <template #default="{ row }">{{ Number(row.discount) * 10 }} 折</template>
          </el-table-column>
          <el-table-column prop="benefits" label="权益" min-width="240" />
          <el-table-column prop="sort" label="排序" width="80" align="center" />
        </el-table>
        <p class="muted" style="margin-top:12px">等级规则由平台初始化模板提供；租户可在数据库升级后扩展等级编辑能力。</p>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="memberDialog" :title="memberForm.id ? '编辑会员' : '新增会员'" width="520px">
      <el-form :model="memberForm" label-width="86px" class="form-grid">
        <el-form-item label="姓名" required><el-input v-model="memberForm.name" /></el-form-item>
        <el-form-item label="手机号" required><el-input v-model="memberForm.phone" /></el-form-item>
        <el-form-item label="性别">
          <el-select v-model="memberForm.gender" style="width:100%">
            <el-option label="男" value="M" />
            <el-option label="女" value="F" />
            <el-option label="未知" value="U" />
          </el-select>
        </el-form-item>
        <el-form-item label="会员等级">
          <el-select v-model="memberForm.levelId" style="width:100%">
            <el-option v-for="l in levels" :key="l.id" :label="l.name" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签" class="full"><el-input v-model="memberForm.tags" placeholder="逗号分隔，如 高价值,商务" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="memberForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">冻结</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="memberDialog = false">取消</el-button>
        <el-button type="primary" @click="saveMember">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pointDialog" title="积分调整" width="420px">
      <el-form label-width="86px">
        <el-form-item label="会员"><el-input :model-value="current?.name" disabled /></el-form-item>
        <el-form-item label="变动值" required>
          <el-input-number v-model="pointForm.changeValue" :min="-99999" :max="99999" style="width:100%" />
          <div class="muted">正数增加，负数扣减</div>
        </el-form-item>
        <el-form-item label="原因"><el-input v-model="pointForm.reason" placeholder="如 活动赠送" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pointDialog = false">取消</el-button>
        <el-button type="primary" @click="savePoints">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="couponDialog" title="发放优惠券" width="440px">
      <el-form label-width="86px">
        <el-form-item label="会员"><el-input :model-value="current?.name" disabled /></el-form-item>
        <el-form-item label="券名称" required><el-input v-model="couponForm.name" placeholder="如 满 500 减 50" /></el-form-item>
        <el-form-item label="面额"><el-input-number v-model="couponForm.amount" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="有效期"><el-date-picker v-model="couponForm.expireDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="couponDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCoupon">发放</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="会员详情" size="600px">
      <template v-if="detail.member">
        <div class="member-head">
          <div class="avatar">{{ detail.member.name?.slice(0, 1) }}</div>
          <div>
            <h3 style="margin:0">{{ detail.member.name }} <el-tag size="small" effect="plain">{{ detail.member.levelName || levelName(detail.member.levelId) }}</el-tag></h3>
            <p class="muted" style="margin:4px 0 0">{{ detail.member.phone }} · 入会 {{ dateTime(detail.member.createTime) }}</p>
          </div>
        </div>
        <div class="stat-grid" style="grid-template-columns:repeat(3,1fr);margin-top:16px">
          <div class="stat-card"><div class="stat-label">积分</div><div class="stat-value" style="font-size:22px">{{ detail.member.points }}</div></div>
          <div class="stat-card"><div class="stat-label">储值</div><div class="stat-value" style="font-size:22px">¥{{ money(detail.member.balance) }}</div></div>
          <div class="stat-card"><div class="stat-label">累计消费</div><div class="stat-value" style="font-size:22px">¥{{ money(detail.member.totalAmount) }}</div></div>
        </div>

        <div class="section-title" style="margin-top:20px">最近订单</div>
        <el-table :data="detail.orders || []" size="small">
          <el-table-column prop="orderNo" label="订单号" width="160" />
          <el-table-column prop="checkIn" label="入住" width="110" />
          <el-table-column label="金额" width="100" align="right">
            <template #default="{ row }">¥{{ money(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" />
        </el-table>

        <div class="section-title" style="margin-top:18px">积分流水</div>
        <el-table :data="detail.points || []" size="small">
          <el-table-column label="变动" width="100">
            <template #default="{ row }">
              <span :style="{ color: row.changeValue > 0 ? '#11875d' : '#d33' }">{{ row.changeValue > 0 ? '+' : '' }}{{ row.changeValue }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="140" />
          <el-table-column prop="createTime" label="时间" width="150">
            <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>

        <div class="section-title" style="margin-top:18px">优惠券</div>
        <el-table :data="detail.coupons || []" size="small">
          <el-table-column prop="name" label="名称" min-width="140" />
          <el-table-column label="面额" width="100">
            <template #default="{ row }">¥{{ money(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="expireDate" label="有效期" width="110" />
          <el-table-column prop="status" label="状态" width="90" />
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '@/api/http'
import { dateTime, money } from '@/utils/format'

const tab = ref('members')
const loading = ref(false)
const members = ref<any[]>([])
const levels = ref<any[]>([])
const query = reactive<any>({ keyword: '', levelId: undefined })
const memberDialog = ref(false)
const pointDialog = ref(false)
const couponDialog = ref(false)
const detailVisible = ref(false)
const detail = ref<any>({})
const current = ref<any>(null)
const memberForm = reactive<any>({})
const pointForm = reactive<any>({ changeValue: 100, reason: '' })
const couponForm = reactive<any>({ name: '', amount: 50, expireDate: '' })

function levelName(id: number) {
  return levels.value.find(l => l.id === id)?.name || '普通会员'
}

async function load() {
  loading.value = true
  try {
    const res: any = await http.get('/admin/members', { params: query })
    members.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function loadLevels() {
  const res: any = await http.get('/admin/members/levels')
  levels.value = res.data || []
}

function openMember(row?: any) {
  Object.assign(memberForm, { id: undefined, name: '', phone: '', gender: 'U', levelId: levels.value[0]?.id, tags: '', status: 1 })
  if (row) Object.assign(memberForm, row)
  memberDialog.value = true
}

async function saveMember() {
  if (!memberForm.name || !memberForm.phone) return ElMessage.warning('请填写姓名与手机号')
  if (memberForm.id) await http.put(`/admin/members/${memberForm.id}`, memberForm)
  else await http.post('/admin/members', memberForm)
  ElMessage.success('保存成功')
  memberDialog.value = false
  load()
}

function openPoints(row: any) {
  current.value = row
  Object.assign(pointForm, { changeValue: 100, reason: '' })
  pointDialog.value = true
}

async function savePoints() {
  if (!pointForm.changeValue) return ElMessage.warning('变动值不能为 0')
  await http.post(`/admin/members/${current.value.id}/points`, pointForm)
  ElMessage.success('积分已调整')
  pointDialog.value = false
  load()
}

function openCoupon(row: any) {
  current.value = row
  Object.assign(couponForm, { name: '', amount: 50, expireDate: '' })
  couponDialog.value = true
}

async function saveCoupon() {
  if (!couponForm.name) return ElMessage.warning('请填写券名称')
  await http.post(`/admin/members/${current.value.id}/coupons`, couponForm)
  ElMessage.success('优惠券已发放')
  couponDialog.value = false
}

async function openDetail(row: any) {
  const res: any = await http.get(`/admin/members/${row.id}/detail`)
  detail.value = res.data || {}
  detailVisible.value = true
}

async function refreshLevels() {
  const res: any = await http.get('/admin/members/levels/refresh')
  ElMessage.success(`等级刷新完成：${res.data?.upgraded || 0} 位会员升级`)
  load()
}

watch(tab, (v) => { if (v === 'levels') loadLevels() })

onMounted(() => {
  load()
  loadLevels()
})
</script>

<style scoped>
.member-head { display: flex; gap: 14px; align-items: center; }
.member-head .avatar { width: 52px; height: 52px; border-radius: 50%; background: linear-gradient(145deg,#4d9bff,#1f6feb); color: #fff; display: grid; place-items: center; font-size: 20px; font-weight: 700; }
</style>
