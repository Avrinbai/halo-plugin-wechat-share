export function cardKindLabelZh(kind: string | undefined | null): string {
  const k = (kind || 'link').toLowerCase()
  if (k === 'image') return '图片'
  if (k === 'audio') return '音频'
  if (k === 'video') return '视频'
  if (k === 'file') return '文件'
  return '链接'
}
