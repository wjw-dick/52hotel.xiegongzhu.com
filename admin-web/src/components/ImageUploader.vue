<template>
  <div class="iu">
    <div class="iu__head">
      <span class="iu__count">{{ urls.length }}<i v-if="multiple">/{{ MAX_COUNT }}</i></span>
      <span class="iu__hint">JPG / PNG / WebP / GIF，单张 ≤ 5MB</span>
    </div>

    <div class="iu__grid">
      <!-- 已选图片卡片 -->
      <div v-for="(url, i) in urls" :key="url + i" class="iu__card">
        <img :src="url" class="iu__img" :alt="`图片${i + 1}`" loading="lazy" />
        <span v-if="!multiple || i === 0" class="iu__badge">{{ multiple ? '封面' : '已上传' }}</span>
        <div class="iu__mask">
          <button type="button" class="iu__act" title="查看大图" @click="openPreview(url)">
            <el-icon><ZoomIn /></el-icon>
          </button>
          <button type="button" class="iu__act iu__act--danger" title="删除" @click="removeAt(i)">
            <el-icon><Delete /></el-icon>
          </button>
        </div>
      </div>

      <!-- 上传中占位 -->
      <div v-for="n in uploading" :key="'up' + n" class="iu__card iu__card--loading">
        <div class="iu__spin"></div>
        <span class="iu__loading-text">上传中</span>
      </div>

      <!-- 新增入口 -->
      <el-upload
        v-if="canAdd"
        :show-file-list="false"
        :multiple="multiple"
        :limit="MAX_COUNT"
        accept="image/jpeg,image/png,image/webp,image/gif"
        :http-request="doUpload"
        :before-upload="beforeUpload"
        :on-progress="onProgress"
        :on-success="onDone"
        :on-error="onDone"
        class="iu__add"
        drag
      >
        <div class="iu__add-tile">
          <el-icon class="iu__add-icon"><Plus /></el-icon>
          <span class="iu__add-text">上传图片</span>
          <span v-if="multiple" class="iu__add-sub">还可添加 {{ MAX_COUNT - urls.length }} 张</span>
        </div>
      </el-upload>
    </div>

    <el-dialog v-model="previewVisible" title="图片预览" width="720px" append-to-body class="iu__dialog">
      <img :src="previewUrl" alt="预览" class="iu__preview" />
      <template #footer>
        <span class="iu__preview-meta">{{ previewUrl }}</span>
        <el-button @click="copyUrl">复制地址</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete, ZoomIn } from '@element-plus/icons-vue'
import http from '@/api/http'

const props = withDefaults(defineProps<{
  modelValue: string | string[]
  multiple?: boolean
  category?: string
}>(), {
  multiple: false,
  category: 'common'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | string[]): void
}>()

const MAX_SIZE = 5 * 1024 * 1024
const MAX_COUNT = 9
const ALLOWED = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

const urls = computed<string[]>(() => {
  const v: string | string[] = props.modelValue
  if (props.multiple) return Array.isArray(v) ? v : []
  return typeof v === 'string' && v ? [v] : []
})

const canAdd = computed(() => urls.value.length + uploading.value < (props.multiple ? MAX_COUNT : 1))

const previewVisible = ref(false)
const previewUrl = ref('')
const uploading = ref(0)

function beforeUpload(file: File): boolean {
  if (!ALLOWED.includes(file.type)) {
    ElMessage.warning('仅支持 JPG / PNG / WebP / GIF 格式图片')
    return false
  }
  if (file.size > MAX_SIZE) {
    ElMessage.warning('图片大小不能超过 5MB')
    return false
  }
  if (urls.value.length + uploading.value >= (props.multiple ? MAX_COUNT : 1)) {
    ElMessage.warning(`最多只能上传 ${props.multiple ? MAX_COUNT : 1} 张图片`)
    return false
  }
  uploading.value += 1
  return true
}

function onProgress() {
  // 保持占位卡片，等待上传结束
}

function onDone() {
  uploading.value = Math.max(0, uploading.value - 1)
}

function emitUrls(next: string[]) {
  emit('update:modelValue', props.multiple ? next : (next[0] || ''))
}

