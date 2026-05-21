import { request } from './request'

export interface RoomTypeVO {
  id: number
  name: string
  description?: string
  rackRate: number
  bedType?: string
  windowType?: string
  nonSmoking?: number
  maxGuests?: number
  status?: number
}

export interface RoomTypeForm {
  name: string
  description?: string
  rackRate: number
  bedType?: string
  windowType?: string
  nonSmoking?: number
  maxGuests?: number
  status?: number
}

export function listRoomTypesApi() {
  return request.get<{ data: RoomTypeVO[] }>('/room-types')
}

export function createRoomTypeApi(data: RoomTypeForm) {
  return request.post<{ data: RoomTypeVO }>('/room-types', data)
}

export function updateRoomTypeApi(id: number, data: RoomTypeForm) {
  return request.put<{ data: RoomTypeVO }>(`/room-types/${id}`, data)
}
