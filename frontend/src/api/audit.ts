import { request } from './request'

export interface OperationLogVO {
  id: number
  bizType: string
  bizId: number
  operationType: string
  operatorId: number
  operatorName: string
  beforeValue?: string
  afterValue?: string
  summary?: string
  createdAt: string
}

export interface PageResultVO<T> {
  total: number
  records: T[]
}

export function listAuditLogs(params: {
  page?: number
  size?: number
  bizType?: string
  operationType?: string
  from?: string
  to?: string
}) {
  return request.get<{ data: PageResultVO<OperationLogVO> }>('/audit/logs', { params })
}
