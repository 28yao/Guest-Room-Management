/** 默认入住/离店时刻（与后端 ReservationTimePolicy 一致） */
export const DEFAULT_ARRIVAL_TIME = '18:00:00'
export const DEFAULT_DEPARTURE_TIME = '12:00:00'

/**
 * 合并日期与时间为 ISO-8601 本地时刻（Spring DATE_TIME 可解析）。
 */
export function combineDateTime(date: string, time: string = DEFAULT_ARRIVAL_TIME): string {
  const t = time && time.length >= 5 ? time.substring(0, 8) : DEFAULT_ARRIVAL_TIME
  const normalized = t.length === 5 ? `${t}:00` : t
  return `${date}T${normalized}`
}

/**
 * 从 API 返回的 arrivalAt/departureAt 或日期生成 ISO 时刻。
 */
export function toIsoDateTime(date: string, isoOrTime?: string, defaultTime = DEFAULT_ARRIVAL_TIME): string {
  if (isoOrTime && isoOrTime.includes('T')) {
    return isoOrTime.length >= 19 ? isoOrTime.substring(0, 19) : isoOrTime
  }
  if (isoOrTime && isoOrTime.includes(' ')) {
    return isoOrTime.replace(' ', 'T')
  }
  const time = isoOrTime && isoOrTime.length >= 5 ? isoOrTime : defaultTime
  return combineDateTime(date, time)
}
