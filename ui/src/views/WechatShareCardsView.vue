<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import {
  Dialog,
  IconAddCircle,
  IconRefreshLine,
  Toast,
  VButton,
  VCard,
  VEmpty,
  VLoading,
  VModal,
  VPagination,
  VSpace,
  VSwitch,
} from '@halo-dev/components'
import WechatShareCardEditingModal, {
  type CardKind,
  type FileNoteFormItem,
  type WechatShareCardForm,
  type WechatShareCardFormErrors,
} from '@/components/WechatShareCardEditingModal.vue'
import { deleteData, getApiErrorMessage, getData, patchData, postData, putData } from '@/api/client'
import { extractAttachmentUrl, thumbSrcForCardRow, type AttachmentLike } from '@/utils/attachmentUrl'
import {
  resolveAbsoluteAssetUrl,
  validateAbsoluteHttpUrl,
  validateCoverUrl,
  validateRedirectUrl,
} from '@/utils/urlSafety'
type WechatShareCardRow = {
  metadataName: string
  sid: string
  cardKind: CardKind | string
  enabled?: boolean
  title: string
  description: string
  img: string
  redirectUrl: string
  mediaUrl?: string | null
  displayName?: string | null
  optionalLinkLabel?: string | null
  optionalLinkUrl?: string | null
  contactInfo?: string | null
  videoTitle?: string | null
  videoGuideText?: string | null
  videoExtraLink?: string | null
  videoExtraLinkLabel?: string | null
  fileNotes?: { title: string; detail: string; jumpLink: boolean; url: string }[] | null
  shareUrl: string
  goUrl: string
  shareQrcodeDataUrl: string | null
  visitCount?: number
}

const listLoading = ref(true)
const saving = ref(false)
const cards = ref<WechatShareCardRow[]>([])
const publicSiteUrl = ref('')

const searchKeyword = ref('')
const selectedCardKindFilter = ref<string | undefined>(undefined)
const selectedStatusFilter = ref<string | undefined>(undefined)
const page = ref(1)
const size = ref(20)

const cardStatusFilterItems: { label: string; value?: string }[] = [
  { label: '全部', value: undefined },
  { label: '启用', value: 'enabled' },
  { label: '停用', value: 'disabled' },
]

const cardKindFilterDropdownItems: { label: string; value?: string }[] = [
  { label: '全部', value: undefined },
  { label: '链接', value: 'link' },
  { label: '图片', value: 'image' },
  { label: '音频', value: 'audio' },
  { label: '视频', value: 'video' },
  { label: '文件', value: 'file' },
]

const hasListFilters = computed(
  () =>
    !!searchKeyword.value.trim() ||
    selectedCardKindFilter.value !== undefined ||
    selectedStatusFilter.value !== undefined,
)

function clearListFilters() {
  searchKeyword.value = ''
  selectedCardKindFilter.value = undefined
  selectedStatusFilter.value = undefined
}

function rowEnabled(row: WechatShareCardRow): boolean {
  return row.enabled !== false
}

const modalOpen = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const editingMetadataName = ref<string | null>(null)
const editingSid = ref('')
const attachmentModalOpen = ref(false)
const attachmentPickerKey = ref(0)
const attachmentTarget = ref<'img' | 'mediaUrl'>('img')
function emptyFileNote(): FileNoteFormItem {
  return { title: '', detail: '', jumpLink: false, url: '' }
}

const qrModalOpen = ref(false)
const qrModalSrc = ref('')
const qrModalKind = ref<CardKind>('link')

const togglingEnabled = ref<string | null>(null)

const form = reactive<WechatShareCardForm>({
  cardKind: 'link',
  title: '',
  description: '',
  img: '',
  redirectUrl: '',
  mediaUrl: '',
  displayName: '',
  optionalLinkLabel: '',
  optionalLinkUrl: '',
  contactInfo: '',
  videoTitle: '',
  videoGuideText: '',
  videoExtraLink: '',
  videoExtraLinkLabel: '',
  fileNotes: [emptyFileNote()],
})

const formErrors = reactive<WechatShareCardFormErrors>({} as WechatShareCardFormErrors)

function resetFormErrors() {
  ;(Object.keys(form) as (keyof WechatShareCardForm)[]).forEach((k) => {
    formErrors[k] = undefined
  })
  formErrors.general = undefined
}

