<script setup lang="ts">
import { VCard } from '@halo-dev/components'
import type { SummaryPayload } from '@/types/analyticsDashboard'
import RiPulseLine from '~icons/ri/pulse-line'
import RiLineChartFill from '~icons/ri/line-chart-fill'
import RiUserStarLine from '~icons/ri/user-star-line'
import RiGlobalFill from '~icons/ri/global-fill'

defineProps<{
  summary: SummaryPayload
}>()
</script>

<template>
  <div class="ad-metrics-grid">
    <VCard class="ad-main-card ad-metric-card :uno: overflow-hidden" :body-class="[':uno: !p-0']">
      <div class="ad-metric-card-inner">
        <span class="ad-metric-card__icon-wrap" aria-hidden="true">
          <RiPulseLine class="ad-metric-card__icon" />
        </span>
        <div class="ad-metric-card__text">
          <p class="ad-metric-card__label">总访问量</p>
          <p class="ad-metric-card__value">{{ summary.totalVisits }}</p>
        </div>
      </div>
    </VCard>

    <VCard class="ad-main-card ad-metric-card :uno: overflow-hidden" :body-class="[':uno: !p-0']">
      <div class="ad-metric-card-inner">
        <span class="ad-metric-card__icon-wrap ad-metric-card__icon-wrap--pv" aria-hidden="true">
          <RiLineChartFill class="ad-metric-card__icon" />
        </span>
        <div class="ad-metric-card__text">
          <p class="ad-metric-card__label">PV（近七日）</p>
          <p class="ad-metric-card__value">{{ summary.pvSevenDays ?? 0 }}</p>
        </div>
      </div>
    </VCard>

    <VCard class="ad-main-card ad-metric-card :uno: overflow-hidden" :body-class="[':uno: !p-0']">
      <div class="ad-metric-card-inner">
        <span class="ad-metric-card__icon-wrap" aria-hidden="true">
          <RiUserStarLine class="ad-metric-card__icon" />
        </span>
        <div class="ad-metric-card__text">
          <p class="ad-metric-card__label">UV（近七日）</p>
          <p class="ad-metric-card__value">{{ summary.uvSevenDays ?? 0 }}</p>
        </div>
      </div>
    </VCard>

    <VCard class="ad-main-card ad-metric-card :uno: overflow-hidden" :body-class="[':uno: !p-0']">
      <div class="ad-metric-card-inner">
        <span class="ad-metric-card__icon-wrap ad-metric-card__icon-wrap--ip" aria-hidden="true">
          <RiGlobalFill class="ad-metric-card__icon" />
        </span>
        <div class="ad-metric-card__text">
          <p class="ad-metric-card__label">独立 IP</p>
          <p class="ad-metric-card__value">{{ summary.uniqueIpAllTime ?? 0 }}</p>
        </div>
      </div>
    </VCard>
  </div>
</template>
