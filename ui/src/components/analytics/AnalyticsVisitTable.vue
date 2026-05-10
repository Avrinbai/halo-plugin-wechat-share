<script setup lang="ts">
import { VButton } from '@halo-dev/components'
import RiEyeLine from '~icons/ri/eye-line'
import type { VisitRow } from '@/types/analyticsDashboard'
import { visitEnvCategory } from '@/types/analyticsDashboard'
import { cardKindLabelZh } from '@/utils/cardKindDisplay'
import { thumbSrcForCardRow } from '@/utils/attachmentUrl'

const props = withDefaults(
  defineProps<{
    visits: VisitRow[]
    formatTs: (iso: string) => string
    ipLookupEnabled?: boolean
    ipDisplay?: (row: VisitRow) => string
    ipLoading?: (row: VisitRow) => boolean
  }>(),
  {
    ipLookupEnabled: false,
  },
)

const emit = defineEmits<{
  detail: [row: VisitRow]
  'ip-lookup': [row: VisitRow]
}>()

function thumbSrc(url: string | undefined | null) {
  return thumbSrcForCardRow((url || '').trim())
}

function displayTitle(row: VisitRow) {
  const t = (row.cardTitle || '').trim()
  return t || '—'
}

function kindClass(row: VisitRow) {
  const k = (row.cardKind || 'link').toString().toLowerCase()
  if (['link', 'image', 'audio', 'video', 'file'].includes(k)) return k
  return 'link'
}

function showIp(row: VisitRow) {
  if (props.ipLookupEnabled && props.ipDisplay) {
    return props.ipDisplay(row)
  }
  return (row.clientIp || '').trim() || '—'
}

function canIpLookup(row: VisitRow) {
  return props.ipLookupEnabled && Boolean((row.clientIp || '').trim())
}

function loadingIp(row: VisitRow) {
  return Boolean(props.ipLoading?.(row))
}
</script>

<template>
  <div class="ad-scroll">
    <table class="ad-table ad-table--visits">
      <colgroup>
        <col style="width: 9%" />
        <col style="width: 14%" />
        <col style="width: 9%" />
        <col style="width: 15%" />
        <col style="width: 18%" />
        <col style="width: 11%" />
        <col style="width: 12%" />
      </colgroup>
      <thead class="ad-thead">
        <tr>
          <th class="ad-th">SID</th>
          <th class="ad-th">卡片</th>
          <th class="ad-th">卡片类型</th>
          <th class="ad-th ad-th--time">访问时间</th>
          <th class="ad-th">IP</th>
          <th class="ad-th">访问环境</th>
          <th class="ad-th ad-th--actions ad-th--actions-visit" />
        </tr>
      </thead>
      <tbody>
        <tr v-for="v in visits" :key="v.metadataName" class="ad-row">
          <td class="ad-td ad-mono">{{ v.sid }}</td>
          <td class="ad-td ad-td--clip">
            <div class="ad-visit-card-cell">
              <div v-if="thumbSrc(v.cardImg)" class="ad-visit-thumb">
                <img
                  class="ad-visit-thumb__img"
                  :src="thumbSrc(v.cardImg)"
                  :alt="displayTitle(v)"
                  loading="lazy"
                />
              </div>
              <div v-else class="ad-visit-thumb ad-visit-thumb--placeholder" aria-hidden="true">
                <span class="ad-visit-thumb__letter">{{ displayTitle(v).slice(0, 1) }}</span>
              </div>
              <div class="ad-visit-card-text">
                <p class="ad-visit-card-title">{{ displayTitle(v) }}</p>
              </div>
            </div>
          </td>
          <td class="ad-td">
            <span class="ad-kind-badge" :class="`ad-kind-badge--${kindClass(v)}`">
              {{ cardKindLabelZh(v.cardKind) }}
            </span>
          </td>
          <td class="ad-td ad-td--nowrap">{{ formatTs(v.visitedAtIso) }}</td>
          <td class="ad-td ad-mono ad-td--ip-loc">{{ showIp(v) }}</td>
          <td class="ad-td ad-td--clip">{{ visitEnvCategory(v.envKind) }}</td>
          <td class="ad-td ad-td--action">
            <div class="ad-visit-actions">
              <VButton
                class="ad-visit-detail-btn"
                size="sm"
                type="secondary"
                title="查看详情"
                aria-label="查看详情"
                @click="emit('detail', v)"
              >
                <template #icon>
                  <RiEyeLine class="ad-visit-detail-icon" />
                </template>
              </VButton>
              <VButton
                v-if="canIpLookup(v)"
                class="ad-visit-detail-btn ad-visit-ip-btn"
                size="sm"
                type="secondary"
                title="查询 IP 归属（实验）"
                aria-label="查询 IP 归属"
                :loading="loadingIp(v)"
                :disabled="loadingIp(v)"
                @click="emit('ip-lookup', v)"
              >
                查询IP归属
              </VButton>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
