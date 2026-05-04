<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import {
  Dialog,
  IconAddCircle,
  Toast,
  VButton,
  VCard,
  VEmpty,
  VLoading,
  VModal,
  VPageHeader,
  VPagination,
  VSpace,
} from '@halo-dev/components'
import { utils } from '@halo-dev/ui-shared'
import WechatShareCardEditingModal, {
  type WechatShareCardForm,
  type WechatShareCardFormErrors,
} from '@/components/WechatShareCardEditingModal.vue'
import WechatShareSettingsPanel from '@/components/WechatShareSettingsPanel.vue'
import { deleteData, getApiErrorMessage, getData, postData, putData } from '@/api/client'
import { extractAttachmentUrl, type AttachmentLike } from '@/utils/attachmentUrl'
import {
  resolveAbsoluteAssetUrl,
  validateAbsoluteHttpUrl,
  validateCoverUrl,
  validateRedirectUrl,
} from '@/utils/urlSafety'
import RiShareForwardLine from '~icons/ri/share-forward-line'

type WechatShareCardRow = {
  metadataName: string
  sid: string
  title: string
  description: string
  img: string
  redirectUrl: string
  shareUrl: string
  goUrl: string
  /** 服务端拼好的 data URL，无缓存时为 null */
  shareQrcodeDataUrl: string | null
}

const loading = ref(true)
const saving = ref(false)
const cards = ref<WechatShareCardRow[]>([])
const publicSiteUrl = ref('')

const q = ref('')
const page = ref(1)
const size = ref(20)

const modalOpen = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const editingMetadataName = ref<string | null>(null)
const editingSid = ref('')
const attachmentModalOpen = ref(false)

const qrModalOpen = ref(false)
const qrModalSrc = ref('')

const settingsModalOpen = ref(false)
const settingsPanelRef = ref<InstanceType<typeof WechatShareSettingsPanel> | null>(null)

const form = reactive<WechatShareCardForm>({
  title: '',
  description: '',
  img: '',
  redirectUrl: '',
})

const formErrors = reactive<WechatShareCardFormErrors>({})

function resetFormErrors() {
  formErrors.title = undefined
  formErrors.description = undefined
  formErrors.img = undefined
  formErrors.redirectUrl = undefined
}

function resetForm() {
  form.title = ''
  form.description = ''
  form.img = ''
  form.redirectUrl = ''
}

const filteredCards = computed(() => {
  const keyword = q.value.trim().toLowerCase()
  const list = cards.value.slice()
  if (!keyword) return list
  return list.filter((c) => {
    const hay = `${c.sid} ${c.title} ${c.description}`.toLowerCase()
    return hay.includes(keyword)
  })
})

const pagedCards = computed(() => {
  const start = (page.value - 1) * size.value
  return filteredCards.value.slice(start, start + size.value)
})

watch([q], () => {
  page.value = 1
})

function openSettingsModal() {
  settingsModalOpen.value = true
  nextTick(() => {
    settingsPanelRef.value?.load?.()
  })
}

function refreshPublicOrigin() {
  if (typeof window === 'undefined') {
    publicSiteUrl.value = ''
    return
  }
  publicSiteUrl.value = window.location.origin.replace(/\/$/, '')
}

async function load() {
  loading.value = true
  try {
    refreshPublicOrigin()
    cards.value = await getData<WechatShareCardRow[]>('/cards')
  } catch (e) {
    Dialog.error({
      title: '加载失败',
      description: getApiErrorMessage(e, '加载卡片列表失败，请稍后重试'),
    })
  } finally {
    loading.value = false
  }
}

function resetEditState() {
  modalMode.value = 'create'
  editingMetadataName.value = null
  editingSid.value = ''
}

function closeModal() {
  modalOpen.value = false
  resetEditState()
}

function openCreateModal() {
  resetForm()
  resetFormErrors()
  resetEditState()
  modalOpen.value = true
}

function openEditModal(row: WechatShareCardRow) {
  resetFormErrors()
  form.title = row.title
  form.description = row.description
  form.img = row.img
  form.redirectUrl = row.redirectUrl
  modalMode.value = 'edit'
  editingMetadataName.value = row.metadataName
  editingSid.value = row.sid
  modalOpen.value = true
}

