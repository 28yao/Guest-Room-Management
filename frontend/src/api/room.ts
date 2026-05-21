import { request } from './request'

export interface RoomVO {
  id: number
  roomNo: string
  roomTypeId: number
  roomTypeName?: string
  floorNo: number
  status: string
  version: number
}

export interface RoomBoardItem {
  id: number
  roomNo: string
  roomTypeId: number
  roomTypeName: string
  floorNo: number
  /** 按查看日计算的展示房态 */
  status: string
  /** 库内实时房态（操作用） */
  actualStatus: string
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

export function forceRoomStatusApi(
  id: number,
  data: { targetStatus: string; reason: string; version?: number }
) {
  return request.post<{ data: RoomVO }>(`/rooms/${id}/status/force`, data)
}

/** 可置为脏房的当前状态 */
export const MARK_DIRTY_FROM = ['VACANT_CLEAN', 'RESERVED', 'OCCUPIED'] as const

/** 可置为空净的当前状态 */
export const MARK_CLEAN_FROM = ['DIRTY'] as const

export const ROOM_STATUS_LABEL: Record<string, string> = {
  VACANT_CLEAN: '空净',
  RESERVED: '预订',
  OCCUPIED: '在住',
  DIRTY: '脏房',
  OUT_OF_ORDER: '维修'
}

export const ROOM_STATUS_COLOR: Record<string, string> = {
  VACANT_CLEAN: '#67c23a',
  RESERVED: '#409eff',
  OCCUPIED: '#e6a23c',
  DIRTY: '#909399',
  OUT_OF_ORDER: '#f56c6c'
}
