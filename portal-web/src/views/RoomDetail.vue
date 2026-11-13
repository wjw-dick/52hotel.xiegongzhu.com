<template>
  <div>
    <section class="page-hero">
      <div style="max-width:1080px;margin:0 auto">
        <h1>{{ data.roomType?.name || '房型详情' }}</h1>
        <p>{{ data.roomType?.storeName }} · {{ data.roomType?.area }}㎡ · 可住 {{ data.roomType?.capacity }} 人 · {{ data.roomType?.bedType }}</p>
      </div>
    </section>

    <div class="page-body">
      <div v-if="loading" class="empty">正在加载房型…</div>
      <div v-else-if="error" class="empty">{{ error }}</div>
      <template v-else>
        <div class="split-2">
          <div>
            <div class="panel">
              <div class="gallery">
              <div class="gallery-main" :class="{ placeholder: !gallery.length }">
                <img v-if="gallery.length" :src="gallery[activeImg]" :alt="data.roomType?.name" @click="openViewer(activeImg)" />
                <span v-else>◈</span>
                <template v-if="gallery.length > 1">
                  <button type="button" class="gallery-arrow left" @click="shiftImg(-1)">‹</button>
                  <button type="button" class="gallery-arrow right" @click="shiftImg(1)">›</button>
                  <span class="gallery-counter">{{ activeImg + 1 }} / {{ gallery.length }}</span>
                </template>
              </div>
              <div v-if="gallery.length" class="gallery-thumbs">
                <button
                  v-for="(g, i) in gallery"
                  :key="g + i"
                  type="button"
                  class="gallery-thumb"
                  :class="{ active: i === activeImg }"
                  @click="activeImg = i"
                >
                  <img :src="g" :alt="`${data.roomType?.name} 图 ${i + 1}`" />
                </button>
              </div>
            </div>
              <h2 style="margin-top:24px">{{ data.roomType?.name }}</h2>
              <p style="color:#69766e;line-height:1.9;font-family:'PingFang SC',sans-serif;font-size:13.5px">{{ data.roomType?.intro }}</p>
              <div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:14px">
                <span v-for="f in data.roomType?.facilities || []" :key="f" class="tag">{{ f }}</span>
              </div>
            </div>

            <div class="panel">
              <h2>未来 30 天房价</h2>
              <p class="hint" style="margin-top:-8px;margin-bottom:16px">绿色为空房可订；灰显日期暂无可售房量。实际价格以选择日期结算为准。</p>
              <div class="calendar">
                <div v-for="d in data.calendar || []" :key="d.date" class="day" :class="{ off: !d.available }">
                  <div class="d">{{ d.date.slice(5) }}</div>
                  <div class="p">¥{{ money(d.price) }}</div>
                </div>
              </div>
            </div>

            <div class="panel">
              <h2>预订须知</h2>
              <p class="hint" style="line-height:2">
                1. 入住时间 14:00 后，退房时间 12:00 前；如需延迟退房请联系门店。<br />
                2. 到店需出示所有入住人有效身份证件，并配合实名登记与公安报送。<br />
                3. 免费取消时限以订单确认信息为准，逾期取消可能收取首晚房费。<br />
                4. 儿童与加床政策请提前与门店确认；未成年人入住需监护人陪同。
              </p>
            </div>
          </div>

          <aside class="summary-box">
            <h3>选择入住日期</h3>
            <div class="summary-line"><span>参考房价</span><b>¥{{ money(data.roomType?.basePrice) }} 起 / 晚</b></div>
            <div class="form-field" style="margin-top:16px">
              <label style="color:#cbd7d0">入住</label>
              <input v-model="form.checkIn" type="date" :min="today" @change="ensureDates" />
            </div>
            <div class="form-field" style="margin-top:12px">
              <label style="color:#cbd7d0">离店</label>
              <input v-model="form.checkOut" type="date" :min="form.checkIn" @change="ensureDates" />
            </div>
            <div class="summary-total">
              <span>共计 {{ nights }} 晚</span>
              <b>¥{{ money(total) }}</b>
            </div>
            <div class="hint" style="color:#cbd7d0;margin-bottom:16px" v-if="availability !== null">
              当前可订 {{ availability }} 间
            </div>
            <button class="btn light" :disabled="!canBook" @click="goBook">去预订</button>
            <p class="hint" style="color:#9fb2a6;margin:12px 0 0">提交订单后将锁定房间，请保持手机畅通。</p>
          </aside>
        </div>
      </template>
    </div>

    <div v-if="viewerOpen" class="viewer" @click.self="viewerOpen = false">
      <button type="button" class="viewer-close" @click="viewerOpen = false">×</button>
      <img :src="gallery[viewerIndex]" :alt="data.roomType?.name" />
      <button v-if="gallery.length > 1" type="button" class="viewer-arrow left" @click="viewerShift(-1)">‹</button>
      <button v-if="gallery.length > 1" type="button" class="viewer-arrow right" @click="viewerShift(1)">›</button>
      <div class="viewer-counter">{{ viewerIndex + 1 }} / {{ gallery.length }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '@/api/http'
import { loadHome, store } from '@/stores/portal'
import { addDays, money, today } from '@/utils'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref('')
const data = ref<any>({})
const availability = ref<number | null>(null)
const form = reactive({ checkIn: String(route.query.checkIn || today()), checkOut: String(route.query.checkOut || addDays(today(), 2)) })

const nights = computed(() => {
  const a = new Date(form.checkIn + 'T00:00:00').getTime()
  const b = new Date(form.checkOut + 'T00:00:00').getTime()
  return Math.max(1, Math.round((b - a) / 86400000))
})
const total = computed(() => Number(data.value.roomType?.basePrice || 0) * nights.value)
const canBook = computed(() => availability.value !== null && availability.value > 0)

const activeImg = ref(0)
const viewerOpen = ref(false)
const viewerIndex = ref(0)

function norm(u: string): string {
  if (!u) return ''
  const path = u.replace(/^https?:\/\/[^/]*\/uploads\//, '/uploads/')
  if (!path.includes('/uploads/')) return path
  return path + (path.includes('?') ? '&' : '?') + 'v=2'
}

function toList(v: any): string[] {
  if (!v) return []
  if (Array.isArray(v)) return v.filter(Boolean).map(x => norm(String(x)))
  if (typeof v === 'string') {
    const s = v.trim()
    if (!s || s === 'null') return []
    try {
      const parsed = JSON.parse(s)
      if (Array.isArray(parsed)) return parsed.filter(Boolean).map(x => norm(String(x)))
    } catch {
      return s.split(',').map(x => norm(x.trim())).filter(Boolean)
    }
  }
  return []
}

const gallery = computed(() => {
  const rt = data.value.roomType || {}
  const imgs = toList(rt.images)
  if (imgs.length) return imgs
  const cover = rt.cover ? String(rt.cover) : ''
  return cover ? [cover] : []
})
function shiftImg(step: number) {
  if (!gallery.value.length) return
  activeImg.value = (activeImg.value + step + gallery.value.length) % gallery.value.length
}
function openViewer(index: number) {
  viewerIndex.value = index
  viewerOpen.value = true
}
function viewerShift(step: number) {
  if (!gallery.value.length) return
  viewerIndex.value = (viewerIndex.value + step + gallery.value.length) % gallery.value.length
}
watch(gallery, () => { activeImg.value = 0 })

function ensureDates() {
  if (form.checkOut <= form.checkIn) form.checkOut = addDays(form.checkIn, 1)
}

async function loadAvailability() {
  if (form.checkOut <= form.checkIn) return
  try {
    const res: any = await http.get(`/public/${store.tenantCode}/availability`, {
      params: { roomTypeId: route.params.id, checkIn: form.checkIn, checkOut: form.checkOut }
    })
    availability.value = res.data?.rooms?.length || 0
    if (res.data?.price) data.value.roomType.basePrice = res.data.price
  } catch {
    availability.value = 0
  }
}

function goBook() {
  router.push({ path: '/booking', query: { roomTypeId: String(route.params.id), checkIn: form.checkIn, checkOut: form.checkOut } })
}

watch(() => [form.checkIn, form.checkOut], loadAvailability)

onMounted(async () => {
  try {
    await loadHome()
    const res: any = await http.get(`/public/${store.tenantCode}/room-types/${route.params.id}`)
    data.value = res.data || {}
    await loadAvailability()
  } catch (e: any) {
    error.value = e.message || '房型加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.gallery-main {
  position: relative;
  aspect-ratio: 16 / 7;
  background: linear-gradient(135deg, #dce7df, #adc4b7);
  display: grid;
  place-items: center;
  color: rgba(36, 72, 60, .45);
  font-size: 52px;
  overflow: hidden;
  border-radius: 8px;
}
.gallery-main img { width: 100%; height: 100%; object-fit: cover; }
.gallery-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, .85);
  color: #24483c;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  display: grid;
  place-items: center;
}
.gallery-arrow.left { left: 12px; }
.gallery-arrow.right { right: 12px; }
.gallery-counter {
  position: absolute;
  right: 12px;
  bottom: 12px;
  padding: 3px 10px;
  border-radius: 14px;
  background: rgba(15, 30, 24, .62);
  color: #fff;
  font-size: 12px;
  letter-spacing: .5px;
}
.gallery-thumbs {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(88px, 1fr));
  gap: 10px;
  margin-top: 12px;
}
.gallery-thumb {
  padding: 0;
  border: 2px solid transparent;
  border-radius: 8px;
  overflow: hidden;
  background: transparent;
  cursor: pointer;
  line-height: 0;
  transition: border-color .18s ease, transform .18s ease;
}
.gallery-thumb img { width: 100%; aspect-ratio: 4 / 3; object-fit: cover; display: block; border-radius: 6px; }
.gallery-thumb:hover { transform: translateY(-1px); }
.gallery-thumb.active { border-color: #24483c; }
.viewer {
  position: fixed;
  inset: 0;
  z-index: 900;
  background: rgba(10, 18, 14, .9);
  display: grid;
  place-items: center;
}
.viewer img { max-width: min(1100px, 92vw); max-height: 86vh; object-fit: contain; border-radius: 6px; }
.viewer-close {
  position: absolute;
  top: 24px;
  right: 28px;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, .88);
  color: #24483c;
  font-size: 22px;
  cursor: pointer;
}
.viewer-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, .82);
  color: #24483c;
  font-size: 24px;
  cursor: pointer;
}
.viewer-arrow.left { left: 3vw; }
.viewer-arrow.right { right: 3vw; }
.viewer-counter {
  position: absolute;
  bottom: 26px;
  left: 50%;
  transform: translateX(-50%);
  color: #dbe6df;
  font-size: 13px;
  letter-spacing: 1px;
}
</style>
