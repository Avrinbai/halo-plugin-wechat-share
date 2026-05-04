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

/**
 * 从 Halo 附件选择器的结果中解析可用于分享的 URL（与 halo-plugin-dishes 逻辑保持一致）。
 */
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
