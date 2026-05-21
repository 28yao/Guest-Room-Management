import { request } from './request'

export interface RoomVO {
  id: number
  roomNo: string
  roomTypeId: number
  roomTypeName?: string
  floorNo: number
  /** 占用态 VACANT|RESERVED|OCCUPIED|OUT_OF_ORDER */
  status: string
  /** 保洁态 CLEAN|DIRTY */
  cleanStatus: string
  version: number
}

export interface RoomBoardItem {
  id: number
  roomNo: string
  roomTypeId: number
  roomTypeName: string
  floorNo: number
  /** 查看日占用展示态 */
  status: string
  /** 库内占用态 */
  occupancyStatus: string
  /** 库内保洁态 */
  cleanStatus: string
  version: number
  rackRate?: number
  dailyTags: string[]
}

export interface RoomForm {
  roomNo: string
  roomTypeId: number
  floorNo: number
}

export function listRoomFloorsApi() {
  return request.get<{ data: number[] }>('/rooms/floors')
}

export interface RoomScheduleOrderVO {
  orderType: 'RESERVATION' | 'STAY'
  orderId: number
  orderNo: string
  guestName: string
  guestPhone: string
  arrivalDate: string
  departureDate: string
  arrivalAt?: string
  departureAt?: string
  status: string
  remark?: string
  agreedDailyRate?: number
  editable?: boolean
}

export interface RoomScheduleVO {
  roomId: number
  roomNo: string
  roomTypeId: number
  roomTypeName: string
  rackRate?: number
  occupancyStatus: string
  cleanStatus: string
  version: number
  viewDate: string
  occupiedOnViewDate: boolean
  orders: RoomScheduleOrderVO[]
}

export function getRoomScheduleApi(roomId: number, fromDate?: string) {
  const params: Record<string, string> = {}
  if (fromDate) {
    params.fromDate = fromDate
  }
  return request.get<{ data: RoomScheduleVO }>(`/rooms/${roomId}/schedule`, { params })
}

export function getRoomBoardApi(floorNo?: number, viewDate?: string) {
  const params: Record<string, string | number> = {}
  if (floorNo != null) {
    params.floorNo = floorNo
  }
  if (viewDate) {
    params.date = viewDate
  }
  return request.get<{ data: RoomBoardItem[] }>('/rooms/board', { params })
}

export function listRoomsApi(floorNo?: number) {
  return request.get<{ data: RoomVO[] }>('/rooms', {
    params: floorNo != null ? { floorNo } : {}
  })
}

export function createRoomApi(data: RoomForm) {
  return request.post<{ data: RoomVO }>('/rooms', data)
}

export function updateRoomApi(id: number, data: RoomForm) {
  return request.put<{ data: RoomVO }>(`/rooms/${id}`, data)
}

export function startMaintenanceApi(
  id: number,
  data: { reason: string; expectedRecoveryAt: string; version?: number }
) {
  return request.post<{ data: RoomVO }>(`/rooms/${id}/maintenance`, data)
}

export function endMaintenanceApi(
  id: number,
  data: { targetStatus: string; version?: number }
) {
  return request.post<{ data: RoomVO }>(`/rooms/${id}/maintenance/end`, data)
}

export function markRoomDirtyApi(id: number, data?: { version?: number }) {
  return request.post<{ data: RoomVO }>(`/rooms/${id}/status/dirty`, data ?? {})
}

export function markRoomCleanApi(id: number, data?: { version?: number }) {
  return request.post<{ data: RoomVO }>(`/rooms/${id}/status/clean`, data ?? {})
}

/** 净/脏一键切换（仅改保洁态，与占用态无关） */
export function toggleCleanDirtyApi(id: number) {
  return request.post<{ data: RoomVO }>(`/rooms/${id}/status/toggle-clean-dirty`)
}

export function forceRoomStatusApi(
  id: number,
  data: { targetStatus: string; reason: string; version?: number }
) {
  return request.post<{ data: RoomVO }>(`/rooms/${id}/status/force`, data)
}

export const OCCUPANCY_STATUS_LABEL: Record<string, string> = {
  VACANT: '空房',
  RESERVED: '预订',
  OCCUPIED: '在住',
  OUT_OF_ORDER: '维修',
  VACANT_CLEAN: '空净',
  DIRTY: '脏房'
}

export const CLEAN_STATUS_LABEL: Record<string, string> = {
  CLEAN: '净',
  DIRTY: '脏'
}

export const OCCUPANCY_STATUS_COLOR: Record<string, string> = {
  VACANT: '#67c23a',
  RESERVED: '#409eff',
  OCCUPIED: '#e6a23c',
  OUT_OF_ORDER: '#f56c6c',
  VACANT_CLEAN: '#67c23a',
  DIRTY: '#909399'
}

export const CLEAN_STATUS_COLOR: Record<string, string> = {
  CLEAN: '#67c23a',
  DIRTY: '#909399'
}

/** @deprecated 使用 OCCUPANCY_STATUS_LABEL */
export const ROOM_STATUS_LABEL = OCCUPANCY_STATUS_LABEL

/** @deprecated 使用 OCCUPANCY_STATUS_COLOR */
export const ROOM_STATUS_COLOR = OCCUPANCY_STATUS_COLOR
