import { request } from './request'

export interface StayVO {
  id: number
  stayNo: string
  reservationId?: number
  resNo?: string
  roomId: number
  roomNo: string
  roomVersion?: number
  roomTypeId: number
  roomTypeName: string
  guestName: string
  guestPhone: string
  idCard?: string
  arrivalDate: string
  departureDate: string
  agreedDailyRate?: number
  status: string
  remark?: string
  checkInAt?: string
  folioId?: number
  folioTotalAmount?: number
  folioPaidAmount?: number
}

export interface CheckInPaymentItem {
  method: string
  amount: number
}

export interface WalkInForm {
  roomId: number | undefined
  guestName: string
  guestPhone: string
  idCard?: string
  arrivalDate: string
  departureDate: string
  /** ISO 本地时刻，未传时后端默认 18:00 / 12:00 */
  arrivalAt?: string
  departureAt?: string
  agreedDailyRate?: number
  remark?: string
  payments: CheckInPaymentItem[]
}

export interface ReservationCheckInForm {
  reservationId: number | undefined
  roomId?: number
  agreedDailyRate?: number
  remark?: string
  payments: CheckInPaymentItem[]
}

export function walkInCheckIn(data: WalkInForm) {
  return request.post<{ data: StayVO }>('/stays/walk-in', data)
}

export function checkInFromReservation(data: ReservationCheckInForm) {
  return request.post<{ data: StayVO }>('/stays/check-in-from-reservation', data)
}

export function getStay(id: number) {
  return request.get<{ data: StayVO }>(`/stays/${id}`)
}

export function listInHouse(guestName?: string) {
  return request.get<{ data: StayVO[] }>('/stays/in-house', {
    params: guestName ? { guestName } : undefined
  })
}

export interface VoidCheckoutForm {
  chargeThroughDate?: string
  refundAmount?: number
  refundMethod: string
  remark?: string
}

export function voidCheckout(stayId: number, data: VoidCheckoutForm) {
  return request.post<{ data: StayVO }>(`/stays/${stayId}/void-checkout`, data)
}

export function changeRoom(stayId: number, data: { targetRoomId: number; targetRoomVersion?: number }) {
  return request.post<{ data: StayVO }>(`/stays/${stayId}/change-room`, data)
}

export function updateStayRemark(stayId: number, remark: string) {
  return request.put<{ data: StayVO }>(`/stays/${stayId}/remark`, { remark })
}

export function checkoutStay(stayId: number) {
  return request.post<{ data: StayVO }>(`/stays/${stayId}/checkout`)
}