function resetForm() {
  form.cardKind = 'link'
  form.title = ''
  form.description = ''
  form.img = ''
  form.redirectUrl = ''
  form.mediaUrl = ''
  form.displayName = ''
  form.optionalLinkLabel = ''
  form.optionalLinkUrl = ''
  form.contactInfo = ''
  form.videoTitle = ''
  form.videoGuideText = ''
  form.videoExtraLink = ''
  form.videoExtraLinkLabel = ''
  form.fileNotes = [emptyFileNote()]
}

const filteredCards = computed(() => {
  let list = cards.value.slice()
  const fkRaw = selectedCardKindFilter.value
  const fk = typeof fkRaw === 'string' ? fkRaw.trim().toLowerCase() : ''
  if (fk) {
    list = list.filter((c) => (c.cardKind || 'link').toString().toLowerCase() === fk)
  }
  const st = selectedStatusFilter.value
  if (st === 'enabled') {
    list = list.filter((c) => rowEnabled(c))
  } else if (st === 'disabled') {
    list = list.filter((c) => !rowEnabled(c))
  }
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return list
  return list.filter((c) => {
    const hay = `${c.sid} ${c.cardKind} ${rowKindLabel(c.cardKind)} ${c.title} ${c.description}`.toLowerCase()
    return hay.includes(keyword)
  })
})

const pagedCards = computed(() => {
  const start = (page.value - 1) * size.value
  return filteredCards.value.slice(start, start + size.value)
})

watch([searchKeyword, selectedCardKindFilter, selectedStatusFilter], () => {
  page.value = 1
})

function refreshPublicOrigin() {
  if (typeof window === 'undefined') {
    publicSiteUrl.value = ''
    return
  }
  publicSiteUrl.value = window.location.origin.replace(/\/$/, '')
}

async function load() {
  listLoading.value = true
  try {
    refreshPublicOrigin()
    const rows = await getData<WechatShareCardRow[]>('/cards', {
      params: { _t: Date.now() },
    })
    cards.value = rows.map((r) => {
      const row = r as WechatShareCardRow
      return {
        ...row,
        enabled: row.enabled !== false,
        visitCount: row.visitCount ?? 0,
      }
    })
  } catch (e) {
    Dialog.error({
      title: '加载失败',
      description: getApiErrorMessage(e, '加载卡片列表失败，请稍后重试'),
    })
  } finally {
    listLoading.value = false
  }
}

async function patchCardEnabled(row: WechatShareCardRow, enabled: boolean) {
  if (togglingEnabled.value === row.metadataName) return
  const prev = rowEnabled(row)
  row.enabled = enabled
  togglingEnabled.value = row.metadataName
  try {
    const saved = await patchData<WechatShareCardRow>(
      `/cards/${encodeURIComponent(row.metadataName)}/enabled`,
      { enabled },
    )
    row.enabled = saved.enabled !== false
    Toast.success(enabled ? '已启用' : '已停用')
  } catch (e) {
    row.enabled = prev
    Dialog.error({
      title: '更新失败',
      description: getApiErrorMessage(e, '状态更新失败，请稍后重试'),
    })
  } finally {
    togglingEnabled.value = null
  }
}

function resetEditState() {
  modalMode.value = 'create'
  editingMetadataName.value = null
  editingSid.value = ''
}

function closeModal() {
  modalOpen.value = false
  attachmentModalOpen.value = false
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
  const k = (row.cardKind || 'link') as CardKind
  form.cardKind = ['link', 'image', 'audio', 'video', 'file'].includes(k) ? k : 'link'
  form.title = row.title
  form.description = row.description
  form.img = row.img
  form.redirectUrl = row.redirectUrl
  form.mediaUrl = row.mediaUrl || ''
  form.displayName = row.displayName || ''
  form.optionalLinkLabel = row.optionalLinkLabel || ''
  form.optionalLinkUrl = row.optionalLinkUrl || ''
  form.contactInfo = row.contactInfo || ''
  form.videoTitle = row.videoTitle || ''
  form.videoGuideText = row.videoGuideText || ''
  form.videoExtraLink = row.videoExtraLink || ''
  form.videoExtraLinkLabel = row.videoExtraLinkLabel || ''
  if (k === 'file' || k === 'audio' || k === 'image') {
    const headline = (form.displayName || '').trim() || (form.title || '').trim()
    form.title = headline
    form.displayName = headline
    form.optionalLinkLabel = ''
    form.optionalLinkUrl = ''
  }
  if (k === 'video') {
    const vh = (form.videoTitle || '').trim() || (form.title || '').trim()
    form.title = vh
    form.videoTitle = vh
  }
  if (k === 'file' || k === 'image') {
    const notes = row.fileNotes
    if (notes && notes.length > 0) {
      form.fileNotes = notes.map((n) => ({
        title: (n.title || '').trim(),
        detail: (n.detail || '').trim(),
        jumpLink: !!n.jumpLink,
        url: (n.url || '').trim(),
      }))
    } else if ((row.optionalLinkUrl || '').trim()) {
      form.fileNotes = [
        {
          title: (row.optionalLinkLabel || '').trim() || '相关链接',
          detail: '',
          jumpLink: true,
          url: (row.optionalLinkUrl || '').trim(),
        },
      ]
    } else {
      form.fileNotes = [emptyFileNote()]
    }
    form.optionalLinkLabel = ''
    form.optionalLinkUrl = ''
  } else {
    form.fileNotes = [emptyFileNote()]
  }
  if (k === 'image' || k === 'audio' || k === 'video' || k === 'file') {
    form.redirectUrl = ''
  }

  modalMode.value = 'edit'
  editingMetadataName.value = row.metadataName
  editingSid.value = row.sid
  modalOpen.value = true
}

