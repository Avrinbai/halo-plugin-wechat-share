<script setup lang="ts">
import { VButton, VDescription, VDescriptionItem, VModal } from '@halo-dev/components'
import RiMapPinLine from '~icons/ri/map-pin-line'
import { visitEnvCategory, type VisitRow } from '@/types/analyticsDashboard'
import { cardKindLabelZh } from '@/utils/cardKindDisplay'
import { thumbSrcForCardRow } from '@/utils/attachmentUrl'

const props = withDefaults(
  defineProps<{
    visible: boolean
    visit: VisitRow | null
    formatTs: (iso: string) => string
    ipLookupEnabled?: boolean
    ipDisplay?: (row: VisitRow) => string
    ipLoading?: (row: VisitRow) => boolean
  }>(),
  {
    ipLookupEnabled: false,
  },
)

const emit = defineEmits<{ 'update:visible': [boolean]; 'ip-lookup': [row: VisitRow] }>()

function onClose() {
  emit('update:visible', false)
}

function thumbSrc(url: string | undefined | null) {
  return thumbSrcForCardRow((url || '').trim())
}

function displayTitle(v: VisitRow) {
  const t = (v.cardTitle || '').trim()
  return t || '—'
}

function showIp(v: VisitRow) {
  if (props.ipLookupEnabled && props.ipDisplay) {
    return props.ipDisplay(v)
  }
  return (v.clientIp || '').trim() || '—'
}

function canIpLookup(v: VisitRow | null) {
  return Boolean(props.ipLookupEnabled && v && (v.clientIp || '').trim())
}

function loadingIp(v: VisitRow | null) {
  if (!v) return false
  return Boolean(props.ipLoading?.(v))
}

function onIpLookup() {
  if (!props.visit) return
  emit('ip-lookup', props.visit)
}
</script>

<template>
  <VModal :visible="visible" title="访问详情" :width="600" @close="onClose">
    <div v-if="visit" class="ad-visit-detail">
      <div class="ad-visit-detail-head">
        <div v-if="thumbSrc(visit.cardImg)" class="ad-visit-detail-thumb">
          <img
            class="ad-visit-detail-thumb__img"
            :src="thumbSrc(visit.cardImg)"
            :alt="displayTitle(visit)"
            loading="lazy"
          />
        </div>
        <div v-else class="ad-visit-detail-thumb ad-visit-detail-thumb--placeholder" aria-hidden="true">
          <span class="ad-visit-detail-thumb__letter">{{ displayTitle(visit).slice(0, 1) }}</span>
        </div>
        <div class="ad-visit-detail-head-text">
          <p class="ad-visit-detail-title">{{ displayTitle(visit) }}</p>
          <p class="ad-visit-detail-sub">SID · {{ visit.sid }}</p>
        </div>
      </div>

      <VDescription>
        <VDescriptionItem label="卡片类型" :content="cardKindLabelZh(visit.cardKind)" />
        <VDescriptionItem label="访问动作" :content="visit.hitLabel || visit.hitType" />
        <VDescriptionItem label="时间" :content="formatTs(visit.visitedAtIso)" />
        <VDescriptionItem label="SID">
          <span class=":uno: font-mono text-xs">{{ visit.sid }}</span>
        </VDescriptionItem>
        <VDescriptionItem label="访问环境" :content="visitEnvCategory(visit.envKind)" />
        <VDescriptionItem label="环境细分" :content="visit.envLabel || visit.envKind || '—'" />
        <VDescriptionItem label="IP">
          <div class="ad-visit-detail-ip-row">
            <span class=":uno: font-mono text-xs break-all">{{ showIp(visit) }}</span>
            <VButton
              v-if="canIpLookup(visit)"
              size="sm"
              type="secondary"
              class="ad-visit-detail-ip-btn"
              title="查询 IP 归属（实验）"
              aria-label="查询 IP 归属"
              :loading="loadingIp(visit)"
              :disabled="loadingIp(visit)"
              @click="onIpLookup"
            >
              <template #icon>
                <RiMapPinLine class="ad-visit-detail-icon" />
              </template>
            </VButton>
          </div>
        </VDescriptionItem>
        <VDescriptionItem label="User-Agent" :vertical-center="false">
          <div
            class=":uno: max-h-48 overflow-auto whitespace-pre-wrap break-all font-mono text-xs text-gray-700"
          >
            {{ visit.userAgent?.trim() || '—' }}
          </div>
        </VDescriptionItem>
      </VDescription>
    </div>
    <p v-else class=":uno: text-sm text-gray-500">暂无数据</p>
  </VModal>
</template>
