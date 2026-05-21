import { request } from './request'

export interface ShiftSessionVO {
  id: number
  operatorId: number
  openedAt: string
  status: string
}

export interface HandoverPendingItem {
  type: string
  refId: number
  title: string
  detail: string
}

export interface ShiftHandoverPreviewVO {
  shiftSessionId: number
  openedAt: string
  cashTotal: number
  wechatTotal: number
  alipayTotal: number
  pendingCount: number
  blockCloseOnPending: boolean
  pendingItems: HandoverPendingItem[]
}

export interface ShiftHandoverVO {
  handoverId: number
  shiftSessionId: number
  openedAt: string
  closedAt?: string
  cashTotal: number
  wechatTotal: number
  alipayTotal: number
  pendingItems: HandoverPendingItem[]
}

export function openShift() {
  return request.post<{ data: ShiftSessionVO }>('/shifts/open')
}

export function getCurrentShift() {
  return request.get<{ data: ShiftSessionVO | null }>('/shifts/current')
}

export function getHandoverPreview(shiftSessionId: number) {
  return request.get<{ data: ShiftHandoverPreviewVO }>(`/shifts/${shiftSessionId}/handover-preview`)
}

export function closeShift(shiftSessionId: number, forceClose?: boolean) {
  return request.post<{ data: ShiftHandoverVO }>(`/shifts/${shiftSessionId}/close`, {
    forceClose: forceClose === true
  })
}
