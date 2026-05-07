import { ref } from 'vue'
import { Dialog } from '@halo-dev/components'
import { getApiErrorMessage, getData } from '@/api/client'
import type { VisitRow } from '@/types/analyticsDashboard'

type SettingsShape = {
  spec?: {
    experimentalIpLookupEnabled?: boolean
    ipLookupApiBase?: string
  }
}

export function useVisitIpLookup() {
  const enabled = ref(false)
  /** 正在查询的访问记录 metadata.name */
  const loadingVisitName = ref<string | null>(null)

  async function refreshSettings() {
    try {
      const s = await getData<SettingsShape>('/settings')
      enabled.value = Boolean(s.spec?.experimentalIpLookupEnabled)
    } catch {
      enabled.value = false
    }
  }

  function init() {
    void refreshSettings()
  }

  async function lookupRow(row: VisitRow): Promise<string | null> {
    const ip = (row.clientIp || '').trim()
    if (!ip || !enabled.value) return null
    const cached = (row.ipLocationText || '').trim()
    if (cached) return cached

    loadingVisitName.value = row.metadataName
    try {
      const res = await getData<{ locationText: string }>('/analytics/ip-location', {
        params: { ip, visit: row.metadataName },
      })
      const t = (res.locationText || '').trim()
      return t || null
    } catch (e) {
      Dialog.error({
        title: 'IP 归属查询失败',
        description: getApiErrorMessage(e, '请求失败'),
      })
      return null
    } finally {
      loadingVisitName.value = null
    }
  }

  function ipDisplay(row: VisitRow) {
    const ip = (row.clientIp || '').trim()
    if (!ip) return '—'
    const loc = (row.ipLocationText || '').trim()
    return loc ? `${ip}/${loc}` : ip
  }

  function isLoading(row: VisitRow) {
    return loadingVisitName.value === row.metadataName
  }

  return {
    enabled,
    loadingVisitName,
    init,
    refreshSettings,
    lookupRow,
    ipDisplay,
    isLoading,
  }
}