async function openAttachmentPicker(target: 'img' | 'mediaUrl') {
  attachmentTarget.value = target
  attachmentModalOpen.value = false
  await nextTick()
  attachmentPickerKey.value += 1
  await nextTick()
  attachmentModalOpen.value = true
}

function onAttachmentSelect(items: AttachmentLike[]) {
  try {
    const url = extractAttachmentUrl(items)
    if (url) {
      if (attachmentTarget.value === 'mediaUrl') {
        form.mediaUrl = url
        if (formErrors.mediaUrl) formErrors.mediaUrl = undefined
      } else {
        form.img = url
        if (formErrors.img) formErrors.img = undefined
      }
      return
    }
    const first = items?.[0]
    const specKeys = first?.spec ? Object.keys(first.spec) : []
    const statusKeys = first?.status ? Object.keys(first.status) : []
    Dialog.warning({
      title: '提示',
      description: `未获取到附件 URL，请检查附件字段（spec: ${specKeys.join(', ') || '-'}；status: ${statusKeys.join(', ') || '-'}）`,
    })
  } finally {
    attachmentModalOpen.value = false
  }
}

function thumbSrc(url: string) {
  return thumbSrcForCardRow(url)
}

function rowKindLabel(kind: string | undefined | null) {
  const k = (kind || 'link').toLowerCase()
  if (k === 'image') return '图片'
  if (k === 'audio') return '音频'
  if (k === 'video') return '视频'
  if (k === 'file') return '文件'
  return '链接'
}

function rowSummary(row: WechatShareCardRow) {
  const k = (row.cardKind || 'link').toString().toLowerCase()
  if (k === 'video') {
    const t = (row.videoGuideText || row.description || '').trim()
    return t || '—'
  }
  return (row.description || '').trim() || '—'
}

const attachmentAccepts = computed(() =>
  attachmentTarget.value === 'mediaUrl'
    ? ['audio/*', 'video/*', 'application/*', 'image/*', '*/*']
    : ['image/*'],
)

function optionalUrlErr(raw: string): string | null {
  const t = raw.trim()
  if (!t) return null
  return validateRedirectUrl(t)
}

function validateAbsoluteResolved(raw: string, field: keyof WechatShareCardFormErrors) {
  const coverErr = validateCoverUrl(raw)
  if (coverErr) {
    formErrors[field] = coverErr
    return
  }
  const resolved = resolveAbsoluteAssetUrl(raw.trim(), publicSiteUrl.value)
  if (!resolved.ok) {
    formErrors[field] =
      '附件地址为站内路径时，请确保可通过站点根解析；建议使用完整 http(s) 地址'
    return
  }
  const absErr = validateAbsoluteHttpUrl(resolved.url)
  if (absErr) formErrors[field] = absErr
}

