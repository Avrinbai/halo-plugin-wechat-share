<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Dialog, Toast, VButton, VCard } from '@halo-dev/components'
import { getApiErrorMessage, getData, putData } from '@/api/client'

type WechatShareSettingsExt = {
  apiVersion?: string
  kind?: string
  metadata?: Record<string, unknown>
  spec?: {
    wxAppId?: string
    wxAppSecret?: string
    publicBasePath?: string
    qrcodeApiBase?: string
  }
}

const loading = ref(true)
const saving = ref(false)
const model = ref<WechatShareSettingsExt | null>(null)

const form = reactive({
  wxAppId: '',
  wxAppSecret: '',
  publicBasePath: '',
  qrcodeApiBase: '',
})

function applyFromModel(m: WechatShareSettingsExt) {
  const s = m.spec || {}
  form.wxAppId = s.wxAppId ?? ''
  form.wxAppSecret = s.wxAppSecret ?? ''
  form.publicBasePath = s.publicBasePath ?? ''
  form.qrcodeApiBase = s.qrcodeApiBase ?? ''
}

async function load() {
  loading.value = true
  try {
    const data = await getData<WechatShareSettingsExt>('/settings')
    model.value = data
    applyFromModel(data)
  } catch (e) {
    Dialog.error({
      title: '加载失败',
      description: getApiErrorMessage(e, '加载设置失败，请稍后重试'),
    })
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!model.value) return
  saving.value = true
  try {
    const payload: WechatShareSettingsExt = {
      ...model.value,
      spec: {
        ...(model.value.spec || {}),
        wxAppId: form.wxAppId.trim(),
        wxAppSecret: form.wxAppSecret.trim(),
        publicBasePath: form.publicBasePath.trim(),
        qrcodeApiBase: form.qrcodeApiBase.trim(),
      },
    }
    const saved = await putData<WechatShareSettingsExt>('/settings', payload)
    model.value = saved
    applyFromModel(saved)
    Toast.success('设置已保存')
  } catch (e) {
    Dialog.error({
      title: '保存失败',
      description: getApiErrorMessage(e, '保存失败，请检查配置项后重试'),
    })
  } finally {
    saving.value = false
  }
}

defineExpose({ load })
</script>

<template>
  <VCard class="settings-card" :body-class="[':uno: !p-0']">
    <div class="settings-body">
      <div v-if="loading" class="settings-loading">加载中…</div>

      <div v-else class="settings-section">
        <h3 class="settings-title">公众号凭据</h3>
        <p class="settings-desc">
          用于服务端换取 <code class="settings-code">access_token</code> 与 <code class="settings-code">jsapi_ticket</code>，并为分享页生成
          <code class="settings-code">wx.config</code> 签名。
        </p>

        <div class="settings-field">
          <div class="settings-field-label">公众号 AppId</div>
          <input v-model="form.wxAppId" type="text" class="settings-input" autocomplete="off" placeholder="wx…" />
        </div>

        <div class="settings-field">
          <div class="settings-field-label">公众号 AppSecret</div>
          <input
            v-model="form.wxAppSecret"
            type="password"
            class="settings-input"
            autocomplete="new-password"
            placeholder="••••••••"
          />
        </div>

        <div class="settings-divider settings-divider--section" />

        <div class="settings-field">
          <div class="settings-field-label">公开路径前缀</div>
          <input v-model="form.publicBasePath" type="text" class="settings-input" placeholder="/wechat-share" />
          <p class="settings-hint">
            默认 <code class="settings-code">/wechat-share</code>；对外固定为该前缀下的
            <code class="settings-code">/share</code> 与 <code class="settings-code">/go</code>。修改保存后立即生效。
          </p>
        </div>

        <div class="settings-divider" />

        <h4 class="settings-subtitle">控制台工具</h4>

        <div class="settings-field">
          <div class="settings-field-label">二维码上游接口</div>
          <input v-model="form.qrcodeApiBase" type="text" class="settings-input" placeholder="https://…" />
          <p class="settings-hint">用于分享卡片二维码生成。</p>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="settings-footer">
        <VButton :disabled="loading || saving" @click="load">重置</VButton>
        <VButton type="primary" :disabled="loading || saving" :loading="saving" @click="save">保存</VButton>
      </div>
    </template>
  </VCard>
</template>

<style scoped>
.settings-card {
  box-shadow: none;
  border-radius: 0;
  border: none;
}

.settings-card :deep(.card-header),
.settings-card :deep(.card-footer),
.settings-card :deep(.card-body) {
  padding: 0;
}

.settings-card :deep(.card-body) {
  background: #fff;
}

.settings-body {
  padding: 0;
  background: #fff;
}

.settings-loading {
  color: rgb(113 113 122);
  font-size: 0.875rem;
  padding: 12px 16px;
}

.settings-section {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  padding: 12px 16px 16px;
}

.settings-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: rgb(24 24 27);
}

.settings-subtitle {
  margin: 0.25rem 0 0;
  font-size: 0.875rem;
  font-weight: 600;
  color: rgb(39 39 42);
}

.settings-desc {
  margin: 0;
  font-size: 0.875rem;
  color: rgb(113 113 122);
}

.settings-field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.settings-field-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: rgb(63 63 70);
}

.settings-input {
  height: 2.25rem;
  border: 1px solid rgb(212 212 216);
  border-radius: 6px;
  padding: 0 0.75rem;
  font-size: 0.875rem;
}

.settings-hint {
  margin: 0;
  font-size: 0.75rem;
  color: rgb(113 113 122);
}

.settings-divider {
  height: 1px;
  margin: 0.75rem 0;
  background: rgb(228 228 231);
}

.settings-divider--section {
  margin: 1rem 0;
}

.settings-code {
  font-size: 0.8125rem;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
  background: rgb(244 244 245);
  color: rgb(63 63 70);
}

.settings-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 10px 16px;
  border-top: 1px solid #ebeef5;
  background: #fff;
}
</style>
