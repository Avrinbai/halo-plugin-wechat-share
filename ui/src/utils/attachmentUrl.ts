export type AttachmentLike = {
  status?: Record<string, unknown>
  spec?: Record<string, unknown>
  permalink?: string
  url?: string
  path?: string
}

function asString(v: unknown) {
  return typeof v === 'string' ? v : ''
}

export function extractAttachmentUrl(attachments: AttachmentLike[]): string {
  const first = attachments?.[0]
  if (!first) return ''
  const status = first.status ?? {}
  const spec = first.spec ?? {}
  return (
    asString(status.permalink) ||
    asString(status.url) ||
    asString(status.link) ||
    asString(status.publicUrl) ||
    asString(status.downloadUrl) ||
    asString(spec.permalink) ||
    asString(spec.url) ||
    asString(spec.link) ||
    asString(spec.path) ||
    asString(first.permalink) ||
    asString(first.url) ||
    asString(first.path) ||
    ''
  ).trim()
}

const HALO_THUMB_M = 800


export function haloListThumbnailUrl(raw: string): string {
  const e = raw.trim()
  if (!e || e.startsWith('data:')) return e
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  if (origin && (e.startsWith(origin) || e.startsWith('/'))) {
    const sep = e.includes('?') ? '&' : '?'
    return `${e}${sep}width=${HALO_THUMB_M}`
  }
  if (!origin && e.startsWith('/')) {
    const sep = e.includes('?') ? '&' : '?'
    return `${e}${sep}width=${HALO_THUMB_M}`
  }
  return `/apis/api.storage.halo.run/v1alpha1/thumbnails/-/via-uri?uri=${encodeURIComponent(e)}&width=${HALO_THUMB_M}`
}


export function urlFingerprint(s: string): string {
  let h = 5381
  for (let i = 0; i < s.length; i++) {
    h = (h * 33) ^ s.charCodeAt(i)
  }
  return (h >>> 0).toString(36)
}


export function appendDisplayCacheBust(displayUrl: string, fingerprintSource?: string): string {
  const u = displayUrl.trim()
  if (!u) return ''
  const tag = urlFingerprint(fingerprintSource ?? u)
  try {
    const base = typeof window !== 'undefined' ? window.location.href : 'http://localhost/'
    const parsed = new URL(u, base)
    parsed.searchParams.set('_wsh', tag)
    if (u.startsWith('/')) {
      return `${parsed.pathname}${parsed.search}${parsed.hash}`
    }
    return parsed.toString()
  } catch {
    const sep = u.includes('?') ? '&' : '?'
    return `${u}${sep}_wsh=${encodeURIComponent(tag)}`
  }
}

export function thumbSrcForCardRow(raw: string): string {
  const t = raw.trim()
  if (!t) return ''
  return appendDisplayCacheBust(haloListThumbnailUrl(t), t)
}