function onAttachmentSelect(items: AttachmentLike[]) {
  const url = extractAttachmentUrl(items)
  if (url) {
    form.img = url
    if (formErrors.img) formErrors.img = undefined
    return
  }
  const first = items?.[0]
  const specKeys = first?.spec ? Object.keys(first.spec) : []
  const statusKeys = first?.status ? Object.keys(first.status) : []
  Dialog.warning({
    title: '提示',
    description: `未获取到附件 URL，请检查附件字段（spec: ${specKeys.join(', ') || '-'}；status: ${statusKeys.join(', ') || '-'}）`,
  })
}

function thumbSrc(url: string) {
  const raw = (url || '').trim()
  if (!raw) return ''
  return utils.attachment.getThumbnailUrl(raw, 'M')
}

function validateBeforeSubmit(): boolean {
  resetFormErrors()

  const title = form.title.trim()
  const description = form.description.trim()

  if (!title) formErrors.title = '请填写标题'
  if (!description) formErrors.description = '请填写摘要'

  if (title.length > 32) formErrors.title = '标题不能超过 32 个字符'
  if (description.length > 32) formErrors.description = '摘要不能超过 32 个字符'

  const coverErr = validateCoverUrl(form.img)
  if (coverErr) formErrors.img = coverErr

  const redirectErr = validateRedirectUrl(form.redirectUrl)
  if (redirectErr) formErrors.redirectUrl = redirectErr

  if (!formErrors.img) {
    const resolved = resolveAbsoluteAssetUrl(form.img.trim(), publicSiteUrl.value)
    if (!resolved.ok) {
      formErrors.img = '封面图为站内路径时，请使用当前控制台所在站点可访问的绝对地址，或确保相对路径在分享环境下可解析'
    } else {
      const absErr = validateAbsoluteHttpUrl(resolved.url)
      if (absErr) formErrors.img = absErr
    }
  }

  return !Object.values(formErrors).some(Boolean)
}

async function submitModal() {
  if (!validateBeforeSubmit()) return

  const resolved = resolveAbsoluteAssetUrl(form.img.trim(), publicSiteUrl.value)
  if (!resolved.ok) return

  const body = {
    title: form.title.trim(),
    description: form.description.trim(),
    img: resolved.url.trim(),
    redirectUrl: form.redirectUrl.trim(),
  }

  saving.value = true
  try {
    let saved: WechatShareCardRow
    if (modalMode.value === 'edit' && editingMetadataName.value) {
      saved = await putData<WechatShareCardRow>(`/cards/${encodeURIComponent(editingMetadataName.value)}`, body)
      Toast.success('已保存')
    } else {
      saved = await postData<WechatShareCardRow>('/cards', body)
      Toast.success('卡片已创建')
    }
    closeModal()
    resetForm()
    await load()
    openShareQrModal(saved.shareQrcodeDataUrl)
  } catch (e) {
    Dialog.error({
      title: modalMode.value === 'edit' ? '保存失败' : '创建失败',
      description: getApiErrorMessage(
        e,
        modalMode.value === 'edit' ? '保存失败，请检查填写项或稍后重试' : '创建失败，请检查填写项或稍后重试',
      ),
    })
  } finally {
    saving.value = false
  }
}

function openShareQrModal(dataUrl: string | null | undefined) {
  qrModalSrc.value = (dataUrl || '').trim()
  qrModalOpen.value = true
}

function openQrPreview(row: WechatShareCardRow) {
  openShareQrModal(row.shareQrcodeDataUrl)
}

async function copyText(label: string, text: string) {
  const v = (text || '').trim()
  if (!v) {
    Dialog.warning({ title: '提示', description: `${label}为空，无法复制` })
    return
  }
  try {
    await navigator.clipboard.writeText(v)
    Toast.success(`已复制${label}`)
  } catch {
    Dialog.warning({ title: '复制失败', description: '浏览器未授予剪贴板权限或环境不支持。' })
  }
}

async function runDeleteCard(row: WechatShareCardRow) {
  try {
    await deleteData(`/cards/${encodeURIComponent(row.metadataName)}`)
    Toast.success('已删除')
    await load()
    const total = filteredCards.value.length
    const totalPages = Math.max(1, Math.ceil(total / size.value))
    if (page.value > totalPages) {
      page.value = totalPages
    }
  } catch (e) {
    Dialog.error({
      title: '删除失败',
      description: getApiErrorMessage(e, '删除失败，请稍后重试'),
    })
  }
}

function removeCard(row: WechatShareCardRow) {
  Dialog.warning({
    title: '确认删除',
    description: `删除卡片「${row.title}」（sid=${row.sid}）？该操作不可恢复。`,
    showCancel: true,
    /** 返回 Promise，确保控制台 Dialog 会等待异步完成后再关闭，从而触发列表 reload */
    onConfirm: () => runDeleteCard(row),
  })
}

