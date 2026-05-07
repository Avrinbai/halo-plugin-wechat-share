<script setup lang="ts">
import { VCard, VEmpty } from '@halo-dev/components'
import RiMedalLine from '~icons/ri/medal-line'
import type { TopCardRow } from '@/types/analyticsDashboard'
import { cardKindLabelZh } from '@/utils/cardKindDisplay'
import { thumbSrcForCardRow } from '@/utils/attachmentUrl'

defineProps<{
  rows: TopCardRow[]
}>()

function thumbSrc(url: string | undefined | null) {
  return thumbSrcForCardRow((url || '').trim())
}

function displayTitle(row: TopCardRow) {
  const t = (row.title || '').trim()
  return t || '—'
}

function kindClass(row: TopCardRow) {
  const k = (row.cardKind || 'link').toString().toLowerCase()
  if (['link', 'image', 'audio', 'video', 'file'].includes(k)) return k
  return 'link'
}

function formatLastVisit(ms: number) {
  if (ms == null || ms <= 0 || Number.isNaN(ms)) return '—'
  const d = new Date(ms)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleString('zh-CN', { hour12: false })
}
</script>

<template>
  <div class="ad-rank-card-host card-wrapper">
    <VCard
      class="ad-main-card :uno: flex h-full min-h-0 flex-col"
      :body-class="[':uno: !p-0 flex min-h-0 flex-1 flex-col overflow-hidden']"
    >
    <template #header>
      <div class="ad-card-section-header ad-card-section-header--solo :uno: bg-gray-50">
        <div class="ad-card-section-header__title">
          <RiMedalLine class="ad-card-section-header__icon ad-card-section-header__icon--amber" aria-hidden="true" />
          <span>访问量排行</span>
        </div>
      </div>
    </template>

    <div v-if="!rows.length" class="ad-rank-card__empty">
      <VEmpty title="暂无排名" message="创建卡片并产生访问后将显示" />
    </div>
    <div v-else class="ad-scroll ad-rank-scroll">
      <table class="ad-table ad-table--rank">
        <colgroup>
          <col style="width: 14%" />
          <col style="width: 20%" />
          <col style="width: 12%" />
          <col style="width: 22%" />
          <col style="width: 18%" />
        </colgroup>
        <thead class="ad-thead">
          <tr>
            <th class="ad-th">SID</th>
            <th class="ad-th">卡片</th>
            <th class="ad-th">卡片类型</th>
            <th class="ad-th ad-th--time">最后访问</th>
            <th class="ad-th">访问量</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in rows" :key="c.sid" class="ad-row">
            <td class="ad-td ad-mono">{{ c.sid }}</td>
            <td class="ad-td ad-td--clip">
              <div class="ad-visit-card-cell">
                <div v-if="thumbSrc(c.img)" class="ad-visit-thumb">
                  <img
                    class="ad-visit-thumb__img"
                    :src="thumbSrc(c.img)"
                    :alt="displayTitle(c)"
                    loading="lazy"
                  />
                </div>
                <div v-else class="ad-visit-thumb ad-visit-thumb--placeholder" aria-hidden="true">
                  <span class="ad-visit-thumb__letter">{{ displayTitle(c).slice(0, 1) }}</span>
                </div>
                <div class="ad-visit-card-text">
                  <p class="ad-visit-card-title">{{ displayTitle(c) }}</p>
                </div>
              </div>
            </td>
            <td class="ad-td">
              <span class="ad-kind-badge" :class="`ad-kind-badge--${kindClass(c)}`">
                {{ cardKindLabelZh(c.cardKind) }}
              </span>
            </td>
            <td class="ad-td ad-td--nowrap ad-muted-time">{{ formatLastVisit(c.lastVisitedAtMillis) }}</td>
            <td class="ad-td">{{ c.visitCount }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    </VCard>
  </div>
</template>
