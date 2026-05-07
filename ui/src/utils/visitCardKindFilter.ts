
export function canonicalVisitCardKindFilter(raw: string | undefined | null): string | undefined {
  const s = (raw ?? '').trim()
  if (!s) return undefined
  const lower = s.toLowerCase()
  if (['link', 'image', 'audio', 'video', 'file'].includes(lower)) return lower
  const zh: Record<string, string> = {
    链接: 'link',
    图片: 'image',
    音频: 'audio',
    视频: 'video',
    文件: 'file',
  }
  const mapped = zh[s]
  return mapped
}
