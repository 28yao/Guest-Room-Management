import { request } from './request'

export interface ReservationVO {
  id: number
  resNo: string
  guestName: string
  guestPhone: string
  roomTypeId: number
  roomTypeName?: string
  rackRate?: number
  roomId?: number
  roomNo?: string
  arrivalDate: string
  departureDate: string
  arrivalAt?: string
  departureAt?: string
  status: string
  remark?: string
  createdAt?: string
}

export interface ReservationForm {
  guestName: string
  guestPhone: string
  roomTypeId: number | undefined
  arrivalDate: string
  departureDate: string
  arrivalAt?: string
  departureAt?: string
  remark?: string
}

export interface AvailableRoomVO {
  roomId: number
  roomNo: string
  roomTypeId: number
  roomTypeName: string
  rackRate?: number
  floorNo: number
  version: number
}

export interface PageResult<T> {
  total: number
  records: T[]
}

export const RES_STATUS_LABEL: Record<string, string> = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
  CHECKED_IN: '已入住',
  CANCELLED: '已取消',
  NO_SHOW: 'No-show',
  RELEASED: '已释放'
}

export function listReservationsApi(params: {
  status?: string
  arrivalFrom?: string
  arrivalTo?: string
  guestPhone?: string
  guestName?: string
  page?: number
  size?: number
}) {
  return request.get<{ data: PageResult<ReservationVO> }>('/reservations', { params })
}

export function createReservationApi(data: ReservationForm) {
  return request.post<{ data: ReservationVO }>('/reservations', data)
}

export function updateReservationApi(id: number, data: ReservationForm) {
  return request.put<{ data: ReservationVO }>(`/reservations/${id}`, data)
}

export function assignRoomApi(id: number, roomId: number) {
  return request.post<{ data: ReservationVO }>(`/reservations/${id}/assign-room`, { roomId })
}

export function cancelReservationApi(id: number) {
  return request.post<{ data: ReservationVO }>(`/reservations/${id}/cancel`)
}

export function releaseReservationApi(id: number, noShow?: boolean) {
  return request.post<{ data: ReservationVO }>(`/reservations/${id}/release`, { noShow: !!noShow })
}

export function listAvailabilityApi(params: {
  roomTypeId?: number
  arrival: string
  departure: string
  arrivalAt?: string
  departureAt?: string
  excludeReservationId?: number
}) {
  return request.get<{ data: AvailableRoomVO[] }>('/reservations/availability', { params })
}