async function doUpload(options: any) {
  const fd = new FormData()
  fd.append('file', options.file)
  fd.append('category', props.category)
  try {
    const res: any = await http.post('/admin/files/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const url: string = res.data?.url || ''
    if (!url) throw new Error('上传返回为空')
    const next = props.multiple ? [...urls.value, url] : [url]
    emitUrls(next)
    ElMessage.success('图片已上传')
    options.onSuccess?.(res.data)
  } catch (e: any) {
    ElMessage.error('图片上传失败，请重试')
    options.onError?.(e)
  }
}

function removeAt(index: number) {
  emitUrls(urls.value.filter((_, i) => i !== index))
}

function openPreview(url: string) {
  previewUrl.value = url
  previewVisible.value = true
}

async function copyUrl() {
  try {
    await navigator.clipboard.writeText(previewUrl.value)
    ElMessage.success('地址已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择文本')
  }
}
</script>

<style scoped>
.iu {
  --iu-border: #e3e6eb;
  --iu-primary: #2f6feb;
  width: 100%;
}

.iu__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.iu__count {
  font-size: 12px;
  font-weight: 600;
  color: #2f6feb;
  background: #eef4ff;
  border-radius: 20px;
  padding: 2px 10px;
}

.iu__count i {
  font-style: normal;
  color: #6b7a90;
  font-weight: 500;
}

.iu__hint {
  font-size: 12px;
  color: #8b94a3;
}

.iu__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(122px, 1fr));
  gap: 12px;
}

.iu__card {
  position: relative;
  aspect-ratio: 4 / 3;
  border-radius: 10px;
  overflow: hidden;
  background: #f5f7fa;
  border: 1px solid var(--iu-border);
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
  transition: box-shadow 0.18s ease, transform 0.18s ease;
}

.iu__card:hover {
  box-shadow: 0 6px 18px rgba(16, 24, 40, 0.12);
  transform: translateY(-1px);
}

.iu__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.iu__badge {
  position: absolute;
  top: 6px;
  left: 6px;
  font-size: 11px;
  line-height: 1;
  color: #fff;
  background: rgba(47, 111, 235, 0.85);
  backdrop-filter: blur(4px);
  border-radius: 6px;
  padding: 3px 7px;
}

.iu__mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.06), rgba(15, 23, 42, 0.62));
  opacity: 0;
  transition: opacity 0.18s ease;
}

.iu__card:hover .iu__mask {
  opacity: 1;
}

.iu__act {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  color: #334155;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.iu__act:hover {
  background: #fff;
  color: #2f6feb;
}

.iu__act--danger:hover {
  color: #e5484d;
}

.iu__card--loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-style: dashed;
  color: #8b94a3;
  font-size: 12px;
}

.iu__spin {
  width: 20px;
  height: 20px;
  border: 2px solid #dfe4ea;
  border-top-color: #2f6feb;
  border-radius: 50%;
  animation: iu-spin 0.8s linear infinite;
}

@keyframes iu-spin {
  to { transform: rotate(360deg); }
}

.iu__add {
  width: 100%;
  height: 100%;
  aspect-ratio: 4 / 3;
}

.iu__add :deep(.el-upload),
.iu__add :deep(.el-upload-dragger) {
  width: 100%;
  height: 100%;
  border-radius: 10px;
  background: #fbfcfd;
  border: 1px dashed #cdd4de;
  transition: border-color 0.18s ease, background 0.18s ease, color 0.18s ease;
}

.iu__add :deep(.el-upload-dragger:hover) {
  border-color: #2f6feb;
  background: #f4f8ff;
  color: #2f6feb;
}

.iu__add-tile {
  height: 100%;
  white-space: nowrap;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px;
}

.iu__add-icon {
  font-size: 22px;
  color: #98a2b3;
  background: #f2f4f7;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.iu__add :deep(.el-upload-dragger:hover) .iu__add-icon {
  color: #2f6feb;
  background: #e8f0ff;
}

.iu__add-text {
  font-size: 12px;
  color: #667085;
  white-space: nowrap;
}

.iu__add-sub {
  font-size: 11px;
  color: #98a2b3;
}

.iu__preview {
  width: 100%;
  border-radius: 10px;
}

.iu__preview-meta {
  flex: 1;
  text-align: left;
  font-size: 12px;
  color: #98a2b3;
  word-break: break-all;
  margin-right: 12px;
}
</style>
