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
  status: string
  version: number
  rackRate?: number
  dailyTags: string[]
}

export interface RoomForm {
  roomNo: string
  roomTypeId: number
  floorNo: number
}

export function getRoomBoardApi(floorNo?: number) {
  return request.get<{ data: RoomBoardItem[] }>('/rooms/board', {
    params: floorNo != null ? { floorNo } : {}
  })
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

export function forceRoomStatusApi(
  id: number,
  data: { targetStatus: string; reason: string; version?: number }
) {
  return request.post<{ data: RoomVO }>(`/rooms/${id}/status/force`, data)
}

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