function validateBeforeSubmit(): boolean {
  resetFormErrors()

  const kind = form.cardKind

  if (kind === 'link') {
    const title = form.title.trim()
    const description = form.description.trim()
    if (!title) formErrors.title = '请填写标题'
    if (!description) formErrors.description = '请填写摘要'
    if (title.length > 32) formErrors.title = '标题不能超过 32 个字符'
    if (description.length > 32) formErrors.description = '摘要不能超过 32 个字符'
    validateAbsoluteResolved(form.img, 'img')
    const redirectErr = validateRedirectUrl(form.redirectUrl)
    if (redirectErr) formErrors.redirectUrl = redirectErr
    return !Object.values(formErrors).some(Boolean)
  }

  if (kind !== 'file' && kind !== 'audio' && kind !== 'video' && kind !== 'image') {
    if (!form.title.trim()) formErrors.title = '请填写页面标题'
    if (form.title.trim().length > 128) formErrors.title = '标题过长'
  }

  if (kind === 'image') {
    const fh = (form.displayName || '').trim() || (form.title || '').trim()
    if (!fh) {
      formErrors.displayName = '请填写页面标题 / 图片名称 / 卡片标题'
    } else if (fh.length > 128) {
      formErrors.displayName = '页面标题 / 图片名称不能超过 128 个字符'
    }
    if (!form.description.trim()) formErrors.description = '请填写图片介绍'
    if (form.description.trim().length > 512) formErrors.description = '图片介绍过长'
    validateAbsoluteResolved(form.img, 'img')
    if ((form.contactInfo || '').length > 512) formErrors.contactInfo = '联系方式过长'
  }

  if (kind === 'audio') {
    const fh = (form.displayName || '').trim() || (form.title || '').trim()
    if (!fh) {
      formErrors.displayName = '请填写页面标题 / 音频名称 / 卡片标题'
    } else if (fh.length > 128) {
      formErrors.displayName = '页面标题 / 音频名称不能超过 128 个字符'
    }
    if (!form.description.trim()) formErrors.description = '请填写音乐介绍'
    if (form.description.trim().length > 512) formErrors.description = '音乐介绍过长'
    validateAbsoluteResolved(form.img, 'img')
    validateAbsoluteResolved(form.mediaUrl, 'mediaUrl')
    if ((form.contactInfo || '').length > 512) formErrors.contactInfo = '联系方式过长'
  }

  if (kind === 'video') {
    const fh = form.videoTitle.trim() || form.title.trim()
    if (!fh) {
      formErrors.videoTitle = '请填写页面标题 / 视频标题 / 卡片标题'
    } else if (fh.length > 128) {
      formErrors.videoTitle = '标题不能超过 128 个字符'
    }
    if (!form.videoGuideText.trim()) {
      formErrors.videoGuideText = '请填写视频简介'
    } else if (form.videoGuideText.trim().length > 512) {
      formErrors.videoGuideText = '视频简介过长'
    }
    const ell = form.videoExtraLinkLabel.trim()
    if (ell.length > 64) {
      formErrors.videoExtraLinkLabel = '相关链接文案不能超过 64 个字符'
    }
    if (ell && !form.videoExtraLink.trim()) {
      formErrors.videoExtraLink = '填写了相关链接文案时请填写附加链接地址'
    }
    validateAbsoluteResolved(form.img, 'img')
    validateAbsoluteResolved(form.mediaUrl, 'mediaUrl')
    const extra = optionalUrlErr(form.videoExtraLink)
    if (extra) formErrors.videoExtraLink = extra
    const extraOk = form.videoExtraLink.trim()
    const mediaOk = form.mediaUrl.trim()
    if (!extraOk && !mediaOk) {
      formErrors.general = '请填写附加链接或视频地址至少一项用于分享跳转'
    }
  }

  if (kind === 'file') {
    if (!form.description.trim()) formErrors.description = '请填写文件介绍'
    if (form.description.trim().length > 512) formErrors.description = '文件介绍过长'
    const fh = (form.displayName || '').trim() || (form.title || '').trim()
    if (!fh) {
      formErrors.displayName = '请填写文件名称 / 页面标题 / 卡片标题'
    } else if (fh.length > 128) {
      formErrors.displayName = '文件名称 / 页面标题不能超过 128 个字符'
    }
    validateAbsoluteResolved(form.img, 'img')
    validateAbsoluteResolved(form.mediaUrl, 'mediaUrl')
    const mediaOk = form.mediaUrl.trim()
    if (!mediaOk) formErrors.general = '请填写文件下载地址'
    if ((form.contactInfo || '').length > 512) formErrors.contactInfo = '联系方式过长'
  }

  if (kind === 'file' || kind === 'image') {
    if (form.fileNotes.length > 20) {
      formErrors.fileNotes = '相关说明最多 20 条'
    } else {
      let idx = 0
      for (const n of form.fileNotes) {
        idx += 1
        const t = n.title.trim()
        const d = n.detail.trim()
        const u = n.url.trim()
        if (!t && !d && !(n.jumpLink && u)) continue
        if (!t) {
          formErrors.fileNotes = `相关说明第 ${idx} 条：请填写标题`
          break
        }
        if (t.length > 128) {
          formErrors.fileNotes = `相关说明第 ${idx} 条：标题过长`
          break
        }
        if (d.length > 512) {
          formErrors.fileNotes = `相关说明第 ${idx} 条：说明文案过长`
          break
        }
        if (n.jumpLink) {
          if (!u) {
            formErrors.fileNotes = `相关说明第 ${idx} 条：开启跳转时请填写链接地址`
            break
          }
          const ue = validateRedirectUrl(u)
          if (ue) {
            formErrors.fileNotes = `相关说明第 ${idx} 条：${ue}`
            break
          }
        }
      }
    }
  }

  return !Object.values(formErrors).some(Boolean)
}

