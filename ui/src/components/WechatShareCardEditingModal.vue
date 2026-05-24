<script setup lang="ts">
import { computed, toRefs, watch } from 'vue'
import { VButton, VModal } from '@halo-dev/components'
import WechatShareFilterLikeSelect from '@/components/WechatShareFilterLikeSelect.vue'
import { appendDisplayCacheBust } from '@/utils/attachmentUrl'

export type CardKind = 'link' | 'image' | 'audio' | 'video' | 'file'

export type FileNoteFormItem = {
  title: string
  detail: string
  jumpLink: boolean
  url: string
}

export type FileNotePreviewRow =
  | { mode: 'link'; title: string; detail: string; url: string }
  | { mode: 'text'; title: string; detail: string }

export type WechatShareCardForm = {
  cardKind: CardKind
  title: string
  description: string
  img: string
  redirectUrl: string
  mediaUrl: string
  displayName: string
  optionalLinkLabel: string
  optionalLinkUrl: string
  fileNotes: FileNoteFormItem[]
  contactInfo: string
  videoTitle: string
  videoGuideText: string
  videoExtraLink: string
  videoExtraLinkLabel: string
}

export type WechatShareCardFormErrors = Partial<Record<keyof WechatShareCardForm | 'general', string>>

const props = withDefaults(
  defineProps<{
    visible: boolean
    saving: boolean
    form: WechatShareCardForm
    errors: WechatShareCardFormErrors
    mode?: 'create' | 'edit'
    sid?: string | null
  }>(),
  {
    mode: 'create',
    sid: null,
  },
)
const { visible, saving, form, errors, mode, sid } = toRefs(props)

const emit = defineEmits<{
  (event: 'close'): void
  (event: 'save'): void
  (event: 'open-attachment', target: 'img' | 'mediaUrl'): void
}>()

const modalTitle = computed(() => (mode.value === 'edit' ? '编辑分享卡片' : '新建分享卡片'))

const submitLabel = computed(() => (mode.value === 'edit' ? '保存' : '创建'))

const filePreviewPrimary = computed(() => {
  const dn = form.value.displayName.trim()
  const t = form.value.title.trim()
  const p = dn || t
  return p || '文件下载'
})

const filePreviewSecondary = computed(() => {
  const dn = form.value.displayName.trim()
  const t = form.value.title.trim()
  if (dn && t && dn !== t) {
    return t
  }
  return ''
})

const fileCardHeadline = computed({
  get() {
    const d = form.value.displayName.trim()
    const t = form.value.title.trim()
    return d || t
  },
  set(v: string) {
    const s = v ?? ''
    form.value.displayName = s
    form.value.title = s
  },
})

const imageCardHeadline = computed({
  get() {
    const d = form.value.displayName.trim()
    const t = form.value.title.trim()
    return d || t
  },
  set(v: string) {
    const s = v ?? ''
    form.value.displayName = s
    form.value.title = s
  },
})

const audioCardHeadline = computed({
  get() {
    const d = form.value.displayName.trim()
    const t = form.value.title.trim()
    return d || t
  },
  set(v: string) {
    const s = v ?? ''
    form.value.displayName = s
    form.value.title = s
  },
})

const audioPreviewArtist = computed(() => {
  const d = form.value.description.trim()
  return d || '纯音乐，请欣赏'
})

const cardNotesPreviewRows = computed((): FileNotePreviewRow[] => {
  if (form.value.cardKind !== 'file' && form.value.cardKind !== 'image') return []
  const out: FileNotePreviewRow[] = []
  for (const n of form.value.fileNotes || []) {
    const t = (n.title || '').trim()
    const d = (n.detail || '').trim()
    const u = (n.url || '').trim()
    const jump = !!n.jumpLink
    if (!t && !d && !(jump && u)) continue
    if (jump && u) {
      out.push({
        mode: 'link',
        title: t || '链接',
        detail: d,
        url: u,
      })
    } else if (t || d) {
      out.push({ mode: 'text', title: t, detail: d })
    }
  }
  return out
})

const videoCardHeadline = computed({
  get() {
    const vt = form.value.videoTitle.trim()
    const pt = form.value.title.trim()
    return vt || pt
  },
  set(v: string) {
    const s = v ?? ''
    form.value.videoTitle = s
    form.value.title = s
  },
})

const videoPreviewHeadline = computed(() => {
  const h = form.value.videoTitle.trim() || form.value.title.trim()
  return h || '视频'
})

const previewHeadline = computed(() => {
  if (form.value.cardKind === 'video') {
    return videoPreviewHeadline.value
  }
  if (form.value.cardKind === 'file') {
    return filePreviewPrimary.value
  }
  if (form.value.cardKind === 'image') {
    const h = form.value.displayName.trim() || form.value.title.trim()
    return h || '图片'
  }
  if (form.value.cardKind === 'audio') {
    const h = form.value.displayName.trim() || form.value.title.trim()
    return h || '未命名曲目'
  }
  return form.value.title.trim() || '页面标题'
})

const previewSub = computed(() => {
  if (form.value.cardKind === 'link') {
    return form.value.description.trim()
  }
  if (form.value.cardKind === 'video') {
    return form.value.videoGuideText.trim() || form.value.description.trim()
  }
  return form.value.description.trim()
})

const videoPreviewCaption = computed(() => form.value.videoGuideText.trim())

const coverPreviewSrc = computed(() => {
  const raw = props.form.img.trim()
  if (!raw) return ''
  return appendDisplayCacheBust(raw)
})

