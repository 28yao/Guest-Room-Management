import { request } from './request'

export interface OccupancyStatsVO {
  totalRooms: number
  sellableRooms: number
  inHouseRooms: number
  occupancyRate: number
}

export interface RevenueDailyItemVO {
  date: string
  amount: number
}

export interface RevenueStatsVO {
  fromDate: string
  toDate: string
  totalRevenue: number
  cashTotal: number
  wechatTotal: number
  alipayTotal: number
  dailyItems: RevenueDailyItemVO[]
}

export function getOccupancyStats() {
  return request.get<{ data: OccupancyStatsVO }>('/stats/occupancy')
}

export function getRevenueStats(from: string, to: string) {
  return request.get<{ data: RevenueStatsVO }>('/stats/revenue', { params: { from, to } })
}
