import { request } from './request'

export interface ShiftSessionVO {
  id: number
  operatorId: number
  openedAt: string
  status: string
}

export function openShift() {
  return request.post<{ data: ShiftSessionVO }>('/shifts/open')
}

export function getCurrentShift() {
  return request.get<{ data: ShiftSessionVO | null }>('/shifts/current')
}
