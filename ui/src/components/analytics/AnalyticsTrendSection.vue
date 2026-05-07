<script setup lang="ts">
import { computed, useId } from 'vue'
import { VCard, VEmpty } from '@halo-dev/components'
import RiLineChartLine from '~icons/ri/line-chart-line'

const props = defineProps<{
  points: { date: string; count: number }[]
  max: number
}>()

const W = 520
const H = 168
const padL = 8
const padR = 8
const padT = 14
const padB = 14

const gradientId = `ad-trend-fill-${useId().replace(/[^a-zA-Z0-9_-]/g, '')}`

const chartModel = computed(() => {
  const pts = props.points || []
  if (!pts.length) {
    return {
      lineD: '',
      areaD: '',
      dots: [] as { cx: number; cy: number; date: string; count: number }[],
      gridYs: [] as number[],
    }
  }
  const max = Math.max(1, props.max)
  const n = pts.length
  const plotW = W - padL - padR
  const plotH = H - padT - padB
  const xAt = (i: number) => padL + (n <= 1 ? plotW / 2 : (i / (n - 1)) * plotW)
  const yAt = (c: number) => padT + plotH - (c / max) * plotH

  const lineD = pts.map((p, i) => `${i === 0 ? 'M' : 'L'} ${xAt(i)} ${yAt(p.count)}`).join(' ')
  const baseY = padT + plotH
  const areaD =
    `M ${xAt(0)} ${baseY} ` +
    pts.map((p, i) => `L ${xAt(i)} ${yAt(p.count)}`).join(' ') +
    ` L ${xAt(n - 1)} ${baseY} Z`

  const dots = pts.map((p, i) => ({
    cx: xAt(i),
    cy: yAt(p.count),
    date: p.date,
    count: p.count,
  }))

  const gridYs = [0.25, 0.5, 0.75].map((t) => padT + plotH * t)

  return { lineD, areaD, dots, gridYs }
})

function xAxisItemStyle(i: number) {
  const n = props.points?.length ?? 0
  if (n <= 0) return {}
  if (n === 1) return { left: '50%', transform: 'translateX(-50%)' }
  const t = padL / W + (i / (n - 1)) * ((W - padL - padR) / W)
  const left = `${t * 100}%`
  /** 首尾不用 -50%，避免一半伸出容器被挤压换行 */
  if (i === 0) return { left, transform: 'translateX(0)' }
  if (i === n - 1) return { left, transform: 'translateX(-100%)' }
  return { left, transform: 'translateX(-50%)' }
}
</script>

<template>
  <VCard class="ad-main-card ad-trend-card :uno: h-full min-h-0">
    <template #header>
      <div class="ad-card-section-header :uno: bg-gray-50">
        <div class="ad-card-section-header__title">
          <RiLineChartLine class="ad-card-section-header__icon" aria-hidden="true" />
          <span>近七日访问趋势</span>
        </div>
      </div>
    </template>

    <div v-if="!points.length" class="ad-trend-empty">
      <VEmpty title="暂无趋势" message="产生访问后按自然日汇总" />
    </div>
    <div v-else class="ad-trend-body">
      <div class="ad-trend-chart-panel">
        <svg
          class="ad-trend-svg"
          :viewBox="`0 0 ${W} ${H}`"
          preserveAspectRatio="xMidYMid meet"
          role="img"
          aria-label="近七日访问量折线图"
        >
          <defs>
            <linearGradient :id="gradientId" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="rgb(76 204 160)" stop-opacity="0.22" />
              <stop offset="55%" stop-color="rgb(76 204 160)" stop-opacity="0.06" />
              <stop offset="100%" stop-color="rgb(76 204 160)" stop-opacity="0" />
            </linearGradient>
          </defs>

          <line
            v-for="(gy, i) in chartModel.gridYs"
            :key="'g' + i"
            class="ad-trend-grid-line"
            :x1="padL"
            :y1="gy"
            :x2="W - padR"
            :y2="gy"
          />

          <path v-if="chartModel.areaD" class="ad-trend-area" :d="chartModel.areaD" :fill="`url(#${gradientId})`" />
          <path v-if="chartModel.lineD" class="ad-trend-line" :d="chartModel.lineD" />
          <circle
            v-for="(d, i) in chartModel.dots"
            :key="i"
            class="ad-trend-dot-ring"
            :cx="d.cx"
            :cy="d.cy"
            r="5"
          />
          <circle
            v-for="(d, i) in chartModel.dots"
            :key="'c' + i"
            class="ad-trend-dot"
            :cx="d.cx"
            :cy="d.cy"
            r="3.25"
          >
            <title>{{ d.date }} · {{ d.count }}</title>
          </circle>
        </svg>
      </div>

      <ul class="ad-trend-xaxis" aria-hidden="true">
        <li
          v-for="(p, i) in points"
          :key="p.date"
          class="ad-trend-xaxis__item"
          :style="xAxisItemStyle(i)"
        >
          <span class="ad-trend-xaxis__date">{{ p.date.slice(5) }}</span>
          <span class="ad-trend-xaxis__count">{{ p.count }}</span>
        </li>
      </ul>

      <p class="ad-trend-hint">
        按自然日汇总，悬停折线节点可查看当日次数。
      </p>
    </div>
  </VCard>
</template>
