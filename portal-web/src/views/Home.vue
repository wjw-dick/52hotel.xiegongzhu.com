<template>
  <div>
    <section class="hero">
      <div class="hero-inner">
        <div class="eyebrow">{{ t('portal.eyebrow', 'YUNQI RETREAT · HANGZHOU & MOGANSHAN') }}</div>
        <h1>{{ t('portal.hero', '把日子，安放在山水之间') }}</h1>
        <p>{{ t('portal.intro', '推窗见景，听风入眠。云栖以克制而温润的方式，把住宿变成一段值得回味的停留。') }}</p>
        <div class="hero-actions">
          <router-link class="btn light" :to="{ path: '/booking', query: search }">{{ t('booking.title', '立即预订') }}</router-link>
          <a class="btn ghost" href="#rooms">{{ t('portal.explore', '浏览房型') }}</a>
        </div>
      </div>
    </section>

    <div class="booking-strip">
      <div class="field">
        <label>入住日期</label>
        <input v-model="search.checkIn" type="date" :min="today" @change="ensureDates" />
      </div>
      <div class="field">
        <label>离店日期</label>
        <input v-model="search.checkOut" type="date" :min="search.checkIn" @change="ensureDates" />
      </div>
      <div class="field">
        <label>入住人数</label>
        <select v-model.number="search.guests">
          <option :value="1">1 位</option>
          <option :value="2">2 位</option>
          <option :value="3">3 位</option>
          <option :value="4">4 位</option>
        </select>
      </div>
      <div class="field">
        <label>意向门店</label>
        <select v-model="search.storeId">
          <option value="">不限门店</option>
          <option v-for="s in stores" :key="s.id" :value="String(s.id)">{{ s.name }}</option>
        </select>
      </div>
      <button class="btn" @click="goBooking">查询房态</button>
    </div>

    <div v-if="home" class="announce">
      <b>山居公告</b>
      <span>{{ home.announcement }}</span>
    </div>

    <section class="section" id="rooms">
      <div class="section-head">
        <div>
          <div class="kicker">ROOMS & SUITES</div>
          <h2>{{ t('portal.rooms', '房型甄选') }}</h2>
        </div>
        <p>{{ t('portal.roomsDesc', '从山景双床到亲子套房，每一间都保留了充足的呼吸感。价格为门市参考价，实际以预订日期房价为准。') }}</p>
      </div>

      <div v-if="loading" class="empty">正在为你准备房间…</div>
      <div v-else-if="error" class="empty">{{ error }}</div>
      <div v-else class="room-grid">
        <article v-for="room in filteredRooms" :key="room.id" class="room-card">
          <div class="room-cover">
            <img v-if="coverOf(room)" :src="coverOf(room)" :alt="room.name" loading="lazy" />
            <div v-else class="placeholder">◈</div>
            <span class="store-tag">{{ room.storeName }}</span>
          </div>
          <div class="room-body">
            <h3>{{ room.name }}</h3>
            <div class="room-meta">
              <span>{{ room.area }}㎡</span>
              <span>可住 {{ room.capacity }} 人</span>
              <span>{{ room.bedType }}</span>
            </div>
            <p class="room-intro">{{ room.intro }}</p>
            <div class="room-foot">
              <div class="price">¥{{ money(room.basePrice) }}<small>/ 晚起</small></div>
              <router-link :to="{ path: `/rooms/${room.id}`, query: search }">查看详情 →</router-link>
            </div>
          </div>
        </article>
      </div>
      <div v-if="!loading && !filteredRooms.length" class="empty">该门店暂无可售房型</div>
    </section>

    <section class="section" style="background:#fff;border-top:1px solid var(--line);border-bottom:1px solid var(--line)">
      <div class="section-head">
        <div>
          <div class="kicker">OUR HOUSES</div>
          <h2>{{ t('portal.stores', '两处山居') }}</h2>
        </div>
        <p>西湖之畔的庭院，莫干山中的竹海。不同的风景，同一种安住的方式。</p>
      </div>
      <div class="store-row">
        <article v-for="s in stores" :key="s.id" class="store-card">
          <div class="store-mark">栖</div>
          <div>
            <h3>{{ s.name }}</h3>
            <p>{{ s.city }} · {{ s.address }}</p>
            <p style="margin-top:8px">{{ s.intro }}</p>
            <p style="margin-top:8px;color:var(--brass)">电话 {{ s.phone }}</p>
            <router-link class="btn outline" style="margin-top:14px;padding:8px 16px" :to="{ path: '/booking', query: { ...search, storeId: String(s.id) } }">查看该店房型</router-link>
          </div>
        </article>
      </div>
    </section>

    <section class="section">
      <div class="section-head">
        <div>
          <div class="kicker">WHY YUNQI</div>
          <h2>在山里，被好好照顾</h2>
        </div>
      </div>
      <div class="store-row">
        <div class="store-card">
          <div class="store-mark">01</div>
          <div><h3>真实房态，即时确认</h3><p>在线选房与实时库存直连，提交后即锁定房间，不必反复电话确认。</p></div>
        </div>
        <div class="store-card">
          <div class="store-mark">02</div>
          <div><h3>会员权益，长住更划算</h3><p>注册会员累积积分与房晚，银卡、金卡、钻石会员享对应折扣与延迟退房。</p></div>
        </div>
        <div class="store-card">
          <div class="store-mark">03</div>
          <div><h3>合规入住，安心无忧</h3><p>入住登记与公安实名报送全流程留痕，隐私数据加密存储。</p></div>
        </div>
        <div class="store-card">
          <div class="store-mark">04</div>
          <div><h3>多端一致，随时管理</h3><p>小程序、H5 与 App 同步订单，订单查询与自助取消随时可办。</p></div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { loadHome, store, t } from '@/stores/portal'
