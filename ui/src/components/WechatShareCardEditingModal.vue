<script setup lang="ts">
import { computed, toRefs } from 'vue'
import { VButton, VModal } from '@halo-dev/components'

export type WechatShareCardForm = {
  title: string
  description: string
  img: string
  redirectUrl: string
}

export type WechatShareCardFormErrors = {
  title?: string
  description?: string
  img?: string
  redirectUrl?: string
}

const props = withDefaults(
  defineProps<{
    visible: boolean
    saving: boolean
    form: WechatShareCardForm
    errors: WechatShareCardFormErrors
    mode?: 'create' | 'edit'
    /** 编辑模式下展示的 SID（只读） */
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
  (event: 'open-attachment'): void
}>()

const modalTitle = computed(() => (mode.value === 'edit' ? '编辑分享卡片' : '新建分享卡片'))

const submitLabel = computed(() => (mode.value === 'edit' ? '保存' : '创建'))
</script>

<template>
  <VModal :visible="visible" :title="modalTitle" @close="emit('close')">
    <div class="plugin-modal-form">
      <div v-if="mode === 'edit' && sid" class="plugin-field">
        <label class="plugin-label" for="wechat-share-sid">SID（不可修改）</label>
        <input id="wechat-share-sid" :value="sid" type="text" class="plugin-control plugin-control--readonly" readonly />
      </div>

      <div class="plugin-field">
        <label class="plugin-label" for="wechat-share-title">标题</label>
        <input
          id="wechat-share-title"
          v-model="form.title"
          type="text"
          class="plugin-control"
          maxlength="32"
          autocomplete="off"
          placeholder="微信分享标题"
        />
        <p v-if="errors.title" class="plugin-error">{{ errors.title }}</p>
      </div>

      <div class="plugin-field">
        <label class="plugin-label" for="wechat-share-desc">摘要</label>
        <input
          id="wechat-share-desc"
          v-model="form.description"
          type="text"
          class="plugin-control"
          maxlength="32"
          autocomplete="off"
          placeholder="微信分享摘要"
        />
        <p v-if="errors.description" class="plugin-error">{{ errors.description }}</p>
      </div>

      <div class="plugin-field">
        <label class="plugin-label" for="wechat-share-img">封面图</label>
        <div class="plugin-inline-row">
          <input
            id="wechat-share-img"
            v-model="form.img"
            type="text"
            class="plugin-control plugin-control--grow"
            placeholder="https://… "
          />
          <VButton size="sm" type="secondary" class="plugin-attach-btn" @click="emit('open-attachment')">
            选择附件
          </VButton>
        </div>
        <p v-if="errors.img" class="plugin-error">{{ errors.img }}</p>
      </div>

      <div class="plugin-field">
        <label class="plugin-label" for="wechat-share-redirect">跳转链接（http/https）</label>
        <input
          id="wechat-share-redirect"
          v-model="form.redirectUrl"
          type="text"
          class="plugin-control"
          autocomplete="off"
          placeholder="https://example.com/path"
        />
        <p v-if="errors.redirectUrl" class="plugin-error">{{ errors.redirectUrl }}</p>
      </div>
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
.plugin-modal-form {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  min-width: 0;
}

.plugin-field {
  min-width: 0;
}

.plugin-label {
  display: block;
  margin-bottom: 0.375rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
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

.plugin-inline-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
}

.plugin-attach-btn {
  flex-shrink: 0;
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
