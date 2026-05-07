<script setup lang="ts">
import { computed } from 'vue'
import { VSpace } from '@halo-dev/components'

const sid = defineModel<string>('sid', { default: '' })
const cardKind = defineModel<string | undefined>('cardKind')
const timePreset = defineModel<string | undefined>('timePreset')

const emit = defineEmits<{
  filter: []
  clear: []
}>()

const visitCardKindItems: { label: string; value?: string }[] = [
  { label: '全部', value: undefined },
  { label: '链接', value: 'link' },
  { label: '图片', value: 'image' },
  { label: '音频', value: 'audio' },
  { label: '视频', value: 'video' },
  { label: '文件', value: 'file' },
]

const visitTimeItems: { label: string; value?: string }[] = [
  { label: '全部', value: undefined },
  { label: '今天', value: 'today' },
  { label: '近 7 天', value: '7d' },
  { label: '近 30 天', value: '30d' },
]

const hasFilters = computed(() => {
  if (sid.value.trim()) return true
  if (cardKind.value != null && String(cardKind.value).trim() !== '') return true
  if (timePreset.value != null && String(timePreset.value).trim() !== '') return true
  return false
})
</script>

<template>
  <div class="ad-visit-toolbar w-full">
    <p class="ad-visit-toolbar__title">访问明细</p>
    <div class="ad-visit-toolbar__row">
      <div
        class="ad-visit-toolbar__search :uno: flex w-full flex-1 items-center sm:w-auto"
      >
        <SearchInput
          v-model="sid"
          placeholder="按 SID（回车）"
          @keyup.enter.prevent="emit('filter')"
        />
      </div>
      <VSpace spacing="lg" class="ad-visit-toolbar__filters">
        <FilterCleanButton v-if="hasFilters" @click="emit('clear')" />
        <FilterDropdown v-model="cardKind" label="类型" :items="visitCardKindItems" />
        <FilterDropdown v-model="timePreset" label="时间" :items="visitTimeItems" />
      </VSpace>
    </div>
  </div>
</template>
