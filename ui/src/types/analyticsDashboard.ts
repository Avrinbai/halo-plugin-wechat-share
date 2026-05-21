export type EnvBreakdown = {
  wechat: number
  wework: number
  qq: number
  mobileOther: number
  desktop: number
  unknown: number
}

export type TopCardRow = {
  sid: string
  title: string
  visitCount: number
  img: string
  cardKind: string
  lastVisitedAtMillis: number
}

export type SummaryPayload = {
  cardCount: number
  enabledCount: number
  totalVisits: number
  pvSevenDays: number
  uvSevenDays: number
  uniqueIpAllTime: number
  envBreakdown: EnvBreakdown
  trendLastDays: { date: string; count: number }[]
  topCards: TopCardRow[]
}

export type VisitRow = {
  metadataName: string
  sid: string
  hitType: string
  hitLabel: string
  cardTitle?: string
  cardImg?: string
  cardKind?: string
  clientIp: string
  userAgent: string
  envKind: string
  envLabel: string
  visitedAtIso: string
  ipLocationText?: string
}

export type VisitPagePayload = {
  page: number
  size: number
  total: number
  items: VisitRow[]
}

export function visitEnvCategory(envKind: string | undefined | null): string {
  const k = (envKind || '').trim().toUpperCase()
  if (k === 'WECHAT' || k === 'WEWORK') return '微信'
  if (k === 'QQ') return 'QQ'
  if (k === 'MOBILE_OTHER' || k === 'DESKTOP') return '浏览器'
  return '其他'
}