onMounted(() => {
  void load()
})
</script>

<template>
  <WechatShareCardEditingModal
    :visible="modalOpen"
    :mode="modalMode"
    :sid="editingSid"
    :saving="saving"
    :form="form"
    :errors="formErrors"
    @close="closeModal"
    @save="submitModal"
    @open-attachment="attachmentModalOpen = true"
  />

  <AttachmentSelectorModal
    v-model:visible="attachmentModalOpen"
    :max="1"
    :accepts="['image/*']"
    @select="onAttachmentSelect"
  />

  <VModal :visible="qrModalOpen" title="分享二维码" @close="qrModalOpen = false">
    <div class="qr-modal-body">
      <p class="qr-modal-hint">使用微信扫描后点击右上角分享即可</p>
      <img v-if="qrModalSrc" class="qr-modal-img" :src="qrModalSrc" alt="分享二维码" />
      <template v-else>
        <p class="qr-modal-empty">暂无预览</p>
        <p class="qr-modal-empty-tip">若刚保存过卡片，请确认 Halo 外部访问地址与二维码上游接口可访问。</p>
      </template>
    </div>
  </VModal>

  <VModal
    :visible="settingsModalOpen"
    title="插件配置"
    :width="720"
    :body-class="[':uno: !p-0']"
    @close="settingsModalOpen = false"
  >
    <div class="settings-modal-body">
      <WechatShareSettingsPanel ref="settingsPanelRef" />
    </div>
  </VModal>

  <VPageHeader title="微信分享卡片">
    <template #icon>
      <RiShareForwardLine />
    </template>
    <template #actions>
      <VButton type="secondary" @click="openSettingsModal">插件配置</VButton>
    </template>
  </VPageHeader>

  <div class="wechat-share-page :uno: p-4 pt-2">
    <VCard class="wechat-share-main-card" :body-class="[':uno: !p-0']">
      <template #header>
        <div class=":uno: block w-full bg-gray-50 px-4 py-3">
          <div class=":uno: flex flex-wrap items-center justify-between gap-3">
            <div class=":uno: flex items-center gap-2 box-border border border-gray-300 rounded-base">
              <input
                v-model="q"
                class=":uno: h-9 w-56 rounded-md border border-gray-200 bg-white px-3 text-sm outline-none focus:border-primary"
                placeholder="搜索 SID / 标题 / 摘要"
              />
            </div>

            <div class=":uno: flex items-center gap-2">
              <VButton size="sm" type="secondary" class="toolbar-btn" @click="load">刷新</VButton>
              <VButton size="sm" type="primary" class="toolbar-btn" @click="openCreateModal">
                <template #icon>
                  <IconAddCircle class=":uno: size-full" />
                </template>
                新建卡片
              </VButton>
            </div>
          </div>
        </div>
      </template>

      <VLoading v-if="loading" />
      <Transition v-else-if="!cards.length" appear name="fade">
        <VEmpty message="你可以尝试刷新或者新建分享卡片" title="暂无自定义分享卡片">
          <template #actions>
            <VSpace>
              <VButton @click="load">刷新</VButton>
              <VButton type="primary" @click="openCreateModal">
                <template #icon>
                  <IconAddCircle class=":uno: size-full" />
                </template>
                新建卡片
              </VButton>
            </VSpace>
          </template>
        </VEmpty>
      </Transition>
      <Transition v-else-if="!filteredCards.length" appear name="fade">
        <VEmpty message="请调整搜索关键词" title="无匹配结果" />
      </Transition>
      <Transition v-else appear name="fade">
        <div class=":uno: overflow-x-auto">
          <table class=":uno: min-w-full table-fixed border-collapse">
            <colgroup>
              <col class=":uno: w-28" />
              <col class=":uno: min-w-[14rem]" />
              <col class=":uno: min-w-[12rem]" />
              <col class=":uno: w-44" />
              <col class=":uno: w-52" />
            </colgroup>
            <thead class=":uno: border-y border-gray-200 bg-gray-50 text-sm text-gray-600 font-semibold">
              <tr>
                <th class=":uno: px-3 py-2 text-left">SID</th>
                <th class=":uno: px-3 py-2 text-left">卡片</th>
                <th class=":uno: px-3 py-2 text-left">摘要</th>
                <th class=":uno: px-3 py-2 text-left">二维码</th>
                <th class=":uno: px-3 py-2 text-left">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in pagedCards"
                :key="row.metadataName"
                class="card-row :uno: border-b border-gray-100 text-sm transition-colors hover:bg-gray-50"
              >
                <td class=":uno: px-3 py-3 align-middle">
                  <span class="sid">{{ row.sid }}</span>
                </td>
                <td class=":uno: px-3 py-3 align-middle">
                  <div class=":uno: min-w-0 flex items-center gap-3">
                    <div v-if="row.img" class="thumb">
                      <img class="thumb__img" :src="thumbSrc(row.img)" :alt="row.title" loading="lazy" />
                    </div>
                    <div v-else class="thumb thumb--placeholder" :aria-hidden="true">
                      <span class="thumb__letter">{{ row.title.slice(0, 1) }}</span>
                    </div>
                    <div class=":uno: min-w-0">
                      <p class=":uno: truncate text-sm text-gray-800 font-medium">{{ row.title }}</p>
                      <p class=":uno: text-xs text-gray-400">metadata {{ row.metadataName.slice(0, 8) }}…</p>
                    </div>
                  </div>
                </td>
                <td class=":uno: px-3 py-3 align-middle text-gray-600">
                  <p class="desc">{{ row.description }}</p>
                </td>
                <td class=":uno: px-3 py-3 align-middle">
                  <div v-if="row.shareQrcodeDataUrl" class="qr-cell">
                    <img class="qr-thumb" :src="row.shareQrcodeDataUrl" alt="分享二维码" loading="lazy" />
                  </div>
                  <span v-else class="qr-missing">未生成</span>
                </td>
                <td class=":uno: px-3 py-3 align-middle">
                  <VSpace class=":uno: gap-2 flex-wrap">
                    <VButton size="xs" type="secondary" class="op-btn" @click="copyText('分享链接', row.shareUrl)">
                      复制链接
                    </VButton>
                    <VButton size="xs" type="secondary" class="op-btn" @click="openEditModal(row)">编辑</VButton>
                    <VButton size="xs" type="secondary" class="op-btn" @click="openQrPreview(row)">查看二维码</VButton>
                    <VButton size="xs" type="danger" class="op-btn" @click="removeCard(row)">删除</VButton>
                  </VSpace>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </Transition>

      <template #footer>
        <div v-if="!loading && cards.length && filteredCards.length" class=":uno: px-4 py-3">
          <VPagination v-model:page="page" v-model:size="size" :total="filteredCards.length" :size-options="[10, 20, 30, 50]" />
        </div>
      </template>
    </VCard>
  </div>
