<template>
  <div class="page">
    <h2 class="page-title">系统配置</h2>
    <p class="page-sub">租户参数、多语言、菜单、字典、存储与微信通知配置</p>

    <el-tabs v-model="tab" class="card">
      <el-tab-pane label="基础配置" name="tenant">
        <div class="section-title">租户门户与联系信息</div>
        <el-form label-width="120px" style="max-width:680px">
          <el-form-item label="租户名称"><el-input v-model="tenantConfig.tenant_name" /></el-form-item>
          <el-form-item label="宣传标语"><el-input v-model="tenantConfig.slogan" /></el-form-item>
          <el-form-item label="客服电话"><el-input v-model="tenantConfig.service_phone" /></el-form-item>
          <el-form-item label="默认门店">
            <el-select v-model="tenantConfig.default_store_id" style="width:100%">
              <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="String(s.id)" />
            </el-select>
          </el-form-item>
          <el-form-item label="Logo 地址"><el-input v-model="tenantConfig.logo" placeholder="可上传后填入 URL" /></el-form-item>
          <el-form-item label="站内通知">
            <el-switch v-model="tenantConfig.notify_enabled" active-value="true" inactive-value="false" />
          </el-form-item>
          <el-form-item label="微信支付">
            <el-switch v-model="tenantConfig.wechat_pay_enabled" active-value="true" inactive-value="false" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveTenantConfig">保存配置</el-button>
          </el-form-item>
        </el-form>

        <div class="section-title" style="margin-top:22px">平台参数（只读）</div>
        <el-table :data="params" size="small">
          <el-table-column prop="paramKey" label="参数键" width="220" />
          <el-table-column prop="name" label="说明" min-width="180" />
          <el-table-column prop="paramValue" label="当前值" min-width="160" />
          <el-table-column prop="scope" label="作用域" width="100" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="多语言" name="i18n">
        <div class="toolbar">
          <el-select v-model="lang" clearable placeholder="全部语言" style="width:150px" @change="loadI18n">
            <el-option v-for="l in languages" :key="l.code" :label="l.name" :value="l.code" />
          </el-select>
          <div class="grow"></div>
          <el-button type="primary" :icon="Plus" @click="openI18n">新增词条</el-button>
        </div>
        <el-table :data="i18nList" size="small">
          <el-table-column prop="lang" label="语言" width="100" />
          <el-table-column prop="msgKey" label="词条键" min-width="200" />
          <el-table-column prop="msgValue" label="译文" min-width="240" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="primary" @click="openI18n(row)">编辑</el-button>
              <el-button link type="danger" @click="removeI18n(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="菜单配置" name="menus">
        <div class="toolbar">
          <span class="muted">管理端 / 小程序 / H5 菜单可分别配置，排序值越小越靠前</span>
          <div class="grow"></div>
          <el-button type="primary" :icon="Plus" @click="openMenu">新增菜单</el-button>
        </div>
        <el-table :data="menus" size="small">
          <el-table-column prop="code" label="编码" width="120" />
          <el-table-column prop="title" label="名称" width="140" />
          <el-table-column prop="platform" label="端" width="90" />
          <el-table-column prop="path" label="路径" min-width="160" />
          <el-table-column prop="icon" label="图标" width="110" />
          <el-table-column prop="sort" label="排序" width="70" align="center" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130">
            <template #default="{ row }">
              <el-button link type="primary" @click="openMenu(row)">编辑</el-button>
              <el-button link type="danger" @click="removeMenu(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="字典扩展" name="dicts">
        <div class="toolbar">
          <el-select v-model="dictType" clearable placeholder="全部字典类型" style="width:180px" @change="loadDicts">
            <el-option v-for="t in dictTypes" :key="t" :label="t" :value="t" />
          </el-select>
          <div class="grow"></div>
          <el-button type="primary" :icon="Plus" @click="openDict">新增扩展项</el-button>
        </div>
        <el-table :data="dictRows" size="small">
          <el-table-column prop="type" label="字典类型" width="160" />
          <el-table-column prop="code" label="编码" width="160" />
          <el-table-column prop="name" label="名称" min-width="160" />
          <el-table-column label="来源" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.scope === 'GLOBAL' ? 'info' : 'success'">{{ row.scope === 'GLOBAL' ? '平台' : '租户' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button v-if="row.scope === 'TENANT'" link type="danger" @click="removeDict(row)">删除</el-button>
              <span v-else class="muted">只读</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="文件存储" name="storage">
        <el-form label-width="110px" style="max-width:620px">
          <el-form-item label="存储驱动">
            <el-radio-group v-model="storageForm.driver">
              <el-radio-button value="LOCAL">本地存储</el-radio-button>
              <el-radio-button value="S3">S3 / MinIO</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <template v-if="storageForm.driver === 'LOCAL'">
            <el-form-item label="存储目录"><el-input v-model="storageForm.config.basePath" placeholder="./data/uploads" /></el-form-item>
          </template>
          <template v-else>
            <el-form-item label="Endpoint"><el-input v-model="storageForm.config.endpoint" placeholder="http://127.0.0.1:9000" /></el-form-item>
            <el-form-item label="Bucket"><el-input v-model="storageForm.config.bucket" /></el-form-item>
            <el-form-item label="AccessKey"><el-input v-model="storageForm.config.accessKey" /></el-form-item>
            <el-form-item label="SecretKey"><el-input v-model="storageForm.config.secretKey" type="password" show-password /></el-form-item>
            <el-form-item label="公开地址"><el-input v-model="storageForm.config.publicUrl" placeholder="可选，CDN 域名" /></el-form-item>
          </template>
          <el-form-item>
            <el-button @click="testStorage">测试连接</el-button>
            <el-button type="primary" @click="saveStorage">保存配置</el-button>
          </el-form-item>
          <el-form-item label="上传测试">
            <el-upload :show-file-list="false" :http-request="uploadFile">
              <el-button :icon="Upload">选择文件上传</el-button>
            </el-upload>
            <span v-if="uploadedUrl" class="muted" style="margin-left:12px">{{ uploadedUrl }}</span>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="微信与通知" name="wechat">
        <div class="section-title">微信公众号 / 小程序配置</div>
        <el-table :data="wechatList" size="small">
          <el-table-column label="应用类型" width="110">
            <template #default="{ row }">{{ row.appType === 'MINI' ? '小程序' : '公众号' }}</template>
          </el-table-column>
          <el-table-column prop="appId" label="AppId" min-width="180" />
          <el-table-column prop="appSecretMask" label="AppSecret" width="160" />
          <el-table-column prop="domain" label="业务域名" min-width="180" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button link type="primary" @click="openWechat(row)">配置</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div style="margin-top:20px" class="section-title">通知模板</div>
        <el-table :data="templates" size="small">
          <el-table-column prop="bizType" label="业务类型" width="170" />
          <el-table-column prop="channel" label="渠道" width="90" />
          <el-table-column prop="title" label="标题模板" min-width="220" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button link type="primary" @click="openTemplate(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div style="margin-top:20px" class="section-title">站内消息</div>
        <el-table :data="notifications" size="small">
          <el-table-column prop="title" label="标题" width="150" />
          <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
          <el-table-column prop="channel" label="渠道" width="90" />
          <el-table-column prop="status" label="状态" width="90" />
          <el-table-column prop="createTime" label="时间" width="160">
            <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="i18nDialog" :title="i18nForm.id ? '编辑词条' : '新增词条'" width="480px">
      <el-form :model="i18nForm" label-width="86px">
        <el-form-item label="语言" required>
          <el-select v-model="i18nForm.lang" style="width:100%">
            <el-option v-for="l in languages" :key="l.code" :label="l.name" :value="l.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="词条键" required><el-input v-model="i18nForm.msgKey" placeholder="如 booking.title" /></el-form-item>
        <el-form-item label="译文" required><el-input v-model="i18nForm.msgValue" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="i18nDialog = false">取消</el-button>
        <el-button type="primary" @click="saveI18n">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="menuDialog" :title="menuForm.id ? '编辑菜单' : '新增菜单'" width="540px">
      <el-form :model="menuForm" label-width="86px" class="form-grid">
        <el-form-item label="菜单编码" required><el-input v-model="menuForm.code" /></el-form-item>
        <el-form-item label="菜单名称" required><el-input v-model="menuForm.title" /></el-form-item>
        <el-form-item label="所属端">
          <el-select v-model="menuForm.platform" style="width:100%">
            <el-option label="管理端" value="ADMIN" />
            <el-option label="小程序" value="MINI" />
            <el-option label="H5" value="H5" />
          </el-select>
        </el-form-item>
        <el-form-item label="路径"><el-input v-model="menuForm.path" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="menuForm.icon" /></el-form-item>
        <el-form-item label="父级编码"><el-input v-model="menuForm.parentCode" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="menuForm.sort" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="menuForm.enabled">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="menuDialog = false">取消</el-button>
        <el-button type="primary" @click="saveMenu">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dictDialog" title="新增租户扩展字典" width="440px">
      <el-form :model="dictForm" label-width="86px">
        <el-form-item label="字典类型" required><el-input v-model="dictForm.type" placeholder="如 payment_channel" /></el-form-item>
        <el-form-item label="编码" required><el-input v-model="dictForm.code" /></el-form-item>
        <el-form-item label="名称" required><el-input v-model="dictForm.name" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="dictForm.sort" :min="0" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dictDialog = false">取消</el-button>
        <el-button type="primary" @click="saveDict">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="wechatDialog" title="微信配置" width="500px">
      <el-form :model="wechatForm" label-width="96px">
        <el-form-item label="应用类型">
          <el-select v-model="wechatForm.appType" style="width:100%">
            <el-option label="小程序" value="MINI" />
            <el-option label="公众号" value="OFFICIAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="AppId"><el-input v-model="wechatForm.appId" /></el-form-item>
        <el-form-item label="AppSecret"><el-input v-model="wechatForm.appSecret" placeholder="留空或包含 ****** 表示不修改" /></el-form-item>
        <el-form-item label="Token"><el-input v-model="wechatForm.token" /></el-form-item>
        <el-form-item label="AES Key"><el-input v-model="wechatForm.aesKey" placeholder="留空表示不修改" /></el-form-item>
        <el-form-item label="业务域名"><el-input v-model="wechatForm.domain" /></el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="wechatForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="wechatDialog = false">取消</el-button>
        <el-button type="primary" @click="saveWechat">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="templateDialog" title="编辑通知模板" width="520px">
      <el-form :model="templateForm" label-width="96px">
        <el-form-item label="业务类型"><el-input v-model="templateForm.bizType" /></el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="templateForm.channel" style="width:100%">
            <el-option label="站内" value="SITE" />
            <el-option label="公众号" value="OFFICIAL" />
            <el-option label="小程序" value="MINI" />
            <el-option label="短信" value="SMS" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="templateForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="templateForm.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="templateForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { Plus, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import { dateTime } from '@/utils/format'

