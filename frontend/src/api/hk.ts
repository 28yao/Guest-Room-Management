import { request } from './request'

export interface HkTaskVO {
  id: number
  roomId: number
  roomNo: string
  floorNo?: number
  roomTypeName?: string
  status: string
  createdAt?: string
  completedAt?: string
}

export function listHkTasksApi(floorNo?: number, status = 'PENDING') {
  const params: Record<string, string | number> = { status }
  if (floorNo != null) {
    params.floorNo = floorNo
  }
  return request.get<{ data: HkTaskVO[] }>('/hk/tasks', { params })
}

export function completeHkTaskApi(taskId: number) {
  return request.post<{ data: HkTaskVO }>(`/hk/tasks/${taskId}/complete`)
}