const vvPosterStyle = computed(() => {
  const u = coverPreviewSrc.value
  if (!u) return {}
  const safe = u.replace(/\\/g, '\\\\').replace(/"/g, '\\"')
  return {
    backgroundImage: `url("${safe}")`,
    backgroundSize: 'cover',
    backgroundPosition: 'center',
  }
})

watch(visible, (v) => {
  if (v && mode.value === 'create') {
  }
})

const cardKindEditorItems: { label: string; value: CardKind }[] = [
  { label: '链接', value: 'link' },
  { label: '图片', value: 'image' },
  { label: '音频', value: 'audio' },
  { label: '视频', value: 'video' },
  { label: '文件', value: 'file' },
]

function onCardKindPicked(v: string | number | boolean | undefined) {
  const s = v == null ? 'link' : String(v)
  const k = (['link', 'image', 'audio', 'video', 'file'].includes(s) ? s : 'link') as CardKind
  if (k === form.value.cardKind) return
  form.value.cardKind = k
  onKindChange()
}

function onKindChange() {
  if (form.value.cardKind === 'link') {
    form.value.mediaUrl = ''
    form.value.displayName = ''
    form.value.videoTitle = ''
    form.value.videoGuideText = ''
    form.value.videoExtraLink = ''
    form.value.videoExtraLinkLabel = ''
  }
}

function emptyFileNote(): FileNoteFormItem {
  return { title: '', detail: '', jumpLink: false, url: '' }
}

function addFileNote() {
  if (!form.value.fileNotes) {
    form.value.fileNotes = []
  }
  if (form.value.fileNotes.length >= 20) return
  form.value.fileNotes.push(emptyFileNote())
}

function removeFileNote(index: number) {
  form.value.fileNotes.splice(index, 1)
}
</script>

<template>
  <VModal :visible="visible" :title="modalTitle" :width="960" @close="emit('close')">
    <div class="editor" :class="{ 'editor--no-preview': form.cardKind === 'link' }">
      <div class="editor__main">
        <div v-if="mode === 'edit' && sid" class="plugin-field">
          <label class="plugin-label" for="wechat-share-sid">SID（不可修改）</label>
          <input id="wechat-share-sid" :value="sid" type="text" class="plugin-control plugin-control--readonly" readonly />
        </div>

        <div class="plugin-field">
          <label class="plugin-label" for="ws-card-kind">卡片类型</label>
          <WechatShareFilterLikeSelect
            :model-value="form.cardKind"
            :items="cardKindEditorItems"
            field-id="ws-card-kind"
            aria-label="卡片类型"
            :disabled="mode === 'edit'"
            @update:model-value="onCardKindPicked"
          />
          <p v-if="mode === 'edit'" class="plugin-hint plugin-hint--tight">
            编辑模式下不可更改类型，避免与已生成分享数据不一致。
          </p>
        </div>

        <!-- LINK -->
        <template v-if="form.cardKind === 'link'">
          <div class="plugin-field">
            <label class="plugin-label" for="ws-title">标题</label>
            <input id="ws-title" v-model="form.title" class="plugin-control" maxlength="32" autocomplete="off" />
            <p v-if="errors.title" class="plugin-error">{{ errors.title }}</p>
          </div>
          <div class="plugin-field">
            <label class="plugin-label" for="ws-desc">摘要</label>
            <input id="ws-desc" v-model="form.description" class="plugin-control" maxlength="32" autocomplete="off" />
            <p v-if="errors.description" class="plugin-error">{{ errors.description }}</p>
          </div>
          <div class="plugin-field">
            <label class="plugin-label" for="ws-img">封面图</label>
            <div class="plugin-inline-row">
              <input id="ws-img" v-model="form.img" class="plugin-control plugin-control--grow" placeholder="https://… 或 /attachments/…" />
              <VButton size="sm" type="secondary" class="plugin-attach-btn" @click="emit('open-attachment', 'img')">
                选择附件
              </VButton>
            </div>
            <p v-if="errors.img" class="plugin-error">{{ errors.img }}</p>
          </div>
          <div class="plugin-field">
            <label class="plugin-label" for="ws-go">跳转链接</label>
            <input id="ws-go" v-model="form.redirectUrl" class="plugin-control" autocomplete="off" placeholder="https://…" />
            <p v-if="errors.redirectUrl" class="plugin-error">{{ errors.redirectUrl }}</p>
          </div>
        </template>

        <!-- IMAGE -->
        <template v-else-if="form.cardKind === 'image'">
          <div class="plugin-field">
            <label class="plugin-label" for="ws-img-headline">页面标题 / 图片名称 / 卡片标题</label>
            <input
              id="ws-img-headline"
              v-model="imageCardHeadline"
              class="plugin-control"
              maxlength="128"
              autocomplete="off"
              placeholder="展示在落地页主标题，亦用于分享卡片标题"
            />
            <p v-if="errors.displayName" class="plugin-error">{{ errors.displayName }}</p>
            <p v-else-if="errors.title" class="plugin-error">{{ errors.title }}</p>
          </div>
          <div class="plugin-field">
            <label class="plugin-label" for="ws-img-res">图片资源</label>
            <div class="plugin-inline-row">
              <input id="ws-img-res" v-model="form.img" class="plugin-control plugin-control--grow" placeholder="图片直链或站内附件路径" />
              <VButton size="sm" type="secondary" @click="emit('open-attachment', 'img')">选择附件</VButton>
            </div>
            <p v-if="errors.img" class="plugin-error">{{ errors.img }}</p>
          </div>
          <div class="plugin-field">
            <label class="plugin-label" for="ws-img-intro">图片介绍 / 卡片摘要</label>
            <textarea id="ws-img-intro" v-model="form.description" class="plugin-textarea" rows="3" maxlength="512" />
            <p v-if="errors.description" class="plugin-error">{{ errors.description }}</p>
          </div>

          <div class="plugin-field plugin-field--subcard">
            <div class="plugin-subcard-hd">
              <span class="plugin-label plugin-label--inline">相关说明</span>
              <p class="plugin-hint plugin-hint--tight">可添加多条：仅展示提示文字，或带链接的说明行（最多 20 条）。</p>
            </div>
            <p v-if="errors.fileNotes" class="plugin-error">{{ errors.fileNotes }}</p>
            <div v-for="(note, idx) in form.fileNotes" :key="'img-' + idx" class="plugin-note-block">
              <div class="plugin-note-block__head">
                <span class="plugin-note-idx">第 {{ idx + 1 }} 条</span>
                <VButton size="sm" type="danger" @click="removeFileNote(idx)">删除</VButton>
              </div>
              <div class="plugin-field plugin-field--nested">
                <label class="plugin-label" :for="'ws-img-fn-t-' + idx">标题</label>
                <input
                  :id="'ws-img-fn-t-' + idx"
                  v-model="note.title"
                  class="plugin-control"
                  maxlength="128"
                  autocomplete="off"
                  placeholder="例如：查看原图"
                />
              </div>
              <div class="plugin-field plugin-field--nested">
                <label class="plugin-label" :for="'ws-img-fn-d-' + idx">说明文案</label>
                <textarea
                  :id="'ws-img-fn-d-' + idx"
                  v-model="note.detail"
                  class="plugin-textarea"
                  rows="2"
                  maxlength="512"
                  placeholder="展示在标题下方；跳转模式下可作为副标题"
                />
              </div>
              <div class="plugin-field plugin-field--nested plugin-field--row">
                <label class="plugin-check">
                  <input v-model="note.jumpLink" type="checkbox" />
                  <span>点击跳转到链接（关闭则仅展示上方文案）</span>
                </label>
              </div>
              <div v-if="note.jumpLink" class="plugin-field plugin-field--nested">
                <label class="plugin-label" :for="'ws-img-fn-u-' + idx">链接地址</label>
                <input :id="'ws-img-fn-u-' + idx" v-model="note.url" class="plugin-control" autocomplete="off" placeholder="https://…" />
              </div>
            </div>
            <VButton size="sm" type="secondary" class="plugin-add-note" :disabled="form.fileNotes.length >= 20" @click="addFileNote">
              添加一条说明
            </VButton>
          </div>

          <div class="plugin-field">
            <label class="plugin-label" for="ws-img-contact">页面底部自定义文案</label>
            <textarea id="ws-img-contact" v-model="form.contactInfo" class="plugin-textarea" rows="2" maxlength="512" />
            <p v-if="errors.contactInfo" class="plugin-error">{{ errors.contactInfo }}</p>
          </div>
        </template>

        <!-- AUDIO -->
        <template v-else-if="form.cardKind === 'audio'">
          <div class="plugin-field">
            <label class="plugin-label" for="ws-au-headline">页面标题 / 音频名称 / 卡片标题</label>
            <input
              id="ws-au-headline"
              v-model="audioCardHeadline"
              class="plugin-control"
              maxlength="128"
              autocomplete="off"
              placeholder="展示在落地页主标题，亦用于分享卡片标题"
            />
            <p v-if="errors.displayName" class="plugin-error">{{ errors.displayName }}</p>
            <p v-else-if="errors.title" class="plugin-error">{{ errors.title }}</p>
          </div>
          <div class="plugin-field">
            <label class="plugin-label" for="ws-au-audio">音频文件</label>
            <div class="plugin-inline-row">
              <input id="ws-au-audio" v-model="form.mediaUrl" class="plugin-control plugin-control--grow" placeholder="音频直链" />
              <VButton size="sm" type="secondary" @click="emit('open-attachment', 'mediaUrl')">选择附件</VButton>
            </div>
            <p v-if="errors.mediaUrl" class="plugin-error">{{ errors.mediaUrl }}</p>
          </div>
          <div class="plugin-field">
            <label class="plugin-label" for="ws-au-intro">音乐介绍 / 卡片摘要</label>
            <textarea id="ws-au-intro" v-model="form.description" class="plugin-textarea" rows="3" maxlength="512" />
            <p v-if="errors.description" class="plugin-error">{{ errors.description }}</p>
          </div>
          <div class="plugin-field">
            <label class="plugin-label" for="ws-au-cover">音频封面</label>
            <div class="plugin-inline-row">
              <input id="ws-au-cover" v-model="form.img" class="plugin-control plugin-control--grow" placeholder="封面图 URL" />
              <VButton size="sm" type="secondary" @click="emit('open-attachment', 'img')">选择附件</VButton>
            </div>
            <p v-if="errors.img" class="plugin-error">{{ errors.img }}</p>
          </div>
          <div class="plugin-field">
            <label class="plugin-label" for="ws-au-contact">页面底部自定义文案</label>
            <textarea id="ws-au-contact" v-model="form.contactInfo" class="plugin-textarea" rows="2" maxlength="512" />
            <p v-if="errors.contactInfo" class="plugin-error">{{ errors.contactInfo }}</p>
          </div>
        </template>

        <!-- VIDEO -->
        <template v-else-if="form.cardKind === 'video'">
          <div class="plugin-field">
            <label class="plugin-label" for="ws-v-headline">页面标题 / 视频标题 / 卡片标题</label>
            <input
              id="ws-v-headline"
              v-model="videoCardHeadline"
              class="plugin-control"
              maxlength="128"
              autocomplete="off"
              placeholder="展示在落地页主标题，亦用于分享卡片标题"
            />
            <p v-if="errors.videoTitle" class="plugin-error">{{ errors.videoTitle }}</p>
            <p v-else-if="errors.title" class="plugin-error">{{ errors.title }}</p>
          </div>
          <div class="plugin-field">
            <label class="plugin-label" for="ws-v-file">视频文件</label>
            <div class="plugin-inline-row">
              <input id="ws-v-file" v-model="form.mediaUrl" class="plugin-control plugin-control--grow" placeholder="视频直链" />
              <VButton size="sm" type="secondary" @click="emit('open-attachment', 'mediaUrl')">选择附件</VButton>
            </div>
            <p v-if="errors.mediaUrl" class="plugin-error">{{ errors.mediaUrl }}</p>
          </div>
          <div class="plugin-field">
            <label class="plugin-label" for="ws-v-cover">视频封面</label>
            <div class="plugin-inline-row">
              <input id="ws-v-cover" v-model="form.img" class="plugin-control plugin-control--grow" placeholder="封面 URL" />
              <VButton size="sm" type="secondary" @click="emit('open-attachment', 'img')">选择附件</VButton>
            </div>
            <p v-if="errors.img" class="plugin-error">{{ errors.img }}</p>
          </div>
          <div class="plugin-field">
            <label class="plugin-label" for="ws-v-guide">视频介绍 / 卡片摘要</label>
            <textarea id="ws-v-guide" v-model="form.videoGuideText" class="plugin-textarea" rows="3" maxlength="512" />
            <p v-if="errors.videoGuideText" class="plugin-error">{{ errors.videoGuideText }}</p>
          </div>
          <div class="plugin-field">
            <label class="plugin-label" for="ws-v-extra-label">相关链接文案（可选）</label>
            <input
              id="ws-v-extra-label"
              v-model="form.videoExtraLinkLabel"
              class="plugin-control"
              maxlength="64"
              autocomplete="off"
              placeholder="留空则前台显示为「相关链接」"
            />
            <p v-if="errors.videoExtraLinkLabel" class="plugin-error">{{ errors.videoExtraLinkLabel }}</p>
          </div>
          <div class="plugin-field">
            <label class="plugin-label" for="ws-v-extra">附加链接地址（可选）</label>
            <input id="ws-v-extra" v-model="form.videoExtraLink" class="plugin-control" autocomplete="off" />
            <p class="plugin-hint">填写链接地址后，前台底部胶囊可跳转；可自定义上方「相关链接文案」。</p>
            <p v-if="errors.videoExtraLink" class="plugin-error">{{ errors.videoExtraLink }}</p>
          </div>
        </template>

        <!-- FILE -->
        <template v-else-if="form.cardKind === 'file'">
          <div class="plugin-field">
            <label class="plugin-label" for="ws-f-headline">文件名称 / 页面标题 / 卡片标题</label>
            <input
              id="ws-f-headline"
              v-model="fileCardHeadline"
              class="plugin-control"
              maxlength="128"
              autocomplete="off"
              placeholder="展示在落地页主标题，亦用于分享卡片标题"
            />
            <p v-if="errors.displayName" class="plugin-error">{{ errors.displayName }}</p>
            <p v-else-if="errors.title" class="plugin-error">{{ errors.title }}</p>
          </div>
          <div class="plugin-field">
            <label class="plugin-label" for="ws-f-down">文件下载地址</label>
            <div class="plugin-inline-row">
              <input id="ws-f-down" v-model="form.mediaUrl" class="plugin-control plugin-control--grow" placeholder="文件直链" />
              <VButton size="sm" type="secondary" @click="emit('open-attachment', 'mediaUrl')">选择附件</VButton>
            </div>
            <p v-if="errors.mediaUrl" class="plugin-error">{{ errors.mediaUrl }}</p>
          </div>
          <div class="plugin-field">
            <label class="plugin-label" for="ws-f-intro">文件介绍 / 卡片摘要</label>
            <textarea id="ws-f-intro" v-model="form.description" class="plugin-textarea" rows="3" maxlength="512" />
            <p v-if="errors.description" class="plugin-error">{{ errors.description }}</p>
          </div>
          <div class="plugin-field">
            <label class="plugin-label" for="ws-f-cover">文件封面</label>
            <div class="plugin-inline-row">
              <input id="ws-f-cover" v-model="form.img" class="plugin-control plugin-control--grow" />
              <VButton size="sm" type="secondary" @click="emit('open-attachment', 'img')">选择附件</VButton>
            </div>
            <p v-if="errors.img" class="plugin-error">{{ errors.img }}</p>
          </div>

          <div class="plugin-field plugin-field--subcard">
            <div class="plugin-subcard-hd">
              <span class="plugin-label plugin-label--inline">相关说明</span>
              <p class="plugin-hint plugin-hint--tight">可添加多条：仅展示提示文字，或带链接的说明行（最多 20 条）。</p>
            </div>
            <p v-if="errors.fileNotes" class="plugin-error">{{ errors.fileNotes }}</p>
            <div v-for="(note, idx) in form.fileNotes" :key="idx" class="plugin-note-block">
              <div class="plugin-note-block__head">
                <span class="plugin-note-idx">第 {{ idx + 1 }} 条</span>
                <VButton size="sm" type="danger" @click="removeFileNote(idx)">删除</VButton>
              </div>
              <div class="plugin-field plugin-field--nested">
                <label class="plugin-label" :for="'ws-fn-t-' + idx">标题</label>
                <input
                  :id="'ws-fn-t-' + idx"
                  v-model="note.title"
                  class="plugin-control"
                  maxlength="128"
                  autocomplete="off"
                  placeholder="例如：使用说明"
                />
              </div>
              <div class="plugin-field plugin-field--nested">
                <label class="plugin-label" :for="'ws-fn-d-' + idx">说明文案</label>
                <textarea
                  :id="'ws-fn-d-' + idx"
                  v-model="note.detail"
                  class="plugin-textarea"
                  rows="2"
                  maxlength="512"
                  placeholder="展示在标题下方；跳转模式下可作为副标题"
                />
              </div>
              <div class="plugin-field plugin-field--nested plugin-field--row">
                <label class="plugin-check">
                  <input v-model="note.jumpLink" type="checkbox" />
                  <span>点击跳转到链接（关闭则仅展示上方文案）</span>
                </label>
              </div>
              <div v-if="note.jumpLink" class="plugin-field plugin-field--nested">
                <label class="plugin-label" :for="'ws-fn-u-' + idx">链接地址</label>
                <input :id="'ws-fn-u-' + idx" v-model="note.url" class="plugin-control" autocomplete="off" placeholder="https://…" />
              </div>
            </div>
            <VButton size="sm" type="secondary" class="plugin-add-note" :disabled="form.fileNotes.length >= 20" @click="addFileNote">
              添加一条说明
            </VButton>
          </div>

          <div class="plugin-field">
            <label class="plugin-label" for="ws-f-contact">页面底部自定义文案</label>
            <textarea id="ws-f-contact" v-model="form.contactInfo" class="plugin-textarea" rows="2" maxlength="512" />
            <p v-if="errors.contactInfo" class="plugin-error">{{ errors.contactInfo }}</p>
          </div>
        </template>

        <p v-if="errors.general" class="plugin-error">{{ errors.general }}</p>
      </div>

      <aside v-if="form.cardKind !== 'link'" class="editor__aside" aria-label="实时预览">
        <div class="preview">
          <div class="preview__device" :class="'preview__device--' + form.cardKind">
            <div class="preview__body">
              <div v-if="form.cardKind === 'image'" class="pv-img-shell">
                <div class="pv-img-stack">
                  <div class="pv-img-card">
                    <div v-if="form.img.trim()" class="pv-img-frame">
                      <img
                        :key="coverPreviewSrc"
                        class="pv-img-photo"
                        :src="coverPreviewSrc"
                        alt=""
                        loading="lazy"
                        decoding="async"
                      />
                    </div>
                    <div class="pv-img-body">
                      <h1 class="pv-img-title">{{ previewHeadline }}</h1>
                      <p v-if="previewSub" class="pv-img-intro">{{ previewSub }}</p>
                      <p v-if="form.contactInfo.trim()" class="pv-img-contact">{{ form.contactInfo }}</p>
                      <p class="pv-img-sdk-ph" aria-hidden="true" />
                      <p class="pv-img-foot">在右上角分享给好友或群，可查看自定义卡片效果（微信 / QQ）</p>
                    </div>
                  </div>
                  <div v-if="cardNotesPreviewRows.length" class="pv-in-card">
                    <div class="pv-in-section">相关说明</div>
                    <div class="pv-in-links">
                      <template v-for="(row, ri) in cardNotesPreviewRows" :key="'pvimg-' + ri">
                        <a
                          v-if="row.mode === 'link'"
                          class="pv-in-link"
                          :href="row.url"
                          rel="noopener"
                          @click.prevent
                        >
                          <span class="pv-in-link-main">
                            <span class="pv-in-link-t">{{ row.title }}</span>
                            <span v-if="row.detail" class="pv-in-link-d">{{ row.detail }}</span>
                            <span v-else class="pv-in-link-d">在浏览器中打开</span>
                          </span>
                          <span class="pv-in-arrow" aria-hidden="true" />
                        </a>
                        <div v-else class="pv-in-note">
                          <p v-if="row.title" class="pv-in-note-t">{{ row.title }}</p>
                          <p v-if="row.detail" class="pv-in-note-d">{{ row.detail }}</p>
                        </div>
                      </template>
                    </div>
                  </div>
                </div>
              </div>

              <div v-else-if="form.cardKind === 'audio'" class="pv-au">
                <div class="pv-au-shell">
                  <div class="pv-au-stage">
                    <div class="pv-au-arm" aria-hidden="true"><span class="pv-au-arm-head"></span></div>
                    <div class="pv-au-disc-outer">
                      <div class="pv-au-groove" aria-hidden="true"></div>
                      <div class="pv-au-disc">
                        <div class="pv-au-art-wrap">
                          <img
                            v-if="form.img.trim()"
                            :key="coverPreviewSrc"
                            class="pv-au-art"
                            :src="coverPreviewSrc"
                            alt=""
                            loading="lazy"
                          />
                          <div v-else class="pv-au-art pv-au-art--ph" aria-hidden="true"></div>
                        </div>
                        <div class="pv-au-play" aria-hidden="true">
                          <span class="pv-au-play__ring"></span>
                          <span class="pv-au-play__ic"></span>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="pv-au-scrub">
                    <div class="pv-au-time"><span>0:00</span><span class="pv-au-sep">/</span><span>0:00</span></div>
                    <div class="pv-au-track"><span class="pv-au-fill"></span></div>
                  </div>
                  <div class="pv-au-meta">
                    <p class="pv-au-title">{{ previewHeadline }}</p>
                    <p class="pv-au-artist">{{ audioPreviewArtist }}</p>
                  </div>
                </div>
              </div>

              <div v-else-if="form.cardKind === 'video'" class="pv-vv">
                  <div class="pv-vv-shell">
                  <div class="pv-vv-video" :key="coverPreviewSrc || 'vv-poster'" :style="vvPosterStyle">
                    <div class="pv-vv-grad" aria-hidden="true"></div>
                    <span class="pv-vv-play" aria-hidden="true"></span>
                    <div class="pv-vv-dock">
                      <a
                        v-if="form.videoExtraLink.trim()"
                        class="pv-vv-chip"
                        :href="form.videoExtraLink"
                        rel="noopener"
                        @click.prevent
                      >
                        <span class="pv-vv-chip-dot" aria-hidden="true"></span>
                        <span class="pv-vv-chip-t">{{ form.videoExtraLinkLabel.trim() || '相关链接' }}</span>
                        <span class="pv-vv-chip-go" aria-hidden="true"></span>
                      </a>
                      <p class="pv-vv-title">{{ videoPreviewHeadline }}</p>
                      <p v-if="videoPreviewCaption" class="pv-vv-desc">{{ videoPreviewCaption }}</p>
                      <div class="pv-vv-prog" aria-hidden="true"><span class="pv-vv-prog-fill"></span></div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-else-if="form.cardKind === 'file'" class="pv-fp">
                <div class="pv-fp-container">
                  <div class="pv-fp-card">
                    <div class="pv-fp-cover-wrap">
                      <img
                        v-if="form.img.trim()"
                        :key="coverPreviewSrc"
                        class="pv-fp-cover"
                        :src="coverPreviewSrc"
                        alt=""
                        loading="lazy"
                      />
                      <div v-else class="pv-fp-cover pv-fp-cover--ph" aria-hidden="true" />
                    </div>
                    <p class="pv-fp-title">{{ filePreviewPrimary }}</p>
                    <p v-if="filePreviewSecondary" class="pv-fp-sub">{{ filePreviewSecondary }}</p>
                    <p v-if="previewSub" class="pv-fp-desc">{{ previewSub }}</p>
                    <div class="pv-fp-sdk-ph" aria-hidden="true" />
                    <span class="pv-fp-dl">下载</span>
                    <p class="pv-fp-tip">若微信内拦截下载，可使用右上角菜单「在浏览器打开」。</p>
                  </div>

                  <div v-if="cardNotesPreviewRows.length" class="pv-fp-card pv-fp-notes-card">
                    <div class="pv-fp-section">相关说明</div>
                    <div class="pv-fp-links">
                      <template v-for="(row, ri) in cardNotesPreviewRows" :key="ri">
                        <a
                          v-if="row.mode === 'link'"
                          class="pv-fp-link"
                          :href="row.url"
                          rel="noopener"
                          @click.prevent
                        >
                          <span class="pv-fp-link-main">
                            <span class="pv-fp-link-t">{{ row.title }}</span>
                            <span v-if="row.detail" class="pv-fp-link-d">{{ row.detail }}</span>
                            <span v-else class="pv-fp-link-d">在浏览器中打开</span>
                          </span>
                          <span class="pv-fp-arrow" aria-hidden="true" />
                        </a>
                        <div v-else class="pv-fp-note">
                          <p v-if="row.title" class="pv-fp-note-t">{{ row.title }}</p>
                          <p v-if="row.detail" class="pv-fp-note-d">{{ row.detail }}</p>
                        </div>
                      </template>
                    </div>
                  </div>
                </div>
              </div>

              <p class="preview__tip">预览与前台落地页结构同步</p>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <template #footer>
      <div class="plugin-modal-footer">
        <div class="plugin-footer-start" />
        <div class="plugin-footer-actions">
          <VButton :disabled="saving" @click="emit('close')">取消</VButton>
          <VButton type="primary" :loading="saving" :disabled="saving" @click="emit('save')">{{ submitLabel }}</VButton>
        </div>
      </div>
    </template>
  </VModal>
</template>

<style scoped>
.editor {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
  align-items: start;
}

.editor.editor--no-preview {
  grid-template-columns: 1fr;
}

@media (max-width: 980px) {
  .editor {
    grid-template-columns: 1fr;
  }
  .editor__aside {
    position: static;
  }
}

.editor__main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1.125rem;
}

