<script setup lang="ts">
import { computed, onMounted, ref, unref, watch } from 'vue'
import { Dialog, VCard, VEmpty, VLoading, VPagination, VSpace } from '@halo-dev/components'
import { getData, getApiErrorMessage } from '@/api/client'
import '@/styles/analytics-dashboard.css'

import type { SummaryPayload, VisitPagePayload, VisitRow } from '@/types/analyticsDashboard'
import AnalyticsSummaryMetrics from '@/components/analytics/AnalyticsSummaryMetrics.vue'
import AnalyticsTrendSection from '@/components/analytics/AnalyticsTrendSection.vue'
import AnalyticsTopRank from '@/components/analytics/AnalyticsTopRank.vue'
import AnalyticsVisitToolbar from '@/components/analytics/AnalyticsVisitToolbar.vue'
import AnalyticsVisitTable from '@/components/analytics/AnalyticsVisitTable.vue'
import AnalyticsVisitDetailModal from '@/components/analytics/AnalyticsVisitDetailModal.vue'
import { canonicalVisitCardKindFilter } from '@/utils/visitCardKindFilter'
import { useVisitIpLookup } from '@/composables/useVisitIpLookup'

const summaryLoading = ref(true)
const summary = ref<SummaryPayload | null>(null)

const visitsLoading = ref(true)
const visitPage = ref(1)
const visitSize = ref(20)
const visitTotal = ref(0)
const visits = ref<VisitRow[]>([])
const sidFilter = ref('')
const visitCardKindFilter = ref<string | undefined>(undefined)
const visitTimePreset = ref<string | undefined>(undefined)

const visitDetailVisible = ref(false)
const visitDetailRow = ref<VisitRow | null>(null)

const ipLookup = useVisitIpLookup()

const trendMax = computed(() => {
  const pts = summary.value?.trendLastDays || []
  const m = pts.reduce((acc, x) => Math.max(acc, x.count), 0)
  return Math.max(1, m)
})

async function loadSummary() {
  summaryLoading.value = true
  try {
    summary.value = await getData<SummaryPayload>('/analytics/summary', {
      params: { _t: Date.now() },
    })
  } catch (e) {
    Dialog.error({
      title: '加载失败',
      description: getApiErrorMessage(e, '无法加载数据概览'),
    })
  } finally {
    summaryLoading.value = false
  }
}

function visitTimeMillisParams(): { visitedAfter?: number; visitedBefore?: number } {
  const p = visitTimePreset.value
  if (!p) return {}
  const now = Date.now()
  if (p === '7d') {
    return { visitedAfter: now - 7 * 24 * 60 * 60 * 1000 }
  }
  if (p === '30d') {
    return { visitedAfter: now - 30 * 24 * 60 * 60 * 1000 }
  }
  if (p === 'today') {
    const d = new Date()
    d.setHours(0, 0, 0, 0)
    const start = d.getTime()
    const end = start + 24 * 60 * 60 * 1000
    return { visitedAfter: start, visitedBefore: end }
  }
  return {}
}

async function loadVisits() {
  visitsLoading.value = true
  try {
    const params: Record<string, string | number> = {
      page: visitPage.value - 1,
      size: visitSize.value,
      _t: Date.now(),
    }
    const sf = sidFilter.value.trim()
    if (sf) params.sid = sf

    const ck = canonicalVisitCardKindFilter(visitCardKindFilter.value)
    if (ck) params.cardKind = ck

    const tr = visitTimeMillisParams()
    if (typeof tr.visitedAfter === 'number' && tr.visitedAfter > 0) {
      params.visitedAfter = tr.visitedAfter
    }
    if (typeof tr.visitedBefore === 'number' && tr.visitedBefore > 0) {
      params.visitedBefore = tr.visitedBefore
    }

    const pageData = await getData<VisitPagePayload>('/analytics/visits', { params })
    visits.value = pageData.items || []
    visitTotal.value = Number(pageData.total || 0)
  } catch (e) {
    Dialog.error({
      title: '加载失败',
      description: getApiErrorMessage(e, '无法加载访问明细'),
    })
  } finally {
    visitsLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadSummary(), loadVisits()])
}