import { addDays, money, today } from '@/utils'

const router = useRouter()
const loading = ref(true)
const error = ref('')
const home = computed(() => store.home)
const stores = computed(() => store.home?.stores || [])
const search = reactive({
  checkIn: today(),
  checkOut: addDays(today(), 2),
  guests: 2,
  storeId: ''
})

const filteredRooms = computed(() => {
  const list = store.home?.roomTypes || []
  return search.storeId ? list.filter((r: any) => String(r.storeId) === search.storeId) : list
})

function norm(u: string): string {
  if (!u) return ''
  const path = u.replace(/^https?:\/\/[^/]*\/uploads\//, '/uploads/')
  if (!path.includes('/uploads/')) return path
  return path + (path.includes('?') ? '&' : '?') + 'v=2'
}

function firstImage(v: any): string {
  if (!v) return ''
  if (Array.isArray(v)) return v.filter(Boolean)[0] ? norm(String(v.filter(Boolean)[0])) : ''
  if (typeof v === 'string') {
    const s = v.trim()
    if (!s || s === 'null') return ''
    try {
      const parsed = JSON.parse(s)
      if (Array.isArray(parsed)) {
        const first = parsed.filter(Boolean)[0]
        return first ? norm(String(first)) : ''
      }
    } catch {
      return s.startsWith('http') || s.startsWith('/uploads/') ? norm(s) : ''
    }
  }
  return ''
}

function coverOf(room: any): string {
  if (room?.cover) return norm(String(room.cover))
  return firstImage(room?.images)
}

function ensureDates() {
  if (search.checkOut <= search.checkIn) search.checkOut = addDays(search.checkIn, 1)
}

function goBooking() {
  ensureDates()
  router.push({ path: '/booking', query: { ...search } })
}

onMounted(async () => {
  try {
    const h = await loadHome()
    const cfg = h?.config || {}
    document.querySelector('meta[name="description"]')?.setAttribute('content',
      `${cfg.tenant_name || h?.tenant?.name || '云栖民宿'}官方预订，${cfg.slogan || '山水之间，安心住下'}。支持实时房态、在线选房与会员权益。`)
  } catch (e: any) {
    error.value = e.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
})
</script>