function buildPayload(imgUrl: string, mediaUrl: string, redirectUrl: string): Record<string, unknown> {
  const k = form.cardKind
  let titleOut = form.title.trim()
  let displayOut = form.displayName.trim()
  if (k === 'file' || k === 'audio' || k === 'image') {
    const fh = displayOut || titleOut
    titleOut = fh
    displayOut = fh
  }
  if (k === 'video') {
    const vh = form.videoTitle.trim() || form.title.trim()
    titleOut = vh
  }
  const payload: Record<string, unknown> = {
    cardKind: k,
    title: titleOut,
    description: form.description.trim(),
    img: imgUrl,
    redirectUrl,
  }

  if (k !== 'link') {
    payload.mediaUrl = mediaUrl
    payload.displayName = displayOut
    payload.contactInfo = form.contactInfo.trim()
    payload.videoTitle = k === 'video' ? titleOut : form.videoTitle.trim()
    payload.videoGuideText = form.videoGuideText.trim()
    payload.videoExtraLink = form.videoExtraLink.trim()
    payload.videoExtraLinkLabel = k === 'video' ? form.videoExtraLinkLabel.trim() : ''
    if (k === 'file' || k === 'image') {
      payload.fileNotes = form.fileNotes.map((n) => ({
        title: n.title.trim(),
        detail: n.detail.trim(),
        jumpLink: n.jumpLink,
        url: n.url.trim(),
      }))
      payload.optionalLinkLabel = ''
      payload.optionalLinkUrl = ''
    } else if (k === 'audio') {
      payload.optionalLinkLabel = ''
      payload.optionalLinkUrl = ''
      payload.fileNotes = []
    } else {
      payload.optionalLinkLabel = form.optionalLinkLabel.trim()
      payload.optionalLinkUrl = form.optionalLinkUrl.trim()
      payload.fileNotes = []
    }
  }

  return payload
}