const tab = ref('tenant')
const tenantConfig = reactive<Record<string, string>>({})
const params = ref<any[]>([])
const stores = ref<any[]>([])
const languages = ref<any[]>([])
const i18nList = ref<any[]>([])
const lang = ref('')
const menus = ref<any[]>([])
const dictTypes = ref<string[]>([])
const dictRows = ref<any[]>([])
const dictType = ref('')
const storageForm = reactive<any>({ driver: 'LOCAL', config: { basePath: './data/uploads' } })
const uploadedUrl = ref('')
const wechatList = ref<any[]>([])
const templates = ref<any[]>([])
const notifications = ref<any[]>([])
const i18nDialog = ref(false)
const menuDialog = ref(false)
const dictDialog = ref(false)
const wechatDialog = ref(false)
const templateDialog = ref(false)
const i18nForm = reactive<any>({})
const menuForm = reactive<any>({})
const dictForm = reactive<any>({})
const wechatForm = reactive<any>({})
const templateForm = reactive<any>({})

async function loadTenant() {
  const [cfg, ps, ss]: any[] = await Promise.all([
    http.get('/admin/tenant-config'), http.get('/admin/params'), http.get('/admin/stores')
  ])
  Object.assign(tenantConfig, cfg.data || {})
  params.value = ps.data || []
  stores.value = ss.data || []
}

