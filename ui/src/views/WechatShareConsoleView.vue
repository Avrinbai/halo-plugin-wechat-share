<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { VButton, VCard, VModal, VPageHeader, VTabbar } from '@halo-dev/components'
import RiShareForwardLine from '~icons/ri/share-forward-line'
import RiRefreshLine from '~icons/ri/refresh-line'
import RiSettings3Line from '~icons/ri/settings-3-line'
import WechatShareCardsView from '@/views/WechatShareCardsView.vue'
import WechatShareAnalyticsView from '@/views/WechatShareAnalyticsView.vue'
import WechatShareSettingsPanel from '@/components/WechatShareSettingsPanel.vue'

const router = useRouter()
const route = useRoute()

const settingsModalOpen = ref(false)
const settingsPanelRef = ref<InstanceType<typeof WechatShareSettingsPanel> | null>(null)
const analyticsRef = ref<{ refreshAll: () => Promise<void> } | null>(null)
const dashboardRefreshing = ref(false)

const activeId = computed({
  get() {
    const t = route.query.tab
    const s = typeof t === 'string' ? t : Array.isArray(t) ? t[0] : ''
    return s === 'dashboard' ? 'dashboard' : 'cards'
  },
  set(v: string) {
    const next = v === 'dashboard' ? 'dashboard' : 'cards'
    router.replace({ path: route.path, query: { ...route.query, tab: next } })
  },
})

const tabs = [
  { id: 'cards', label: '卡片管理' },
  { id: 'dashboard', label: '数据看板' },
]

function openSettingsModal() {
  settingsModalOpen.value = true
  nextTick(() => {
    settingsPanelRef.value?.load?.()
  })
}

async function refreshDashboard() {
  await nextTick()
  const run = analyticsRef.value?.refreshAll
  if (!run) return
  dashboardRefreshing.value = true
  try {
    await run()
  } finally {
    dashboardRefreshing.value = false
  }
}
</script>

<template>
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

  <VPageHeader title="自定义社交分享卡片">
    <template #icon>
      <RiShareForwardLine />
    </template>
    <template #actions>
      <div class=":uno: flex flex-wrap items-center gap-2">
        <VButton
          v-if="activeId === 'dashboard'"
          type="secondary"
          :loading="dashboardRefreshing"
          @click="refreshDashboard"
        >
          <template #icon>
            <RiRefreshLine class=":uno: size-full" />
          </template>
          刷新
        </VButton>
        <VButton type="secondary" @click="openSettingsModal">
          <template #icon>
            <RiSettings3Line class=":uno: size-full" />
          </template>
          插件配置
        </VButton>
      </div>
    </template>
  </VPageHeader>

  <div class=":uno: m-0 md:m-4">
    <VCard :body-class="[':uno: !p-0']">
      <template #header>
        <VTabbar
          v-model:active-id="activeId"
          :items="tabs"
          class=":uno: w-full !rounded-none"
          type="outline"
        />
      </template>
      <div class=":uno: bg-white">
        <WechatShareCardsView v-if="activeId === 'cards'" />
        <WechatShareAnalyticsView v-else ref="analyticsRef" />
      </div>
    </VCard>
  </div>
</template>

<style scoped>
.settings-modal-body {
  padding: 0;
}
</style>
