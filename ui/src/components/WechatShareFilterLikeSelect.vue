<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import RiArrowDownSLine from '~icons/ri/arrow-down-s-line'
import RiCheckLine from '~icons/ri/check-line'

export type WechatShareFilterLikeItem = {
  label: string
  value?: string | number | boolean
}

const props = withDefaults(
  defineProps<{
    modelValue: string | number | boolean | undefined
    items: WechatShareFilterLikeItem[]
    disabled?: boolean
    triggerClass?: string
    ariaLabel?: string
    fieldId?: string
  }>(),
  {
    disabled: false,
    triggerClass: '',
    ariaLabel: '请选择',
    fieldId: '',
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', v: string | number | boolean | undefined): void
}>()

/** 勿命名为 open，模板中会与 window.open 冲突 */
const menuOpen = ref(false)
const triggerRef = ref<HTMLElement | null>(null)
const panelRef = ref<HTMLElement | null>(null)
const panelStyle = ref<Record<string, string>>({})

function sameValue(a: unknown, b: unknown) {
  return a === b
}

const selectedItem = computed(() => {
  return props.items.find((it) => sameValue(it.value, props.modelValue)) ?? props.items[0]
})

const displayLabel = computed(() => selectedItem.value?.label ?? '')

function syncPanelPosition() {
  const el = triggerRef.value
  if (!el) return
  const r = el.getBoundingClientRect()
  const gap = 4
  panelStyle.value = {
    position: 'fixed',
    top: `${Math.round(r.bottom + gap)}px`,
    left: `${Math.round(r.left)}px`,
    minWidth: `${Math.round(r.width)}px`,
    zIndex: '10050',
  }
}

function toggle() {
  if (props.disabled) return
  menuOpen.value = !menuOpen.value
}

function selectItem(it: WechatShareFilterLikeItem) {
  if (props.disabled) return
  emit('update:modelValue', it.value)
  menuOpen.value = false
}

function onDocMouseDown(ev: MouseEvent) {
  if (!menuOpen.value) return
  const t = ev.target as Node | null
  if (!t) return
  if (triggerRef.value?.contains(t)) return
  if (panelRef.value?.contains(t)) return
  menuOpen.value = false
}

function onKeyDown(ev: KeyboardEvent) {
  if (!menuOpen.value) return
  if (ev.key === 'Escape') {
    ev.preventDefault()
    menuOpen.value = false
  }
}

function onResize() {
  if (menuOpen.value) syncPanelPosition()
}

watch(menuOpen, async (v) => {
  if (v) {
    await nextTick()
    syncPanelPosition()
    document.addEventListener('mousedown', onDocMouseDown, true)
    window.addEventListener('keydown', onKeyDown, true)
    window.addEventListener('resize', onResize)
  } else {
    document.removeEventListener('mousedown', onDocMouseDown, true)
    window.removeEventListener('keydown', onKeyDown, true)
    window.removeEventListener('resize', onResize)
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', onDocMouseDown, true)
  window.removeEventListener('keydown', onKeyDown, true)
  window.removeEventListener('resize', onResize)
})
</script>

<template>
  <div class="ws-filter-like" :class="{ 'ws-filter-like--disabled': disabled }">
    <button
      ref="triggerRef"
      type="button"
      class="ws-filter-like__trigger"
      :class="triggerClass || undefined"
      :disabled="disabled"
      :id="fieldId || undefined"
      :aria-label="props.ariaLabel"
      :aria-expanded="menuOpen"
      aria-haspopup="listbox"
      @click.stop="toggle"
    >
      <span class="ws-filter-like__value">{{ displayLabel }}</span>
      <RiArrowDownSLine
        class="ws-filter-like__chevron"
        :class="{ 'ws-filter-like__chevron--open': menuOpen }"
        aria-hidden="true"
      />
    </button>

    <Teleport to="body">
      <Transition name="ws-filter-like-fade">
        <div
          v-if="menuOpen"
          ref="panelRef"
          class="ws-filter-like-panel"
          :style="panelStyle"
          role="listbox"
          :aria-label="props.ariaLabel"
          @mousedown.stop
        >
          <button
            v-for="(it, idx) in items"
            :key="idx + ':' + String(it.value)"
            type="button"
            role="option"
            class="ws-filter-like-panel__item"
            :class="{ 'ws-filter-like-panel__item--active': sameValue(it.value, modelValue) }"
            :aria-selected="sameValue(it.value, modelValue) ? 'true' : 'false'"
            @click="selectItem(it)"
          >
            <span class="ws-filter-like-panel__label">{{ it.label }}</span>
            <RiCheckLine v-if="sameValue(it.value, modelValue)" class="ws-filter-like-panel__check" aria-hidden="true" />
          </button>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.ws-filter-like {
  display: block;
  width: 100%;
  max-width: 100%;
}

.ws-filter-like--disabled {
  opacity: 0.65;
  pointer-events: none;
}

.ws-filter-like__trigger {
  box-sizing: border-box;
  position: relative;
  display: flex;
  width: 100%;
  max-width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  height: 2.25rem;
  min-width: 0;
  padding: 0 2.25rem 0 0.75rem;
  margin: 0;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  background-color: #fff;
  font-size: 0.875rem;
  line-height: 1.25;
  color: #111827;
  cursor: pointer;
  outline: none;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.ws-filter-like__trigger:hover:not(:disabled) {
  border-color: #9ca3af;
}

.ws-filter-like__trigger:focus-visible {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgb(59 130 246 / 0.15);
}

.ws-filter-like__trigger:disabled {
  color: #6b7280;
  background-color: #f9fafb;
  cursor: default;
}

.ws-filter-like__value {
  flex: 1;
  min-width: 0;
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ws-filter-like__chevron {
  position: absolute;
  right: 10px;
  top: 50%;
  width: 18px;
  height: 18px;
  color: #64748b;
  transform: translateY(-50%);
  transition: transform 0.15s ease;
  pointer-events: none;
}

.ws-filter-like__chevron--open {
  transform: translateY(-50%) rotate(180deg);
}

.ws-filter-like-panel {
  box-sizing: border-box;
  padding: 0.25rem 0;
  margin: 0;
  list-style: none;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 0.375rem;
  box-shadow:
    0 10px 15px -3px rgb(15 23 42 / 0.08),
    0 4px 6px -4px rgb(15 23 42 / 0.06);
  max-height: min(16rem, 70vh);
  overflow: auto;
}

.ws-filter-like-panel__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  width: 100%;
  padding: 0.5rem 0.75rem;
  margin: 0;
  border: 0;
  background: transparent;
  font-size: 0.875rem;
  line-height: 1.35;
  color: #374151;
  text-align: left;
  cursor: pointer;
  outline: none;
}

.ws-filter-like-panel__item:hover {
  background: #f9fafb;
}

.ws-filter-like-panel__item:focus-visible {
  background: #f3f4f6;
}

.ws-filter-like-panel__item--active {
  background: #f3f4f6;
  color: #111827;
  font-weight: 500;
}

.ws-filter-like-panel__label {
  flex: 1;
  min-width: 0;
}

.ws-filter-like-panel__check {
  flex-shrink: 0;
  width: 1rem;
  height: 1rem;
  color: #2563eb;
}

.ws-filter-like-fade-enter-active,
.ws-filter-like-fade-leave-active {
  transition: opacity 0.12s ease;
}

.ws-filter-like-fade-enter-from,
.ws-filter-like-fade-leave-to {
  opacity: 0;
}
</style>
