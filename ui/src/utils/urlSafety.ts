const DANGEROUS_PROTOCOL = /^\s*(javascript:|data:|vbscript:)/i

export function hasDangerousProtocol(raw: string): boolean {
  return DANGEROUS_PROTOCOL.test(raw.trim())
}

/** 跳转链接：必须为完整 http(s)，禁止 javascript/data 等协议 */
export function validateRedirectUrl(raw: string): string | null {
  const s = raw.trim()
  if (!s) return '跳转链接不能为空'
  if (hasDangerousProtocol(s)) return '跳转链接包含不安全的协议'
  if (!/^https?:\/\//i.test(s)) return '跳转链接需为完整的 http(s) 地址'
  try {
    const u = new URL(s)
    if (u.protocol !== 'http:' && u.protocol !== 'https:') return '跳转链接仅支持 http 或 https'
    return null
  } catch {
    return '跳转链接格式不正确'
  }
}

/**
 * 封面图：允许完整 http(s)、协议相对 //、或以 / 开头的站内路径（提交前再由站点地址拼成绝对 URL）。
 */
export function validateCoverUrl(raw: string): string | null {
  const s = raw.trim()
  if (!s) return '封面图不能为空'
  if (hasDangerousProtocol(s)) return '封面图地址包含不安全的协议'
  if (s.startsWith('//')) return null
  if (/^https?:\/\//i.test(s)) return null
  if (s.startsWith('/')) return null
  return '封面图需为 http(s) 链接、以 // 开头的协议相对地址、或以 / 开头的站内路径'
}

/** 将站内相对路径转为绝对 URL；已是绝对地址则原样返回 */
/** 提交后端前：封面解析后的最终地址必须是绝对 http(s)。 */
export function validateAbsoluteHttpUrl(raw: string): string | null {
  const s = raw.trim()
  if (!s) return '地址不能为空'
  if (hasDangerousProtocol(s)) return '地址包含不安全的协议'
  if (!/^https?:\/\//i.test(s)) return '地址需为完整的 http(s) 链接'
  try {
    const u = new URL(s)
    if (u.protocol !== 'http:' && u.protocol !== 'https:') return '仅支持 http 或 https'
    return null
  } catch {
    return '地址格式不正确'
  }
}

export function resolveAbsoluteAssetUrl(raw: string, publicSiteUrl: string): { ok: true; url: string } | { ok: false } {
  const t = raw.trim()
  if (/^https?:\/\//i.test(t)) return { ok: true, url: t }
  if (t.startsWith('//')) return { ok: true, url: `https:${t}` }
  if (t.startsWith('/')) {
    const base = publicSiteUrl.trim().replace(/\/$/, '')
    if (!base) return { ok: false }
    return { ok: true, url: `${base}${t}` }
  }
  return { ok: true, url: t }
}
