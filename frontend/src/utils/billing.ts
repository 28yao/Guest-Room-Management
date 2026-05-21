/**
 * 与后端 BillingService 一致的房费晚数、退款预览计算。
 */

function parseYmd(dateStr: string): Date {
  return new Date(dateStr + 'T12:00:00')
}

function addDaysYmd(dateStr: string, days: number): string {
  const d = parseYmd(dateStr)
  d.setDate(d.getDate() + days)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/**
 * 计费截止日对应的收费晚数（至少 1 晚）。
 */
export function computeChargedNights(
  arrivalDate: string,
  departureDate: string,
  chargeThroughDate: string
): number {
  const arrival = parseYmd(arrivalDate)
  let capped = parseYmd(chargeThroughDate)
  const lastNight = parseYmd(addDaysYmd(departureDate, -1))
  if (capped.getTime() > lastNight.getTime()) {
    capped = lastNight
  }
  if (capped.getTime() < arrival.getTime()) {
    return 1
  }
  const end = new Date(capped)
  end.setDate(end.getDate() + 1)
  const nights = Math.round((end.getTime() - arrival.getTime()) / 86400000)
  return nights < 1 ? 1 : nights
}

/**
 * 按截止日预览应付与建议退款（已收 − 应付，不小于 0）。
 */
export function computeRefundPreview(
  paid: number,
  dailyRate: number,
  arrivalDate: string,
  departureDate: string,
  chargeThroughDate: string
): { nights: number; chargeable: number; refund: number } {
  const nights = computeChargedNights(arrivalDate, departureDate, chargeThroughDate)
  const rate = dailyRate > 0 ? dailyRate : 0
  const chargeable = Math.round(rate * nights * 100) / 100
  const refund = Math.max(0, Math.round((paid - chargeable) * 100) / 100)
  return { nights, chargeable, refund }
}