defineExpose({
  refreshAll,
})

function formatTs(iso: string) {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString('zh-CN', { hour12: false })
}

watch([visitPage, visitSize], () => {
  void loadVisits()
})

watch([visitCardKindFilter, visitTimePreset], () => {
  visitPage.value = 1
  void loadVisits()
})

function applyVisitFilters() {
  visitPage.value = 1
  void loadVisits()
}

function clearVisitFilters() {
  sidFilter.value = ''
  visitCardKindFilter.value = undefined
  visitTimePreset.value = undefined
  visitPage.value = 1
  void loadVisits()
}

function openVisitDetail(row: VisitRow) {
  const latest = visits.value.find((v) => v.metadataName === row.metadataName)
  visitDetailRow.value = latest ?? row
  visitDetailVisible.value = true
}

async function onIpLookup(row: VisitRow) {
  const text = await ipLookup.lookupRow(row)
  if (!text) return
  const name = row.metadataName
  visits.value = visits.value.map((v) =>
    v.metadataName === name ? { ...v, ipLocationText: text } : v,
  )
  if (visitDetailRow.value?.metadataName === name) {
    visitDetailRow.value = { ...visitDetailRow.value, ipLocationText: text }
  }
}

onMounted(() => {
  ipLookup.init()
  void refreshAll()
})
</script>
 
<template>
  <div class="analytics-page w-full">
    <VSpace class="ad-analytics-stack w-full" direction="column" spacing="lg">
      <VLoading v-if="summaryLoading && !summary" />

      <VSpace
        v-else-if="summary"
        class="ad-analytics-summary-stack :uno: w-full min-w-0"
        direction="column"
        spacing="md"
      >
        <AnalyticsSummaryMetrics :summary="summary" />

        <section class="ad-trend-rank-row w-full">
          <AnalyticsTrendSection :points="summary.trendLastDays || []" :max="trendMax" />
          <AnalyticsTopRank :rows="summary.topCards" />
        </section>
      </VSpace>

      <VCard class="ad-main-card ad-visits-card :uno: overflow-hidden w-full" :body-class="[':uno: !p-0']">
        <template #header>
          <AnalyticsVisitToolbar
            v-model:sid="sidFilter"
            v-model:card-kind="visitCardKindFilter"
            v-model:time-preset="visitTimePreset"
            @filter="applyVisitFilters"
            @clear="clearVisitFilters"
          />
        </template>

        <div class="ad-visits-body-pad">
          <VLoading v-if="visitsLoading && !visits.length" />
          <VEmpty
            v-else-if="!visits.length"
            class=":uno: py-10"
            title="暂无访问记录"
            message="公开页产生访问后会出现在这里；也可尝试调整 SID、类型或时间筛选条件。"
          />
          <AnalyticsVisitTable
            v-else
            :visits="visits"
            :format-ts="formatTs"
            :ip-lookup-enabled="unref(ipLookup.enabled)"
            :ip-display="ipLookup.ipDisplay"
            :ip-loading="ipLookup.isLoading"
            @detail="openVisitDetail"
            @ip-lookup="onIpLookup"
          />
        </div>

        <template #footer>
          <div v-if="visitTotal > 0" class=":uno: px-3 ">
            <VPagination
              v-model:page="visitPage"
              v-model:size="visitSize"
              :total="visitTotal"
              :size-options="[10, 20, 50, 100]"
            />
          </div>
        </template>
      </VCard>

      <AnalyticsVisitDetailModal
        v-model:visible="visitDetailVisible"
        :visit="visitDetailRow"
        :format-ts="formatTs"
        :ip-lookup-enabled="unref(ipLookup.enabled)"
        :ip-display="ipLookup.ipDisplay"
        :ip-loading="ipLookup.isLoading"
        @ip-lookup="onIpLookup"
      />
    </VSpace>
  </div>
</template>