async function saveTenantConfig() {
  await http.put('/admin/tenant-config', tenantConfig)
  ElMessage.success('租户配置已保存')
}

async function loadLanguages() {
  const res: any = await http.get('/admin/languages')
  languages.value = res.data || []
}

async function loadI18n() {
  const res: any = await http.get('/admin/i18n', { params: lang.value ? { lang: lang.value } : {} })
  i18nList.value = res.data || []
}

function openI18n(row?: any) {
  Object.assign(i18nForm, { id: undefined, lang: languages.value[0]?.code || 'zh-CN', msgKey: '', msgValue: '' })
  if (row) Object.assign(i18nForm, row)
  i18nDialog.value = true
}

async function saveI18n() {
  if (!i18nForm.lang || !i18nForm.msgKey || !i18nForm.msgValue) return ElMessage.warning('请填写完整')
  await http.post('/admin/i18n', i18nForm)
  ElMessage.success('词条已保存')
  i18nDialog.value = false
  loadI18n()
}

async function removeI18n(row: any) {
  await ElMessageBox.confirm(`确认删除词条「${row.msgKey}」？`, '提示', { type: 'warning' })
  await http.delete(`/admin/i18n/${row.id}`)
  ElMessage.success('已删除')
  loadI18n()
}

async function loadMenus() {
  const res: any = await http.get('/admin/menus', { params: { platform: 'ADMIN' } })
  menus.value = res.data || []
}