</template>

<style scoped>
.settings-modal-body {
  max-height: min(78vh, 640px);
  overflow: auto;
  margin: 0;
  padding: 0;
}

.wechat-share-page :deep(.halo-card) {
  border-radius: 12px;
}

.wechat-share-main-card {
  box-shadow: 0 4px 18px rgb(15 23 42 / 0.06);
}

.wechat-share-main-card :deep(.card-header),
.wechat-share-main-card :deep(.card-footer) {
  padding: 0;
}

.toolbar-btn {
  border-radius: 4px;
  font-weight: 500;
}

.op-btn {
  border-radius: 4px;
}

.sid {
  font-variant-numeric: tabular-nums;
  color: rgb(63 63 70);
  font-weight: 600;
}

.desc {
  margin: 0;
  word-break: break-word;
  line-height: 1.45;
}

.thumb {
  flex: 0 0 auto;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  overflow: hidden;
  background: rgb(244 244 245);
  border: 1px solid rgb(228 228 231);
  box-sizing: border-box;
}

.thumb__img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.thumb--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb__letter {
  font-size: 0.875rem;
  font-weight: 600;
  color: rgb(113 113 122);
  line-height: 1;
}

.qr-cell {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  border: 1px solid rgb(228 228 231);
  overflow: hidden;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qr-thumb {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qr-missing {
  font-size: 0.75rem;
  color: rgb(161 161 170);
}

.qr-modal-body {
  min-height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 0.5rem 0;
  text-align: center;
}

.qr-modal-hint {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 500;
  color: rgb(39 39 42);
  line-height: 1.5;
  max-width: 22rem;
}

.qr-modal-img {
  max-width: min(320px, 100%);
  height: auto;
  border-radius: 8px;
  border: 1px solid rgb(228 228 231);
  background: #fff;
}

.qr-modal-empty {
  margin: 0;
  font-size: 0.875rem;
  color: rgb(113 113 122);
}

.qr-modal-empty-tip {
  margin: 0;
  max-width: 22rem;
  font-size: 0.75rem;
  line-height: 1.45;
  color: rgb(161 161 170);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