async function submitModal() {
  if (!validateBeforeSubmit()) return

  const resolvedImg = resolveAbsoluteAssetUrl(form.img.trim(), publicSiteUrl.value)
  if (!resolvedImg.ok) return

  const kind = form.cardKind
  const resolvedMedia =
    kind === 'link'
      ? ({ ok: true, url: '' } as const)
      : resolveAbsoluteAssetUrl(form.mediaUrl.trim(), publicSiteUrl.value)
  if ((kind === 'audio' || kind === 'video' || kind === 'file') && !resolvedMedia.ok) {
    Dialog.warning({ title: '提示', description: '媒体/文件地址无法解析为可提交的绝对地址，请检查附件或直链。' })
    return
  }

  let redirectTrim = ''
  if (kind === 'link') {
    redirectTrim = form.redirectUrl.trim()
  } else if (kind === 'video') {
    redirectTrim = form.videoExtraLink.trim() || form.mediaUrl.trim()
  } else if (kind === 'file') {
    redirectTrim = form.mediaUrl.trim()
  } else if (kind === 'image') {
    redirectTrim = resolvedImg.url.trim()
  } else if (kind === 'audio') {
    redirectTrim = resolvedMedia.ok ? resolvedMedia.url.trim() : ''
  }

  const redirectResolved = redirectTrim
    ? resolveAbsoluteAssetUrl(redirectTrim, publicSiteUrl.value)
    : { ok: false as const }
  if (redirectTrim && !redirectResolved.ok) {
    Dialog.warning({ title: '提示', description: '跳转链接无法解析为绝对地址，请使用 http(s) 或站内路径。' })
    return
  }

  const redirectFinal =
    redirectResolved.ok ? redirectResolved.url.trim() : ''

  const body = buildPayload(
    resolvedImg.url.trim(),
    resolvedMedia.ok ? resolvedMedia.url.trim() : '',
    redirectFinal,
  )

  const editedMeta = modalMode.value === 'edit' ? editingMetadataName.value : null
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
    if (editedMeta) {
      const i = cards.value.findIndex((c) => c.metadataName === editedMeta)
      if (i >= 0) {
        cards.value[i] = {
          ...cards.value[i],
          ...saved,
          enabled: saved.enabled !== false,
        }
      }
    }
    openShareQrModal(saved.shareQrcodeDataUrl, saved.cardKind as CardKind)
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

function openShareQrModal(dataUrl: string | null | undefined, kind?: CardKind | string | null) {
  qrModalSrc.value = (dataUrl || '').trim()
  const kk = (kind || 'link').toString().toLowerCase()
  qrModalKind.value = ['link', 'image', 'audio', 'video', 'file'].includes(kk) ? (kk as CardKind) : 'link'
  qrModalOpen.value = true
}

function openQrPreview(row: WechatShareCardRow) {
  openShareQrModal(row.shareQrcodeDataUrl, row.cardKind as CardKind)
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
    @open-attachment="openAttachmentPicker"
  />

  <AttachmentSelectorModal
    :key="attachmentPickerKey"
    v-model:visible="attachmentModalOpen"
    :max="1"
    :accepts="attachmentAccepts"
    @select="onAttachmentSelect"
  />

  <VModal :visible="qrModalOpen" title="分享二维码" @close="qrModalOpen = false">
    <div class="qr-modal-body">
      <p v-if="qrModalKind === 'link'" class="qr-modal-hint">
        使用微信扫描后点击右上角分享，会话卡片将跳转到你配置的链接。
      </p>
      <p v-else class="qr-modal-hint">
        使用微信扫描后，点击右上角分享即可封装为卡片样式。
      </p>
      <img v-if="qrModalSrc" class="qr-modal-img" :src="qrModalSrc" alt="分享二维码" />
      <template v-else>
        <p class="qr-modal-empty">暂无预览</p>
        <p class="qr-modal-empty-tip">若刚保存过卡片，请确认 Halo 外部访问地址与二维码上游接口可访问。</p>
      </template>
    </div>
  </VModal>

  <div class="wechat-share-page :uno:  pt-2">
    <VCard class="wechat-share-main-card" :body-class="[':uno: !p-0']">
      <template #header>
        <div class=":uno: block w-full bg-gray-50 px-4 py-3">
          <div class=":uno: relative flex flex-col flex-wrap items-start gap-4 sm:flex-row sm:items-center">
            <div class=":uno: flex w-full flex-1 items-center sm:w-auto">
              <SearchInput v-model="searchKeyword" placeholder="搜索 SID / 标题" />
            </div>

            <VSpace spacing="lg" class=":uno: flex-wrap">
              <FilterCleanButton v-if="hasListFilters" @click="clearListFilters" />
              <FilterDropdown
                v-model="selectedCardKindFilter"
                label="类型"
                :items="cardKindFilterDropdownItems"
              />
              <FilterDropdown v-model="selectedStatusFilter" label="状态" :items="cardStatusFilterItems" />
              <div class=":uno: flex flex-row gap-2">
                <div
                  class=":uno: group cursor-pointer rounded p-1 hover:bg-gray-200"
                  title="刷新"
                  role="button"
                  tabindex="0"
                  @click="load()"
                  @keydown.enter.prevent="load()"
                  @keydown.space.prevent="load()"
                >
                  <IconRefreshLine
                    :class="{ 'animate-spin text-gray-900': listLoading }"
                    class=":uno: h-4 w-4 text-gray-600 group-hover:text-gray-900"
                  />
                </div>
              </div>
              <VButton size="sm" type="primary" class="toolbar-btn" @click="openCreateModal">
                <template #icon>
                  <IconAddCircle class=":uno: size-full" />
                </template>
                新建卡片
              </VButton>
            </VSpace>
          </div>
        </div>
      </template>

      <VLoading v-if="listLoading && !cards.length" />
      <Transition v-else-if="!listLoading && !cards.length" appear name="fade">
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
      <Transition v-else-if="cards.length && !filteredCards.length" appear name="fade">
        <VEmpty message="请调整类型、状态筛选，或在搜索框输入后按回车确认关键词" title="无匹配结果" />
      </Transition>
      <Transition v-else-if="cards.length && filteredCards.length" appear name="fade">
        <div class="ws-cards-scroll">
          <table class="ws-cards-table">
            <colgroup>
              <col style="width: 7%" />
              <col style="width: 18%" />
              <col style="width: 14%" />
              <col style="width: 10%" />
              <col style="width: 10%" />
              <col style="width: 11%" />
              <col style="width: 11%" />
              <col style="width: 22%" />
            </colgroup>
            <thead class="ws-cards-thead">
              <tr>
                <th class="ws-cards-th">SID</th>
                <th class="ws-cards-th">卡片</th>
                <th class="ws-cards-th">摘要</th>
                <th class="ws-cards-th">类型</th>
                <th class="ws-cards-th">状态</th>
                <th class="ws-cards-th">访问量</th>
                <th class="ws-cards-th">二维码</th>
                <th class="ws-cards-th">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in pagedCards" :key="row.metadataName" class="ws-card-row">
                <td class="ws-cards-td">
                  <span class="sid">{{ row.sid }}</span>
                </td>
                <td class="ws-cards-td ws-cards-td--clip">
                  <div class="ws-card-cell">
                    <div v-if="row.img" class="thumb">
                      <img
                        class="thumb__img"
                        :key="`${row.metadataName}:${row.img || ''}`"
                        :src="thumbSrc(row.img)"
                        :alt="row.title"
                        loading="lazy"
                      />
                    </div>
                    <div v-else class="thumb thumb--placeholder" :aria-hidden="true">
                      <span class="thumb__letter">{{ row.title.slice(0, 1) }}</span>
                    </div>
                    <div class="ws-card-cell-text">
                      <p class="ws-card-title">{{ row.title }}</p>
                      <p class="ws-card-meta">metadata {{ row.metadataName.slice(0, 8) }}…</p>
                    </div>
                  </div>
                </td>
                <td class="ws-cards-td ws-cards-td--clip ws-cards-td--muted">
                  <p class="desc">{{ rowSummary(row) }}</p>
                </td>
                <td class="ws-cards-td">
                  <span class="kind-badge" :class="`kind-badge--${(row.cardKind || 'link').toString().toLowerCase()}`">
                    {{ rowKindLabel(row.cardKind) }}
                  </span>
                </td>
                <td class="ws-cards-td">
                  <div class="status-switch-wrap">
                    <VSwitch
                      :model-value="rowEnabled(row)"
                      :disabled="togglingEnabled === row.metadataName"
                      @update:model-value="(v) => patchCardEnabled(row, !!v)"
                    />
                  </div>
                </td>
                <td class="ws-cards-td">
                  <span class="ws-visit-count">{{ row.visitCount ?? 0 }}</span>
                </td>
                <td class="ws-cards-td">
                  <div v-if="row.shareQrcodeDataUrl" class="qr-cell">
                    <img class="qr-thumb" :src="row.shareQrcodeDataUrl" alt="分享二维码" loading="lazy" />
                  </div>
                  <span v-else class="qr-missing">未生成</span>
                </td>
                <td class="ws-cards-td">
                  <div class="ws-actions-inner">
                    <VButton size="xs" type="secondary" class="op-btn" @click="copyText('分享链接', row.shareUrl)">
                      复制链接
                    </VButton>
                    <VButton size="xs" type="secondary" class="op-btn" @click="openEditModal(row)">编辑</VButton>
                    <VButton size="xs" type="secondary" class="op-btn" @click="openQrPreview(row)">查看二维码</VButton>
                    <VButton size="xs" type="danger" class="op-btn" @click="removeCard(row)">删除</VButton>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </Transition>

      <template #footer>
        <div v-if="cards.length && filteredCards.length" class=":uno: px-4 py-3">
          <VPagination v-model:page="page" v-model:size="size" :total="filteredCards.length" :size-options="[10, 20, 30, 50]" />
        </div>
      </template>
    </VCard>
  </div>
</template>

<style scoped>
.wechat-share-main-card {
  box-shadow: 0 4px 18px rgb(15 23 42 / 0.06);
}

.wechat-share-main-card :deep(.card-header),
.wechat-share-main-card :deep(.card-footer) {
  padding: 0;
}

.wechat-share-main-card :deep(.halo-card) {
  border-radius: 12px;
  min-height: 0 !important;
  height: auto !important;
}

.wechat-share-main-card :deep(.card-body) {
  display: flex !important;
  flex: 0 1 auto !important;
  flex-direction: column !important;
  align-items: stretch !important;
  width: 100%;
  min-width: 0;
  min-height: 0 !important;
  height: auto !important;
  overflow: visible !important;
}

.ws-cards-scroll {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  flex: 0 0 auto;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 0;
  height: auto;
  box-sizing: border-box;
  overflow-x: auto;
}

.ws-cards-table {
  flex: 0 0 auto;
  table-layout: fixed;
  width: 100%;
  height: auto !important;
  max-height: none !important;
  min-width: 62rem;
  border-collapse: collapse;
}

.ws-cards-table tbody tr {
  height: auto !important;
  max-height: none !important;
}

.ws-cards-table :is(th, td) {
  box-sizing: border-box;
}

.ws-cards-thead {
  border-bottom: 1px solid rgb(229 231 235);
  background: rgb(249 250 251);
  font-size: 0.875rem;
  font-weight: 600;
  color: rgb(75 85 99);
}

.ws-cards-th {
  padding: 0.5rem 0.75rem;
  text-align: left;
  vertical-align: middle;
}

.ws-cards-th--num {
  text-align: right;
}

.ws-cards-td--num {
  text-align: right;
}

.ws-visit-count {
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  color: rgb(63 63 70);
}

.ws-card-row {
  border-bottom: 1px solid rgb(243 244 246);
  font-size: 0.875rem;
  line-height: 1.45;
  transition: background-color 0.15s ease;
}

.ws-card-row:hover {
  background: rgb(249 250 251);
}

.ws-cards-td {
  padding: 0.5rem 0.75rem;
  vertical-align: middle;
  min-width: 0;
}

.ws-cards-td--clip {
  overflow: hidden;
}

.ws-cards-td--muted {
  color: rgb(75 85 99);
}

.ws-card-cell {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
}

.ws-card-cell-text {
  min-width: 0;
  flex: 1 1 auto;
}

.ws-card-title {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 500;
  color: rgb(31 41 55);
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ws-card-meta {
  margin: 0.125rem 0 0;
  font-size: 0.75rem;
  color: rgb(156 163 175);
  line-height: 1.35;
}

.ws-actions-inner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}

.ws-actions-inner :deep(.halo-button),
.ws-actions-inner :deep(button.halo-button),
.ws-actions-inner :deep(.n-button) {
  width: auto !important;
  flex: 0 0 auto;
}

.toolbar-btn {
  border-radius: 4px;
  font-weight: 500;
}

.op-btn {
  border-radius: 4px;
}

.status-switch-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  vertical-align: middle;
  line-height: 1;
}

.status-switch-wrap :deep(.switch-wrapper) {
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  box-sizing: border-box !important;
  width: fit-content !important;
  height: auto !important;
  min-width: 0 !important;
  min-height: 0 !important;
  max-width: 3.75rem !important;
  max-height: 2rem !important;
  margin: 0 !important;
  padding: 0 !important;
  line-height: 1 !important;
}

.status-switch-wrap :deep(button.switch-inner) {
  box-sizing: border-box !important;
  margin: 0 !important;
  flex-shrink: 0 !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: flex-start !important;
  width: 2.75rem !important;
  height: 1.5rem !important;
  min-width: 2.75rem !important;
  min-height: 1.5rem !important;
  max-width: 2.75rem !important;
  max-height: 1.5rem !important;
  padding: 0 0.125rem !important;
  border-width: 2px !important;
  border-style: solid !important;
  border-color: transparent !important;
  border-radius: 9999px !important;
}

.status-switch-wrap :deep(.switch-indicator) {
  flex-shrink: 0 !important;
  width: 1.125rem !important;
  height: 1.125rem !important;
  min-width: 1.125rem !important;
  min-height: 1.125rem !important;
  margin-block: 0 !important;
  box-shadow:
    0 1px 2px rgb(15 23 42 / 0.08),
    0 0 0 1px rgb(15 23 42 / 0.04) !important;
}

.status-switch-wrap :deep(.switch-indicator.translate-x-5) {
  --tw-translate-x: calc(2.75rem - 4px - 0.25rem - 1.125rem) !important;
}

.sid {
  font-variant-numeric: tabular-nums;
  color: rgb(63 63 70);
  font-weight: 600;
}

.kind-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 2.75rem;
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
  font-size: 0.75rem;
  letter-spacing: 0.02em;
  line-height: 1.35;
  white-space: nowrap;
  color: rgb(63 63 70);
  background: rgb(248 250 252);
  border: 1px solid rgb(226 232 240);
}

.kind-badge--link {
  color: rgb(63 63 70);
  background: rgb(244 244 245);
  border-color: rgb(228 228 231);
}

.kind-badge--image {
  color: rgb(3 105 161);
  background: rgb(240 249 255);
  border-color: rgb(186 230 253);
}

.kind-badge--audio {
  color: rgb(91 33 182);
  background: rgb(245 243 255);
  border-color: rgb(221 214 254);
}

.kind-badge--video {
  color: rgb(159 18 57);
  background: rgb(255 241 242);
  border-color: rgb(254 205 211);
}

.kind-badge--file {
  color: rgb(120 53 15);
  background: rgb(255 251 235);
  border-color: rgb(253 230 138);
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
