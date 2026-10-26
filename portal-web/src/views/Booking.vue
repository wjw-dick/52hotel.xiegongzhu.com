<template>
  <div>
    <section class="page-hero">
      <div style="max-width:1080px;margin:0 auto">
        <h1>在线预订</h1>
        <p>选择房型与日期，填写入住人信息，提交后即锁定房间</p>
      </div>
    </section>

    <div class="page-body">
      <div v-if="success" class="panel" style="text-align:center;padding:56px 24px">
        <div style="font-size:46px;color:var(--pine)">✓</div>
        <h2 style="margin:14px 0 8px">预订成功</h2>
        <p class="hint">订单号 <b style="color:var(--pine);font-size:16px">{{ success.orderNo }}</b> · 请凭订单号与手机号查询</p>
        <p class="hint">{{ success.checkIn }} 入住，{{ success.checkOut }} 离店，共 ¥{{ money(success.amount) }}</p>
        <div style="display:flex;gap:12px;justify-content:center;margin-top:24px;flex-wrap:wrap">
          <router-link class="btn" :to="{ path: '/query', query: { orderNo: success.orderNo, phone: form.guestPhone } }">查看订单</router-link>
          <router-link class="btn outline" to="/">返回首页</router-link>
        </div>
      </div>

      <div v-else class="split-2">
        <div class="panel">
          <h2>预订信息</h2>
          <div class="form-grid">
            <div class="form-field">
              <label>门店</label>
              <select v-model="form.storeId" @change="onStoreChange">
                <option value="">请选择门店</option>
                <option v-for="s in stores" :key="s.id" :value="String(s.id)">{{ s.name }}</option>
              </select>
            </div>
            <div class="form-field">
              <label>房型</label>
              <select v-model="form.roomTypeId" @change="checkAvailability">
                <option value="">请选择房型</option>
                <option v-for="r in filteredTypes" :key="r.id" :value="String(r.id)">{{ r.name }} · ¥{{ money(r.basePrice) }} 起</option>
              </select>
            </div>
            <div class="form-field">
              <label>入住日期</label>
              <input v-model="form.checkIn" type="date" :min="today" @change="onDateChange" />
            </div>
            <div class="form-field">
              <label>离店日期</label>
              <input v-model="form.checkOut" type="date" :min="form.checkIn" @change="onDateChange" />
            </div>
            <div class="form-field">
              <label>房间数</label>
              <select v-model.number="form.roomCount">
                <option :value="1">1 间</option>
                <option :value="2">2 间</option>
                <option :value="3">3 间</option>
              </select>
            </div>
            <div class="form-field">
              <label>支付方式</label>
              <select v-model="form.payType">
                <option value="ARRIVAL">到店支付</option>
                <option value="ONLINE">在线支付（演示）</option>
              </select>
            </div>
          </div>

          <h3>入住人信息</h3>
          <div class="form-grid">
            <div class="form-field">
              <label>姓名</label>
              <input v-model="form.guestName" placeholder="请填写真实姓名" />
            </div>
            <div class="form-field">
              <label>手机号</label>
              <input v-model="form.guestPhone" inputmode="tel" placeholder="用于接收确认信息" />
            </div>
            <div class="form-field full">
              <label>身份证号</label>
              <input v-model="form.idNo" placeholder="入住登记与公安报送需要，加密存储" />
            </div>
            <div class="form-field">
              <label>协议单位（选填）</label>
              <input v-model="form.company" placeholder="企业协议客户可填写" />
            </div>
            <div class="form-field">
              <label>预计到店</label>
              <select v-model="arrival">
                <option value="14:00 - 16:00">14:00 - 16:00</option>
                <option value="16:00 - 18:00">16:00 - 18:00</option>
                <option value="18:00 - 20:00">18:00 - 20:00</option>
                <option value="20:00 之后">20:00 之后</option>
              </select>
            </div>
            <div class="form-field full">
              <label>备注</label>
              <textarea v-model="form.remark" rows="3" placeholder="如高楼层、安静房间、亲子需求等"></textarea>
            </div>
          </div>

          <p v-if="error" class="error">{{ error }}</p>
          <p class="hint" style="margin-top:14px">提交即表示同意《入住须知》与《隐私政策》。我们将仅将证件信息用于入住登记与合规报送。</p>
        </div>

        <aside class="summary-box">
          <h3>订单摘要</h3>
          <div class="summary-line"><span>房型</span><b>{{ selectedType?.name || '待选择' }}</b></div>
          <div class="summary-line"><span>门店</span><b>{{ selectedStore?.name || '待选择' }}</b></div>
          <div class="summary-line"><span>日期</span><b>{{ form.checkIn }} → {{ form.checkOut }}</b></div>
          <div class="summary-line"><span>间夜</span><b>{{ nights }} 晚 · {{ form.roomCount }} 间</b></div>
          <div class="summary-line"><span>可订房量</span><b>{{ available === null ? '待查询' : available + ' 间' }}</b></div>
          <div class="summary-line"><span>参考单价</span><b>¥{{ money(unitPrice) }} / 晚</b></div>
          <div class="summary-total">
            <span>预计总价</span>
            <b>¥{{ money(total) }}</b>
          </div>
          <p class="hint" style="color:#9fb2a6;margin-top:0">最终金额以酒店确认及实际入住天数为准。</p>
          <button class="btn light" :disabled="submitting" @click="submit">{{ submitting ? '提交中…' : '提交预订' }}</button>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '@/api/http'