function openMenu(row?: any) {
  Object.assign(menuForm, { id: undefined, code: '', title: '', platform: 'ADMIN', path: '', icon: '', parentCode: '', sort: 0, enabled: 1 })
  if (row) Object.assign(menuForm, row)
  menuDialog.value = true
}

async function saveMenu() {
  if (!menuForm.code || !menuForm.title) return ElMessage.warning('请填写菜单编码与名称')
  if (menuForm.id) await http.put(`/admin/menus/${menuForm.id}`, menuForm)
  else await http.post('/admin/menus', menuForm)
  ElMessage.success('菜单已保存')
  menuDialog.value = false
  loadMenus()
}

async function removeMenu(row: any) {
  await ElMessageBox.confirm(`确认删除菜单「${row.title}」？`, '提示', { type: 'warning' })
  await http.delete(`/admin/menus/${row.id}`)
  ElMessage.success('已删除')
  loadMenus()
}

async function loadDicts() {
  const res: any = await http.get('/admin/dicts')
  dictTypes.value = res.data?.globalTypes || []
  const grouped = res.data?.dicts || {}
  const rows: any[] = []
  Object.keys(grouped).forEach(type => {
    if (dictType.value && type !== dictType.value) return
    grouped[type].forEach((d: any) => rows.push({ ...d, type }))
  })
  dictRows.value = rows
}

function openDict() {
  Object.assign(dictForm, { type: dictType.value || '', code: '', name: '', sort: 0 })
  dictDialog.value = true
}

async function saveDict() {
  if (!dictForm.type || !dictForm.code || !dictForm.name) return ElMessage.warning('请填写完整')
  await http.post('/admin/dicts', dictForm)
  ElMessage.success('字典项已新增')
  dictDialog.value = false
  loadDicts()
}

async function removeDict(row: any) {
  await ElMessageBox.confirm(`确认删除字典项「${row.name}」？`, '提示', { type: 'warning' })
  await http.delete(`/admin/dicts/${row.id}`)
  ElMessage.success('已删除')
  loadDicts()
}

async function loadStorage() {
  const res: any = await http.get('/admin/storage')
  storageForm.driver = res.data?.driver || 'LOCAL'
  storageForm.config = res.data?.config || {}
}

async function saveStorage() {
  await http.post('/admin/storage', { driver: storageForm.driver, config: storageForm.config })
  ElMessage.success('存储配置已保存')
}

async function testStorage() {
  const res: any = await http.post('/admin/storage/test', { driver: storageForm.driver, config: storageForm.config })
  ElMessage[res.data?.success ? 'success' : 'error'](res.data?.message || '测试完成')
}

async function uploadFile(options: any) {
  const fd = new FormData()
  fd.append('file', options.file)
  fd.append('category', 'config')
  const res: any = await http.post('/admin/files/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  uploadedUrl.value = res.data?.url || ''
  ElMessage.success('上传成功：' + uploadedUrl.value)
}

async function loadWechat() {
  const [w, t, n]: any[] = await Promise.all([
    http.get('/admin/wechat'), http.get('/admin/notify-templates'), http.get('/admin/notifications')
  ])
  wechatList.value = w.data || []
  templates.value = t.data || []
  notifications.value = n.data || []
}

function openWechat(row: any) {
  Object.assign(wechatForm, { ...row, appSecret: '', aesKey: '' })
  wechatDialog.value = true
}

async function saveWechat() {
  await http.post('/admin/wechat', wechatForm)
  ElMessage.success('微信配置已保存')
  wechatDialog.value = false
  loadWechat()
}

function openTemplate(row: any) {
  Object.assign(templateForm, row)
  templateDialog.value = true
}

async function saveTemplate() {
  await http.post('/admin/notify-templates', templateForm)
  ElMessage.success('模板已保存')
  templateDialog.value = false
  loadWechat()
}

watch(tab, (v) => {
  if (v === 'i18n') { loadLanguages(); loadI18n() }
  if (v === 'menus') loadMenus()
  if (v === 'dicts') loadDicts()
  if (v === 'storage') loadStorage()
  if (v === 'wechat') loadWechat()
})

onMounted(() => {
  loadTenant()
  loadLanguages()
  loadI18n()
  loadMenus()
  loadDicts()
  loadStorage()
  loadWechat()
})
</script>