.editor__aside {
  position: sticky;
  top: 0;
}

.preview__device {
  border-radius: 18px;
  border: 1px solid #e7e7ea;
  background: linear-gradient(180deg, #fbfbfc 0%, #ffffff 40%);
  box-shadow: 0 14px 45px rgb(15 23 42 / 0.08);
  overflow: hidden;
}


.preview__body {
  padding: 14px;
}

.preview__device--video {
  background: #000;
}

.preview__device--video .preview__bar {
  opacity: 1;
}

.preview__device--file {
  background: #f6f7f9;
}

.preview__device--image {
  background: linear-gradient(165deg, #eef2f7 0%, #f8fafc 42%, #f1f5f9 100%);
  border-color: rgb(15 23 42 / 0.08);
  box-shadow: 0 14px 45px rgb(15 23 42 / 0.07);
}

.preview__device--image .preview__body {
  padding: 10px 8px 12px;
}

.preview__device--file .preview__body {
  padding: 10px 8px 12px;
}

.preview__device--audio {
  background: #2c2e31;
  border-color: rgb(255 255 255 / 0.08);
  box-shadow: 0 14px 45px rgb(0 0 0 / 0.35);
}

.preview__device--audio .preview__tip {
  color: #9ca3af;
}

.preview__tip {
  margin: 10px 0 0;
  font-size: 0.75rem;
  line-height: 1.45;
  color: #94a3b8;
  text-align: center;
}

.pv-img-shell {
  color: #0f172a;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', Arial, 'Segoe UI', sans-serif;
  -webkit-font-smoothing: antialiased;
  box-sizing: border-box;
}

.pv-img-shell *,
.pv-img-shell *::before,
.pv-img-shell *::after {
  box-sizing: border-box;
}

.pv-img-stack {
  max-width: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pv-img-card {
  background: #fff;
  border: 1px solid rgb(15 23 42 / 0.06);
  border-radius: 18px;
  box-shadow:
    0 10px 40px rgb(15 23 42 / 0.07),
    0 1px 0 rgb(255 255 255 / 0.9) inset;
  overflow: hidden;
}

.pv-img-frame {
  background: linear-gradient(180deg, #f8fafc, #eef2f7);
}

.pv-img-photo {
  display: block;
  width: 100%;
  height: auto;
  vertical-align: middle;
}

.pv-img-body {
  padding: 14px 12px 12px;
}

.pv-img-title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 680;
  letter-spacing: -0.02em;
  line-height: 1.28;
  color: #0f172a;
  text-align: center;
}

.pv-img-intro {
  margin: 10px 0 0;
  font-size: 0.82rem;
  line-height: 1.65;
  color: #64748b;
  text-align: center;
}

.pv-img-contact {
  margin-top: 12px;
  font-size: 0.78rem;
  line-height: 1.55;
  color: #64748b;
  text-align: center;
}

.pv-img-sdk-ph {
  display: none;
}

.pv-img-foot {
  margin: 12px 0 0;
  padding-top: 10px;
  border-top: 1px solid #eef2f7;
  font-size: 0.72rem;
  line-height: 1.55;
  color: #94a3b8;
  text-align: center;
}

/* 图片卡片「相关说明」：前台为 in-*，非 fp-* */
.pv-in-card {
  background: #fff;
  border-radius: 16px;
  padding: 12px 12px;
  border: 1px solid rgb(15 23 42 / 0.06);
  box-shadow: 0 4px 22px rgb(15 23 42 / 0.05);
}

.pv-in-section {
  font-size: 0.82rem;
  font-weight: 650;
  margin: 0 0 8px;
  color: #0f172a;
}

.pv-in-links {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pv-in-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 12px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  text-decoration: none;
  color: #0f172a;
  background: #fff;
  cursor: default;
  transition:
    background 0.15s ease,
    transform 0.15s ease;
}

.pv-in-link:active {
  transform: scale(0.99);
  background: #f9fafb;
}

.pv-in-link-main {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
  text-align: left;
}

.pv-in-link-t {
  font-size: 0.8rem;
  font-weight: 550;
}

.pv-in-link-d {
  font-size: 0.68rem;
  color: #6b7280;
  margin-top: 2px;
  line-height: 1.35;
}

.pv-in-arrow {
  width: 7px;
  height: 7px;
  border-right: 2px solid #9ca3af;
  border-top: 2px solid #9ca3af;
  transform: rotate(45deg);
  flex-shrink: 0;
  margin-left: 8px;
}

.pv-in-note {
  padding: 11px 12px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #fafafa;
  text-align: left;
}

.pv-in-note-t {
  margin: 0;
  font-size: 0.8rem;
  font-weight: 550;
  color: #0f172a;
  line-height: 1.35;
}

.pv-in-note-d {
  margin: 5px 0 0;
  font-size: 0.68rem;
  color: #6b7280;
  line-height: 1.55;
}

/* 音频预览：对齐前台 WechatSharePageRenderer（body-au） */
.pv-au {
  color: #f3f4f6;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', Arial, 'Segoe UI', sans-serif;
  -webkit-font-smoothing: antialiased;
  border-radius: 12px;
  overflow: hidden;
}

.pv-au-shell {
  padding: 10px 8px 12px;
}

.pv-au-stage {
  position: relative;
  display: flex;
  justify-content: center;
  padding: 16px 0 4px;
}

.pv-au-arm {
  position: absolute;
  top: 4px;
  left: 50%;
  width: min(52%, 132px);
  height: 3px;
  margin-left: 5px;
  background: linear-gradient(90deg, rgb(255 255 255 / 0.08), rgb(255 255 255 / 0.9) 55%, #f8fafc);
  border-radius: 3px;
  transform: rotate(-26deg);
  transform-origin: 100% 50%;
  opacity: 0.95;
  box-shadow: 0 1px 0 rgb(0 0 0 / 0.4), inset 0 0 0 1px rgb(255 255 255 / 0.06);
}

.pv-au-arm-head {
  position: absolute;
  right: -1px;
  top: 50%;
  transform: translateY(-50%);
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: linear-gradient(145deg, #fff, #e5e7eb);
  box-shadow: 0 0 0 1px rgb(0 0 0 / 0.28), 0 2px 5px rgb(0 0 0 / 0.35);
}

.pv-au-disc-outer {
  position: relative;
  width: 168px;
  height: 168px;
  border-radius: 50%;
  background: radial-gradient(circle at 50% 42%, #1a1a1a 0 58%, #050505 58% 70%, #0d0d0d 70% 100%);
  box-shadow:
    0 18px 40px rgb(0 0 0 / 0.55),
    0 0 0 1px rgb(255 255 255 / 0.06),
    inset 0 0 32px rgb(0 0 0 / 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
}

.pv-au-groove {
  position: absolute;
  inset: 5%;
  border-radius: 50%;
  pointer-events: none;
  background: repeating-radial-gradient(
    circle at 50% 50%,
    transparent 0 2px,
    rgb(255 255 255 / 0.02) 2px 3px
  );
  opacity: 0.55;
}

.pv-au-disc {
  width: 82%;
  height: 82%;
  border-radius: 50%;
  position: relative;
  overflow: hidden;
  z-index: 1;
  box-shadow:
    inset 0 0 0 8px #080808,
    inset 0 0 0 12px rgb(255 255 255 / 0.04),
    inset 0 0 18px rgb(0 0 0 / 0.5);
}

.pv-au-art-wrap {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  overflow: hidden;
  background: #111827;
}

.pv-au-art {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  opacity: 0.97;
}

.pv-au-art--ph {
  min-height: 100%;
  background: radial-gradient(circle at 36% 30%, #6b7280, #111827 62%);
}

.pv-au-play {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 44px;
  height: 44px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pv-au-play__ring {
  position: absolute;
  inset: 0;
  border-radius: 999px;
  background: linear-gradient(160deg, rgb(255 255 255 / 0.22), rgb(0 0 0 / 0.55));
  box-shadow:
    0 0 0 1px rgb(255 255 255 / 0.22),
    0 8px 20px rgb(0 0 0 / 0.45),
    inset 0 1px 0 rgb(255 255 255 / 0.25);
  backdrop-filter: blur(8px);
}

.pv-au-play__ic {
  position: relative;
  z-index: 1;
  width: 18px;
  height: 18px;
  display: block;
}

.pv-au-play__ic::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-38%, -50%);
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 7px 0 7px 11px;
  border-color: transparent transparent transparent #fff;
  filter: drop-shadow(0 1px 1px rgb(0 0 0 / 0.35));
}

.pv-au-scrub {
  margin-top: 12px;
  padding: 0 4px;
}

.pv-au-time {
  font-size: 0.68rem;
  color: #9ca3af;
  font-variant-numeric: tabular-nums;
  text-align: center;
  letter-spacing: 0.02em;
}

.pv-au-sep {
  margin: 0 5px;
  opacity: 0.45;
}

.pv-au-track {
  height: 4px;
  border-radius: 999px;
  background: rgb(255 255 255 / 0.1);
  overflow: hidden;
  margin-top: 8px;
}

.pv-au-fill {
  display: block;
  width: 36%;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #e5e7eb, #f9fafb);
  opacity: 0.92;
}

.pv-au-meta {
  margin-top: 14px;
  padding: 0 4px;
  text-align: center;
}

.pv-au-title {
  margin: 0;
  font-size: 0.86rem;
  font-weight: 640;
  letter-spacing: -0.02em;
  line-height: 1.28;
  color: #fff;
}

.pv-au-artist {
  margin: 8px auto 0;
  max-width: 17rem;
  font-size: 0.72rem;
  line-height: 1.45;
  color: #9ca3af;
}

.pv-vv-shell {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgb(255 255 255 / 0.1);
  background: #000;
}

.pv-vv-video {
  aspect-ratio: 9 / 16;
  background: #0a0a0a;
  position: relative;
  overflow: hidden;
  width: 100%;
}

.pv-vv-grad {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 52%;
  pointer-events: none;
  background: linear-gradient(
    180deg,
    transparent 0%,
    rgb(0 0 0 / 0.14) 38%,
    rgb(0 0 0 / 0.55) 72%,
    rgb(0 0 0 / 0.88) 100%
  );
}

.pv-vv-play {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 12px 0 12px 20px;
  border-color: transparent transparent transparent rgb(255 255 255 / 0.92);
  margin-left: 5px;
  filter: drop-shadow(0 2px 10px rgb(0 0 0 / 0.35));
}

.pv-vv-dock {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 0 10px 10px;
  text-align: left;
}

.pv-vv-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin: 0 0 8px;
  padding: 4px 9px 4px 7px;
  border-radius: 999px;
  background: rgb(0 0 0 / 0.38);
  border: 1px solid rgb(255 255 255 / 0.14);
  color: #f9fafb;
  font-size: 0.65rem;
  font-weight: 600;
  text-decoration: none;
  max-width: 100%;
}

.pv-vv-chip-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: linear-gradient(145deg, #fb923c, #ea580c);
  flex-shrink: 0;
}

.pv-vv-chip-t {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.pv-vv-chip-go {
  width: 5px;
  height: 5px;
  margin-left: 1px;
  border-right: 1.5px solid rgb(255 255 255 / 0.55);
  border-top: 1.5px solid rgb(255 255 255 / 0.55);
  transform: rotate(45deg);
  flex-shrink: 0;
  opacity: 0.85;
}

.pv-vv-title {
  margin: 0;
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: -0.015em;
  line-height: 1.25;
  color: #fff;
}

.pv-vv-desc {
  margin: 6px 0 0;
  font-size: 0.72rem;
  line-height: 1.42;
  color: rgb(255 255 255 / 0.86);
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
}

.pv-vv-prog {
  margin-top: 8px;
  height: 3px;
  border-radius: 999px;
  background: rgb(255 255 255 / 0.18);
  overflow: hidden;
}

.pv-vv-prog-fill {
  display: block;
  width: 32%;
  height: 100%;
  border-radius: 999px;
  background: rgb(255 255 255 / 0.9);
}

/* 文件卡片预览：对齐前台 WechatSharePageRenderer（fp-*） */
.pv-fp {
  --pv-fp-bg: #f6f7f9;
  --pv-fp-card: #ffffff;
  --pv-fp-text: #1a1a1a;
  --pv-fp-sub: #6b7280;
  --pv-fp-primary: #2563eb;
  --pv-fp-border: #e5e7eb;
  --pv-fp-radius: 16px;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
  color: var(--pv-fp-text);
  background: var(--pv-fp-bg);
  border-radius: 12px;
  overflow: hidden;
}

.pv-fp-container {
  max-width: 100%;
  margin: 0 auto;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pv-fp-card {
  background: var(--pv-fp-card);
  border-radius: var(--pv-fp-radius);
  padding: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.pv-fp-cover-wrap {
  display: flex;
  justify-content: center;
  margin: 0 0 2px;
}

.pv-fp-cover {
  width: 90px;
  height: 90px;
  object-fit: cover;
  border-radius: 12px;
  background: #e5e7eb;
}

.pv-fp-cover--ph {
  min-height: 0;
}

.pv-fp-title {
  margin: 10px 0 4px;
  font-size: 0.95rem;
  font-weight: 600;
  line-height: 1.35;
  letter-spacing: -0.01em;
  text-align: center;
}

.pv-fp-sub {
  margin: 0 0 6px;
  font-size: 0.78rem;
  color: var(--pv-fp-sub);
  line-height: 1.45;
  text-align: center;
}

.pv-fp-desc {
  margin: 0;
  font-size: 0.78rem;
  color: var(--pv-fp-sub);
  line-height: 1.55;
  text-align: center;
}

.pv-fp-sdk-ph {
  display: none;
  margin-top: 10px;
  min-height: 0;
}

.pv-fp-dl {
  display: block;
  margin: 10px auto 0;
  max-width: 200px;
  width: 100%;
  padding: 7px 14px;
  border-radius: 10px;
  background: var(--pv-fp-primary);
  color: #fff;
  font-size: 0.78rem;
  font-weight: 600;
  text-align: center;
  box-sizing: border-box;
}

.pv-fp-tip {
  margin: 8px 0 0;
  font-size: 0.65rem;
  color: var(--pv-fp-sub);
  line-height: 1.45;
  text-align: center;
}

/* 相关说明：对齐前台 fp-section / fp-links / fp-link / fp-note */
.pv-fp-notes-card {
  margin-top: 0;
}

.pv-fp-section {
  font-size: 0.78rem;
  font-weight: 600;
  margin: 0 0 8px;
  color: var(--pv-fp-text);
}

.pv-fp-links {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pv-fp-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 11px;
  border-radius: 10px;
  border: 1px solid var(--pv-fp-border);
  text-decoration: none;
  color: var(--pv-fp-text);
  background: #fff;
  cursor: default;
}

.pv-fp-link-main {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
  text-align: left;
}

.pv-fp-link-t {
  font-size: 0.8rem;
  font-weight: 500;
  line-height: 1.3;
}

.pv-fp-link-d {
  font-size: 0.65rem;
  color: var(--pv-fp-sub);
  margin-top: 2px;
  line-height: 1.35;
}

.pv-fp-arrow {
  width: 6px;
  height: 6px;
  border-right: 2px solid #9ca3af;
  border-top: 2px solid #9ca3af;
  transform: rotate(45deg);
  flex-shrink: 0;
  margin-left: 8px;
}

.pv-fp-note {
  padding: 10px 11px;
  border-radius: 10px;
  border: 1px solid var(--pv-fp-border);
  background: #fafafa;
  text-align: left;
}

.pv-fp-note-t {
  margin: 0;
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--pv-fp-text);
  line-height: 1.35;
}

.pv-fp-note-d {
  margin: 5px 0 0;
  font-size: 0.68rem;
  color: var(--pv-fp-sub);
  line-height: 1.5;
}

.plugin-modal-form {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  min-width: 0;
}

.plugin-field {
  min-width: 0;
}

.plugin-field--nested {
  margin-bottom: 0;
}

.plugin-field--row {
  margin-bottom: 0;
}

.plugin-field--subcard {
  padding: 14px 14px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fafbfc;
}

.plugin-subcard-hd {
  margin-bottom: 10px;
}

.plugin-label--inline {
  margin-bottom: 0.25rem;
}

.plugin-hint--tight {
  margin-top: 0.2rem;
}

.plugin-note-block {
  margin-bottom: 12px;
  padding: 12px;
  border-radius: 10px;
  border: 1px solid #e8eaed;
  background: #fff;
}

.plugin-note-block__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.plugin-note-idx {
  font-size: 0.75rem;
  color: #64748b;
}

.plugin-add-note {
  margin-top: 4px;
}

.plugin-check {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 0.8125rem;
  color: #4b5563;
  line-height: 1.45;
  cursor: pointer;
}

.plugin-check input {
  margin-top: 2px;
}

.plugin-label {
  display: block;
  margin-bottom: 0.375rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
}

.plugin-field-help {
  margin: 0 0 0.5rem;
  font-size: 0.75rem;
  line-height: 1.45;
  color: #6b7280;
}

.plugin-field-help--after {
  margin: 0.5rem 0 0;
}

.plugin-hint {
  margin: 0.35rem 0 0;
  font-size: 0.75rem;
  line-height: 1.45;
  color: #6b7280;
}

.plugin-control {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  height: 2.25rem;
  padding: 0 0.75rem;
  font-size: 0.875rem;
  line-height: 1.25;
  color: #111827;
  background-color: #fff;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  outline: none;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.plugin-select-wrap {
  position: relative;
}

.plugin-select {
  appearance: none;
  padding-right: 2.25rem;
  background-image: none;
}

.plugin-select-ico {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  color: #64748b;
  pointer-events: none;
}

.plugin-control:hover {
  border-color: #9ca3af;
}

.plugin-control:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgb(59 130 246 / 0.15);
}

.plugin-control--readonly {
  color: #6b7280;
  background-color: #f9fafb;
  cursor: default;
}

.plugin-control--grow {
  flex: 1;
  width: auto;
  min-width: 0;
}

.plugin-textarea {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  padding: 0.55rem 0.75rem;
  font-size: 0.875rem;
  line-height: 1.45;
  color: #111827;
  background-color: #fff;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  outline: none;
  resize: vertical;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.plugin-textarea:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgb(59 130 246 / 0.15);
}

.plugin-inline-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
}

.plugin-inline-row--mt {
  margin-top: 0.5rem;
}

.plugin-attach-btn {
  flex-shrink: 0;
}

.plugin-mini {
  font-size: 0.75rem;
  color: #6b7280;
  line-height: 1.35;
}

.plugin-error {
  margin: 0.25rem 0 0;
  font-size: 0.75rem;
  line-height: 1.35;
  color: #dc2626;
}

.plugin-modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  width: 100%;
  flex-wrap: wrap;
}

.plugin-footer-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}
</style>