import { loadHome, store } from '@/stores/portal'
import { addDays, money, today } from '@/utils'

const route = useRoute()
const submitting = ref(false)
const success = ref<any>(null)
const error = ref('')
const available = ref<number | null>(null)
const unitPrice = ref(0)
const arrival = ref('14:00 - 16:00')
const form = reactive<any>({
  storeId: String(route.query.storeId || ''),
  roomTypeId: String(route.query.roomTypeId || ''),
  checkIn: String(route.query.checkIn || today()),
  checkOut: String(route.query.checkOut || addDays(today(), 2)),
  roomCount: 1,
  payType: 'ARRIVAL',
  guestName: '',
  guestPhone: '',
  idNo: '',
  company: '',
  remark: '',
  source: 'ONLINE'
})

const stores = computed(() => store.home?.stores || [])
const selectedStore = computed(() => stores.value.find((s: any) => String(s.id) === form.storeId))
const selectedType = computed(() => (store.home?.roomTypes || []).find((r: any) => String(r.id) === form.roomTypeId))
const filteredTypes = computed(() => {
  const list = store.home?.roomTypes || []
  return form.storeId ? list.filter((r: any) => String(r.storeId) === form.storeId) : list
})
const nights = computed(() => {
  const a = new Date(form.checkIn + 'T00:00:00').getTime()
  const b = new Date(form.checkOut + 'T00:00:00').getTime()
  return Math.max(1, Math.round((b - a) / 86400000))
})
const total = computed(() => unitPrice.value * nights.value * Number(form.roomCount || 1))

function onStoreChange() {
  if (selectedType.value && String(selectedType.value.storeId) !== form.storeId) form.roomTypeId = ''
  available.value = null
  checkAvailability()
}

function onDateChange() {
  if (form.checkOut <= form.checkIn) form.checkOut = addDays(form.checkIn, 1)
  checkAvailability()
}

async function checkAvailability() {
  unitPrice.value = Number(selectedType.value?.basePrice || 0)
  if (!form.roomTypeId || !form.checkIn || !form.checkOut) return
  try {
    const res: any = await http.get(`/public/${store.tenantCode}/availability`, {
      params: { roomTypeId: form.roomTypeId, checkIn: form.checkIn, checkOut: form.checkOut }
    })
    available.value = res.data?.rooms?.length || 0
    if (res.data?.price) unitPrice.value = Number(res.data.price)
  } catch {
    available.value = 0
  }
}

async function submit() {
  error.value = ''
  if (!form.roomTypeId) return error.value = '请选择房型'
  if (!form.guestName.trim()) return error.value = '请填写入住人姓名'
  if (!/^1\d{10}$/.test(form.guestPhone)) return error.value = '请填写正确的 11 位手机号'
  if (!form.idNo || form.idNo.trim().length < 15) return error.value = '请填写有效身份证号'
  if (form.checkOut <= form.checkIn) return error.value = '离店日期必须晚于入住日期'
  submitting.value = true
  try {
    const payload = {
      ...form,
      storeId: Number(form.storeId || selectedType.value?.storeId),
      roomTypeId: Number(form.roomTypeId),
      roomId: null,
      remark: `${form.remark || ''}${form.remark ? '；' : ''}预计到店 ${arrival.value}`.trim(),
      channel: 'H5'
    }
    const res: any = await http.post(`/public/${store.tenantCode}/orders`, payload, {
      headers: { 'X-Request-Id': `h5-${Date.now()}-${Math.random().toString(36).slice(2, 8)}` }
    })
    success.value = res.data
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (e: any) {
    error.value = e.message || '预订失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadHome()
  if (!form.storeId && selectedType.value) form.storeId = String(selectedType.value.storeId)
  await checkAvailability()
})
</script>
